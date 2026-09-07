package com.rockandhardplaces.project;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;

/** Repairs the legacy SQLite constraint to match RH&P-017's existing enum. */
@Component
@DependsOnDatabaseInitialization
public class TaskStatusSchemaMigration {
    private final DataSource dataSource;

    public TaskStatusSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrate() throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            String definition;
            try (var rows = statement.executeQuery("SELECT sql FROM sqlite_master WHERE name = 'tasks'")) {
                if (!rows.next()) return;
                definition = rows.getString(1);
            }
            if (definition.contains("'READY_FOR_REVIEW'")) return;
            List<String> dependentDefinitions = new ArrayList<>();
            try (var rows = statement.executeQuery("SELECT sql FROM sqlite_master WHERE tbl_name = 'tasks' "
                    + "AND type IN ('index', 'trigger') AND sql IS NOT NULL")) {
                while (rows.next()) dependentDefinitions.add(rows.getString(1));
            }
            // Disable on this connection before beginning SQLite's table-rebuild transaction.
            boolean foreignKeys;
            try (var rows = statement.executeQuery("PRAGMA foreign_keys")) {
                foreignKeys = rows.next() && rows.getInt(1) != 0;
            }
            statement.execute("PRAGMA foreign_keys = OFF");
            connection.setAutoCommit(false);
            try {
                statement.execute(definition.replaceFirst("(?i)CREATE TABLE(?: IF NOT EXISTS)? [\"`]?tasks[\"`]?",
                        "CREATE TABLE tasks_rhp017")
                        .replace("'IN_PROGRESS',", "'IN_PROGRESS', 'READY_FOR_REVIEW',"));
                statement.execute("INSERT INTO tasks_rhp017 SELECT * FROM tasks");
                statement.execute("DROP TABLE tasks");
                statement.execute("ALTER TABLE tasks_rhp017 RENAME TO tasks");
                for (String dependent : dependentDefinitions) statement.execute(dependent);
                try (var violations = statement.executeQuery("PRAGMA foreign_key_check(tasks)")) {
                    if (violations.next()) throw new SQLException("Task migration found invalid relationships");
                }
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            } finally {
                connection.setAutoCommit(true);
                statement.execute("PRAGMA foreign_keys = " + (foreignKeys ? "ON" : "OFF"));
            }
        }
    }
}
