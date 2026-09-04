package com.rockandhardplaces;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RockAndHardPlacesApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
    }

    @Test
    void sqliteDataSourceCanOpenConnection() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(1));
            assertEquals("SQLite", connection.getMetaData().getDatabaseProductName());
        }
    }
}