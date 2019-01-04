// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class JdbcConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JdbcConfiguration.class);

    @Autowired
    DataSource dataSource;

    @Autowired
    DataSourceProperties dataSourceProperties;

    @Autowired
    FlywayProperties flywayProperties;

    @PostConstruct
    public void statusReport() throws SQLException {
        try (var connection = DataSourceUtils.getConnection(dataSource)) {
            var metaData = connection.getMetaData();
            log.info("Database: {} {}", metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
            log.info("User: {}", metaData.getUserName());
            log.info("Connection URL: {} (configuration was {})", metaData.getURL(), dataSourceProperties.getUrl());
            log.info("Flyway locations: {}", flywayProperties.getLocations());
        }
    }
}
