// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = NONE)
public class PostgresFunctionSpikeTest {

    @Autowired
    DataSource dataSource;

    @Test
    public void call_function_with_hard_coded_parameters() {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        int result = jdbcTemplate.queryForObject(
                "SELECT spike(array[1, 2, 3, 4])",
                new MapSqlParameterSource(),
                Integer.class);

        assertThat(result, is(10));
    }

    @Test
    public void call_function_with_dynamic_parameters() throws SQLException {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        try (Connection connection = DataSourceUtils.getConnection(dataSource)) {
            Array numbers = connection.createArrayOf("int", new Integer[]{1, 2, 3, 4});

            int result = jdbcTemplate.queryForObject(
                    "SELECT spike(:numbers)",
                    new MapSqlParameterSource("numbers", numbers),
                    Integer.class);

            assertThat(result, is(10));
        }
    }

    @Test
    public void call_function_with_SimpleJdbcCall() throws SQLException {
        SimpleJdbcCall call = new SimpleJdbcCall(dataSource)
                .withFunctionName("spike")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("numbers", Types.ARRAY)
                );

        try (Connection connection = DataSourceUtils.getConnection(dataSource)) {
            Array numbers = connection.createArrayOf("int", new Integer[]{1, 2, 3, 4});
            Map<String, Object> result = call.execute(new MapSqlParameterSource("numbers", numbers));
            assertThat(result.toString(), is("{#result-set-1=[{result=10}]}"));
        }
    }

    @Test
    public void multiple_parameters() throws SQLException {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        try (Connection connection = DataSourceUtils.getConnection(dataSource)) {
            Array numbers = connection.createArrayOf("int", new Integer[]{1, 2, 3, 4});
            Array strings = connection.createArrayOf("text", new String[]{"alpha", "bravo", "charlie", "delta"});

            int result = jdbcTemplate.queryForObject(
                    "SELECT spike2(:numbers, :strings)",
                    new MapSqlParameterSource()
                            .addValue("numbers", numbers)
                            .addValue("strings", strings),
                    Integer.class);

            assertThat(result, is(10));
        }
    }
}
