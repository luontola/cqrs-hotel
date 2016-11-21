// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PsqlEventStore implements EventStore {

    private static final Pattern OPTIMISTIC_LOCKING_FAILURE_MESSAGE =
            Pattern.compile("^optimistic locking failure, current version is (\\d+)$");

    private final DataSource dataSource;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper json = new ObjectMapper();

    public PsqlEventStore(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public List<Event> getEventsForStream(UUID streamId) {
        List<Event> events = jdbcTemplate.query("SELECT data, metadata " +
                        "FROM event " +
                        "WHERE stream_id = :stream_id " +
                        "ORDER BY version",
                new MapSqlParameterSource("stream_id", streamId),
                (rs, rowNum) -> {
                    String data = rs.getString("data");
                    String metadata = rs.getString("metadata");
                    return deserialize(data, metadata);
                });
        if (events.isEmpty()) {
            throw new EventStreamNotFoundException(streamId);
        }
        return events;
    }

    @Override
    public void saveEvents(UUID streamId, List<Event> newEvents, int expectedVersion) {
        try (Connection connection = DataSourceUtils.getConnection(dataSource)) {
            Array data = connection.createArrayOf("jsonb", serializeData(newEvents));
            Array metadata = connection.createArrayOf("jsonb", serializeMetadata(newEvents));

            int latestVersion = jdbcTemplate.queryForObject(
                    "SELECT save_events(:stream_id, :expected_version, :data, :metadata)",
                    new MapSqlParameterSource()
                            .addValue("stream_id", streamId)
                            .addValue("expected_version", expectedVersion)
                            .addValue("data", data)
                            .addValue("metadata", metadata),
                    Integer.class);

        } catch (UncategorizedSQLException e) {
            if (e.getCause() instanceof PSQLException) {
                ServerErrorMessage serverError = ((PSQLException) e.getCause()).getServerErrorMessage();
                Matcher m = OPTIMISTIC_LOCKING_FAILURE_MESSAGE.matcher(serverError.getMessage());
                if (m.matches()) {
                    String currentVersion = m.group(1);
                    throw new OptimisticLockingException("expected version " + expectedVersion +
                            " but was " + currentVersion + " for stream " + streamId, e);
                }
            }
            throw e;
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String[] serializeData(List<Event> events) throws JsonProcessingException {
        return events.stream()
                .map(this::serialize)
                .toArray(String[]::new);
    }

    private String[] serializeMetadata(List<Event> events) throws JsonProcessingException {
        return events.stream()
                .map(PsqlEventStore::getMetadata)
                .map(this::serialize)
                .toArray(String[]::new);
    }

    private static EventMetadata getMetadata(Event event) {
        EventMetadata meta = new EventMetadata();
        meta.type = event.getClass().getName();
        return meta;
    }

    private String serialize(Object event) {
        try {
            return json.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize: " + event, e);
        }
    }

    private Event deserialize(String dataJson, String metadataJson) {
        try {
            EventMetadata metadata = json.readValue(metadataJson, EventMetadata.class);
            return (Event) json.readValue(dataJson, Class.forName(metadata.type));
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
