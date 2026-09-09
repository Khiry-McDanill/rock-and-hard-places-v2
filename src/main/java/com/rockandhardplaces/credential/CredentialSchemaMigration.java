package com.rockandhardplaces.credential;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Upgrade existing RH&P-030 databases without changing supplied credential facts. */
@Component
public class CredentialSchemaMigration {
    private final JdbcTemplate jdbc;
    public CredentialSchemaMigration(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @PostConstruct
    public void migrate() {
        add("verification_status", "TEXT NOT NULL DEFAULT 'PROVIDED' CHECK(verification_status IN ('PROVIDED','PENDING_VERIFICATION','VERIFIED'))");
        add("evidence_kind", "TEXT NOT NULL DEFAULT 'OTHER'");
        add("evidence_reference", "TEXT");
        add("confirmed_trade_id", "INTEGER REFERENCES trades(id)");
        add("verified_at", "TEXT");
        add("verification_basis", "TEXT");
    }
    private void add(String name, String definition) {
        var columns=jdbc.query("PRAGMA table_info(professional_credentials)",(row,index)->row.getString("name"));
        if(!columns.contains(name)) jdbc.execute("ALTER TABLE professional_credentials ADD COLUMN " + name + " " + definition);
    }
}
