package com.rockandhardplaces.portfolio;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Nullable outside-work image reference; preserves existing rows and publication consent. */
@Component
public class PortfolioSchemaMigration {
    private final JdbcTemplate jdbc;
    public PortfolioSchemaMigration(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @PostConstruct
    void migrate() {
        var columns = jdbc.query("PRAGMA table_info(portfolio_items)", (row, index) -> row.getString("name"));
        if (!columns.contains("media_reference"))
            jdbc.execute("ALTER TABLE portfolio_items ADD COLUMN media_reference VARCHAR(1000)");
    }
}
