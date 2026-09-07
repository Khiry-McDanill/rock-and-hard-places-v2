package com.rockandhardplaces.demo;

import static org.assertj.core.api.Assertions.*;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.communication.*;
import com.rockandhardplaces.portfolio.*;
import com.rockandhardplaces.project.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "rhp.demo.enabled=true")
@Transactional
class DemoDataSeederTests {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:./target/rhp-demo-" + DATABASE + ".sqlite");
    }
    private static final String DATABASE = UUID.randomUUID().toString();
    private static final List<String> TABLES = List.of("users", "homeowners", "tradespeople", "trades",
            "specialties", "person_trades", "person_specialties", "projects", "tasks", "task_trades",
            "bids", "task_assignments", "project_teams", "project_team_trades", "message_requests",
            "conversations", "conversation_participants", "messages", "message_attachments", "reviews",
            "review_responses", "portfolio_items", "portfolio_publication_requests", "demo_seed_versions");
    @Autowired JdbcTemplate jdbc;
    @Autowired DemoDataSeeder seeder;
    @Autowired DemoAccountSeeder accountSeeder;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired DemoActiveAccountContext context;
    @Autowired ProjectRepository projects;
    @Autowired TaskProgressService progress;
    @Autowired TaskAssignmentRepository assignments;
    @Autowired TaskAssignmentService assignmentService;
    @Autowired ConversationRepository conversations;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired CommunicationService communication;
    @Autowired PortfolioPublicationRequestRepository publications;

    @Test
    void repeatInitializationPreservesEveryRowIncludingUserEdits() {
        jdbc.update("UPDATE projects SET description = 'Homeowner revised the scope after initialization' WHERE title = 'Passyunk kitchen remodel'");
        Map<String, List<Map<String, Object>>> before = snapshot();
        seeder.run(null);
        seeder.run(null);
        assertThat(snapshot()).isEqualTo(before);
        assertThat(count("demo_seed_versions")).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void lateInitializationFailureRollsBackTheEntireGraphAndMarker() {
        Map<String, List<Map<String, Object>>> before = snapshot();
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactions);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            for (int i = TABLES.size() - 1; i >= 0; i--) jdbc.update("DELETE FROM " + TABLES.get(i));
            accountSeeder.run(null);
            jdbc.execute("CREATE TRIGGER reject_demo_marker BEFORE INSERT ON demo_seed_versions "
                    + "BEGIN SELECT RAISE(ABORT, 'Simulated final seed write failure'); END");
            seeder.run(null);
        })).isInstanceOf(org.springframework.dao.DataAccessException.class)
                .hasMessageContaining("Simulated final seed write failure");
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void usersAreDistinctActiveAndDualContextRetainsItsIdentity() {
        assertThat(count("users")).isEqualTo(12);
        assertThat(count("homeowners")).isEqualTo(5);
        assertThat(count("tradespeople")).isEqualTo(8);
        assertThat(count("trades")).isEqualTo(6);
        assertThat(count("specialties")).isEqualTo(8);
        assertThat(jdbc.queryForList("PRAGMA foreign_key_check")).isEmpty();
        assertThat(number("SELECT COUNT(*) FROM tradespeople WHERE account_status <> 'ACTIVE' OR verification_status <> 'VERIFIED' OR profile_image_reference IS NULL OR base_zip = '00000'")).isZero();
        assertThat(number("SELECT COUNT(*) FROM homeowners WHERE account_status <> 'ACTIVE' OR profile_image_reference IS NULL")).isZero();
        Long userId = context.currentUser().getId();
        try {
            context.switchTo(AccountRole.HOMEOWNER);
            assertThat(context.activeProfile()).isInstanceOf(Homeowner.class);
            context.switchTo(AccountRole.TRADESPERSON);
            assertThat(context.activeProfile()).isInstanceOf(Tradesperson.class);
            assertThat(context.currentUser().getId()).isEqualTo(userId);
            assertThat(context.availableProfiles()).hasSize(2);
        } finally { context.switchTo(AccountRole.HOMEOWNER); }
    }

    @Test
    void progressUsesLeavesAndPreservesCancellationAndApproval() {
        assertThat(count("projects")).isEqualTo(9);
        assertThat(count("tasks")).isEqualTo(37);
        assertThat(count("task_trades")).isEqualTo(28);
        assertThat(projects.findAll()).extracting(Project::getStatus).contains(ProjectStatus.values());
        for (Project project : projects.findAll()) {
            int expected = switch (project.getStatus()) {
                case COMPLETED -> 100;
                case IN_PROGRESS -> 33;
                default -> 0;
            };
            assertThat(progress.progressPercentage(project)).as(project.getTitle()).isEqualTo(expected);
            assertThat(progress.isProjectComplete(project)).isEqualTo(project.getStatus() == ProjectStatus.COMPLETED);
            if (project.getStatus() == ProjectStatus.IN_PROGRESS) {
                assertThat(project.getTasks()).extracting(Task::getStatus).contains(TaskStatus.READY_FOR_REVIEW);
                assertThat(progress.totalSubtasks(project)).isEqualTo(3);
                assertThat(progress.completedSubtasks(project)).isEqualTo(1);
            }
        }
        Project kitchen = projects.findAll().stream().filter(p -> p.getTitle().contains("kitchen")).findFirst().orElseThrow();
        assertThat(kitchen.getTasks()).filteredOn(t -> t.getStatus() == TaskStatus.CANCELLED).hasSize(1);
        assertThat(progress.totalTopLevelTasks(kitchen)).isEqualTo(1);
    }

    @Test
    void bidsAssignmentsAndTeamRolesAreQualifiedAndNeverSelfDealing() {
        assertThat(count("bids")).isEqualTo(26);
        assertThat(number("SELECT COUNT(*) FROM bids WHERE status = 'ACCEPTED'")).isEqualTo(21);
        assertThat(number("SELECT COUNT(*) FROM bids WHERE status = 'SUBMITTED'")).isEqualTo(3);
        assertThat(number("SELECT COUNT(*) FROM bids WHERE status = 'REJECTED'")).isEqualTo(2);
        assertThat(count("task_assignments")).isEqualTo(21);
        assertThat(count("project_teams")).isEqualTo(15);
        assertThat(count("project_team_trades")).isEqualTo(15);
        assignments.findAll().forEach(a -> assignmentService.validateAssignmentEligibility(a.getTask(), a.getTradesperson()));
        assertThat(number("""
                SELECT COUNT(*) FROM bids b JOIN task_trades tt ON tt.id = b.task_trade_id
                JOIN tasks t ON t.id = b.task_id JOIN projects p ON p.id = t.project_id
                JOIN homeowners h ON h.id = p.homeowner_id JOIN tradespeople w ON w.id = b.tradesperson_id
                WHERE tt.task_id <> t.id OR h.user_id = w.user_id OR w.verification_status <> 'VERIFIED'
                OR NOT EXISTS (SELECT 1 FROM person_trades pt WHERE pt.tradesperson_id = w.id AND pt.trade_id = tt.trade_id)
                """)).isZero();
    }

    @Test
    void communicationsHaveAuthorizedSendersAndHistoricalOrdering() {
        assertThat(count("conversations")).isEqualTo(15);
        assertThat(count("messages")).isEqualTo(52);
        assertThat(count("message_requests")).isEqualTo(1);
        em.createQuery("select m from Message m", Message.class).getResultList().forEach(m -> assertThat(communication.canAccess(m.getConversation(), m.getSender())).isTrue());
        assertThat(number("""
                SELECT COUNT(*) FROM messages m JOIN conversations c ON c.id = m.conversation_id
                JOIN conversation_participants cp ON cp.conversation_id = c.id AND cp.user_id = m.sender_id
                WHERE m.created_at < c.created_at OR m.created_at < cp.joined_at
                """)).isZero();
        assertThat(number("""
                SELECT COUNT(*) FROM messages a JOIN messages b ON a.conversation_id = b.conversation_id AND a.id < b.id
                WHERE a.created_at >= b.created_at
                """)).isZero();
        assertThat(number("SELECT MAX(created_at) - MIN(created_at) FROM messages")).isGreaterThan(180L * 86400000);
        assertThat(number("""
                SELECT COUNT(*) FROM conversation_participants cp JOIN conversations c ON c.id = cp.conversation_id
                JOIN projects p ON p.id = c.project_id JOIN homeowners h ON h.id = p.homeowner_id
                WHERE c.type = 'PROJECT_TEAM' AND cp.user_id <> h.user_id AND NOT EXISTS (
                SELECT 1 FROM project_teams pt JOIN tradespeople t ON t.id = pt.tradesperson_id
                WHERE pt.project_id = p.id AND pt.status = 'ACTIVE' AND t.user_id = cp.user_id)
                """)).isZero();
    }

    @Test
    void reputationAndPhotosHaveCompletedWorkAndHomeownerConsent() {
        assertThat(count("reviews")).isEqualTo(7);
        assertThat(count("review_responses")).isEqualTo(3);
        assertThat(count("portfolio_items")).isEqualTo(5);
        assertThat(count("message_attachments")).isEqualTo(14);
        assertThat(count("portfolio_publication_requests")).isEqualTo(6);
        assertThat(number("""
                SELECT COUNT(*) FROM reviews r JOIN projects p ON p.id = r.project_id
                JOIN homeowners h ON h.id = r.homeowner_id JOIN tradespeople w ON w.id = r.tradesperson_id
                WHERE h.id <> p.homeowner_id OR h.user_id = w.user_id OR r.overall_rating NOT BETWEEN 1 AND 5
                OR NOT EXISTS (SELECT 1 FROM task_assignments a JOIN tasks t ON t.id = a.task_id
                    WHERE a.tradesperson_id = w.id AND t.project_id = p.id AND t.status = 'COMPLETED'
                    AND (r.task_id IS NULL OR t.id = r.task_id))
                """)).isZero();
        assertThat(number("SELECT COUNT(*) FROM (SELECT homeowner_id, tradesperson_id, project_id FROM reviews GROUP BY homeowner_id, tradesperson_id, project_id HAVING COUNT(*) > 1)")).isZero();
        assertThat(number("""
                SELECT COUNT(*) FROM portfolio_items i WHERE i.provenance = 'RHP_VERIFIED' AND NOT EXISTS (
                SELECT 1 FROM task_assignments a JOIN tasks t ON t.id = a.task_id
                WHERE a.tradesperson_id = i.tradesperson_id AND t.id = i.task_id
                    AND t.project_id = i.project_id AND t.status = 'COMPLETED')
                """)).isZero();
        assertThat(publications.findAll()).filteredOn(PortfolioPublicationRequest::isPublic).hasSize(3).allSatisfy(p -> {
            assertThat(p.getDecidedBy().getId()).isEqualTo(p.getPortfolioItem().getProject().getHomeowner().getId());
            assertThat(p.getAttachment().getMessage().getConversation().getProject().getId())
                    .isEqualTo(p.getPortfolioItem().getProject().getId());
            assertThat(p.getDecidedAt()).isAfter(p.getCreatedAt());
        });
        assertThat(publications.findAll()).filteredOn(p -> !p.isPublic()).hasSize(3);
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
        Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();
        TABLES.forEach(table -> data.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY 1")));
        return data;
    }
    private long count(String table) { return number("SELECT COUNT(*) FROM " + table); }
    private long number(String sql) { return jdbc.queryForObject(sql, Long.class); }
}
