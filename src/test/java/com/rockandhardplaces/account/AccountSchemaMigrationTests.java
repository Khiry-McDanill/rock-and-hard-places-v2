package com.rockandhardplaces.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AccountSchemaMigrationTests {

    @Test
    void migratesExistingProfileTablesWithoutLosingExistingRows() throws Exception {
        String url = "jdbc:sqlite:./target/account-schema-migration-test.sqlite";

        Path databasePath = Path.of("./target/account-schema-migration-test.sqlite");
        Files.deleteIfExists(databasePath);
        
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {

            statement.execute("""
                    CREATE TABLE homeowners (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL UNIQUE,
                        display_name VARCHAR(255) NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE tradespeople (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL UNIQUE,
                        display_name VARCHAR(255) NOT NULL
                    )
                    """);

            statement.execute("""
                    INSERT INTO homeowners (user_id, display_name)
                    VALUES (1, 'Existing Homeowner')
                    """);

            statement.execute("""
                    INSERT INTO tradespeople (user_id, display_name)
                    VALUES (2, 'Existing Tradesperson')
                    """);
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl(url);

        AccountSchemaMigration migration = new AccountSchemaMigration(dataSource);
        migration.migrateProfileLifecycleColumns();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            assertThat(columns(statement, "homeowners"))
                    .contains("account_status", "profile_image_reference");

            assertThat(columns(statement, "tradespeople"))
                    .contains(
                            "account_status",
                            "verification_status",
                            "profile_image_reference"
                    );

            try (ResultSet homeowner = statement.executeQuery(
                    "SELECT display_name, account_status FROM homeowners WHERE user_id = 1")) {
                assertThat(homeowner.next()).isTrue();
                assertThat(homeowner.getString("display_name"))
                        .isEqualTo("Existing Homeowner");
                assertThat(homeowner.getString("account_status"))
                        .isEqualTo("ACTIVE");
            }

            try (ResultSet tradesperson = statement.executeQuery(
                    """
                    SELECT display_name, account_status, verification_status
                    FROM tradespeople
                    WHERE user_id = 2
                    """)) {
                assertThat(tradesperson.next()).isTrue();
                assertThat(tradesperson.getString("display_name"))
                        .isEqualTo("Existing Tradesperson");
                assertThat(tradesperson.getString("account_status"))
                        .isEqualTo("ACTIVE");
                assertThat(tradesperson.getString("verification_status"))
                        .isEqualTo("NOT_SUBMITTED");
            }
        }
    }

    private List<String> columns(Statement statement, String table) throws Exception {
        List<String> columns = new ArrayList<>();

        try (ResultSet result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                columns.add(result.getString("name"));
            }
        }

        return columns;
    }
}