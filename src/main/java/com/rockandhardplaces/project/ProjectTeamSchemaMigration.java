package com.rockandhardplaces.project;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProjectTeamSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    public ProjectTeamSchemaMigration(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    void migrateLegacyProjectTeamStatusConstraint() {
        String tableDefinition = jdbcTemplate.queryForObject(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'project_teams'",
                String.class);

        if (tableDefinition == null || tableDefinition.contains("'SUSPENDED'")) {
            return;
        }

        jdbcTemplate.execute("PRAGMA foreign_keys = OFF");
        jdbcTemplate.execute("ALTER TABLE project_team_trades RENAME TO project_team_trades_legacy");
        jdbcTemplate.execute("ALTER TABLE project_teams RENAME TO project_teams_legacy");
        jdbcTemplate.execute("""
                CREATE TABLE project_teams (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    tradesperson_id INTEGER NOT NULL,
                    status VARCHAR(32) NOT NULL
                        CHECK (status IN ('INVITED', 'PENDING', 'ACTIVE', 'SUSPENDED')),
                    UNIQUE (project_id, tradesperson_id),
                    FOREIGN KEY (project_id) REFERENCES projects(id),
                    FOREIGN KEY (tradesperson_id) REFERENCES tradespeople(id)
                )
                """);
            jdbcTemplate.execute("""
                CREATE TABLE project_team_trades (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_team_id INTEGER NOT NULL,
                    trade_id INTEGER NOT NULL,
                    UNIQUE (project_team_id, trade_id),
                    FOREIGN KEY (project_team_id) REFERENCES project_teams(id),
                    FOREIGN KEY (trade_id) REFERENCES trades(id)
                )
                """);
        jdbcTemplate.execute("""
                INSERT INTO project_teams (id, project_id, tradesperson_id, status)
                SELECT id, project_id, tradesperson_id, status
                FROM project_teams_legacy
                """);
            jdbcTemplate.execute("""
                INSERT INTO project_team_trades (id, project_team_id, trade_id)
                SELECT id, project_team_id, trade_id
                FROM project_team_trades_legacy
                """);
            jdbcTemplate.execute("DROP TABLE project_team_trades_legacy");
        jdbcTemplate.execute("DROP TABLE project_teams_legacy");
        jdbcTemplate.execute("PRAGMA foreign_keys = ON");
    }
}
