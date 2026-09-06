package com.rockandhardplaces.account;

import java.util.List;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adds RH&P-020 columns without replacing profile rows or their history. */
@Component
public class AccountSchemaMigration {
    private final JdbcTemplate jdbc;

    public AccountSchemaMigration(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    void migrateProfileLifecycleColumns() {
        addIfMissing("homeowners", "account_status",
                "account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' "
                        + "CHECK (account_status IN ('ACTIVE','SUSPENDED','DEACTIVATED'))");
        addIfMissing("homeowners", "profile_image_reference", "profile_image_reference VARCHAR(1000)");
        addIfMissing("tradespeople", "account_status",
                "account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' "
                        + "CHECK (account_status IN ('ACTIVE','SUSPENDED','DEACTIVATED'))");
        addIfMissing("tradespeople", "verification_status",
                "verification_status VARCHAR(32) NOT NULL DEFAULT 'NOT_SUBMITTED' "
                        + "CHECK (verification_status IN ('NOT_SUBMITTED','PENDING','VERIFIED','REJECTED'))");
        addIfMissing("tradespeople", "profile_image_reference", "profile_image_reference VARCHAR(1000)");
    }

    private void addIfMissing(String table, String column, String definition) {
        List<String> columns = jdbc.query("PRAGMA table_info(" + table + ")",
                (result, row) -> result.getString("name"));
        if (!columns.contains(column)) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + definition);
        }
    }
}
