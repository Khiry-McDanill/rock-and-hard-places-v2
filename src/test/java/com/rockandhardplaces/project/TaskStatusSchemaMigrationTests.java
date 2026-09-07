package com.rockandhardplaces.project;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

class TaskStatusSchemaMigrationTests {
    @TempDir Path directory;

    @Test
    void legacyTasksKeepIdsHierarchyReferencesAndIndexesAcrossRepeatedMigration() throws Exception {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + directory.resolve("legacy.sqlite"));
        dataSource.setEnforceForeignKeys(true);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String schema = new ClassPathResource("schema.sql").getContentAsString(StandardCharsets.UTF_8)
                .replace("'READY_FOR_REVIEW', ", "").replaceAll("(?m)^--.*$", "");
        for (String sql : schema.split(";")) if (!sql.isBlank()) jdbc.execute(sql);
        jdbc.execute("INSERT INTO users(id,email) VALUES (11,'migration@demo.local'),(12,'worker@demo.local')");
        jdbc.execute("INSERT INTO homeowners(id,user_id,display_name) VALUES (21,11,'Owner')");
        jdbc.execute("INSERT INTO tradespeople(id,user_id,display_name,base_zip,service_radius,availability_status) VALUES (22,12,'Worker','19147',20,'AVAILABLE_NOW')");
        jdbc.execute("INSERT INTO projects(id,title,description,status,job_zip,homeowner_id) VALUES (31,'Repair','Repair','IN_PROGRESS','19147',21)");
        jdbc.execute("INSERT INTO tasks(id,title,description,status,project_id,parent_task_id) VALUES (41,'Parent','Scope','IN_PROGRESS',31,NULL),(42,'Child','Work','IN_PROGRESS',31,41)");
        jdbc.execute("INSERT INTO task_assignments(task_id,tradesperson_id) VALUES (42,22)");
        jdbc.execute("CREATE INDEX idx_legacy_task_project ON tasks(project_id)");
        assertThatThrownBy(() -> jdbc.update("UPDATE tasks SET status = 'READY_FOR_REVIEW' WHERE id = 42"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);

        TaskStatusSchemaMigration migration = new TaskStatusSchemaMigration(dataSource);
        migration.migrate();
        migration.migrate();

        jdbc.update("UPDATE tasks SET status = 'READY_FOR_REVIEW' WHERE id = 42");
        assertThat(jdbc.queryForObject("SELECT parent_task_id FROM tasks WHERE id = 42", Long.class)).isEqualTo(41);
        assertThat(jdbc.queryForObject("SELECT task_id FROM task_assignments", Long.class)).isEqualTo(42);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sqlite_master WHERE name = 'idx_legacy_task_project'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("PRAGMA foreign_key_check")).isEmpty();
        assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.execute("UPDATE tasks SET status = 'UNRECOGNIZED' WHERE id = 42"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
}
