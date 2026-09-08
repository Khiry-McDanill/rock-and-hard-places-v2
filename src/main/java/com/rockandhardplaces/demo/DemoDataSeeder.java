package com.rockandhardplaces.demo;

import com.rockandhardplaces.account.*;
import com.rockandhardplaces.catalog.*;
import com.rockandhardplaces.communication.*;
import com.rockandhardplaces.portfolio.*;
import com.rockandhardplaces.project.*;
import com.rockandhardplaces.review.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Application demo history, deliberately independent of test fixtures and active-role selection. */
@Component
@Order(100)
@ConditionalOnProperty(name = "rhp.demo.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {
    static final String VERSION = "rhp-023-v1";
    static final String WINDOW_RETURNS_VERSION = "rhp-025-window-returns-carpentry-v1";
    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final BidSubmissionService bidding;
    private final BidAcceptanceService acceptance;
    private final ProjectWorkflowService workflow;
    private final TaskProgressService progress;
    private final CommunicationService communication;
    private final ReviewService reviews;
    private final PortfolioService portfolio;
    private final ProjectTeamRepository teams;

    public DemoDataSeeder(EntityManager em, JdbcTemplate jdbc, BidSubmissionService bidding,
            BidAcceptanceService acceptance, ProjectWorkflowService workflow, TaskProgressService progress,
            CommunicationService communication, ReviewService reviews, PortfolioService portfolio,
            ProjectTeamRepository teams) {
        this.em = em; this.jdbc = jdbc; this.bidding = bidding; this.acceptance = acceptance;
        this.workflow = workflow; this.progress = progress; this.communication = communication;
        this.reviews = reviews; this.portfolio = portfolio; this.teams = teams;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Repair only this exact legacy fixture copy, including already-seeded databases.
        // Keep seed markers, identities, and media references intact for removal.
        jdbc.update("UPDATE portfolio_items SET description = ? WHERE title = ? AND description = ? "
                        + "AND tradesperson_id IN (SELECT id FROM tradespeople WHERE user_id IN "
                        + "(SELECT id FROM users WHERE email LIKE '%@demo.rockandhardplaces.local'))",
                "Pre-platform work with a checked client reference; not completed through RH&P.", "Germantown garden wall restoration",
                "Pre-platform work; demo external verification represents a checked client reference, not RH&P completion.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_versions WHERE version = ?",
                Integer.class, VERSION) != 0) {
            seedWindowReturnsCarpentry();
            return;
        }
        List<Runnable> history = new ArrayList<>();
        Homeowner jordan = em.createQuery("select h from Homeowner h where h.user.email = :email", Homeowner.class)
                .setParameter("email", DemoActiveAccountContext.DEMO_EMAIL).getSingleResult();
        jordan.setProfileImageReference(portrait("jordan-ellis"));
        Tradesperson dual = em.createQuery("select t from Tradesperson t where t.user = :user", Tradesperson.class)
                .setParameter("user", jordan.getUser()).getSingleResult();
        dual.setProfileImageReference(portrait("jordan-ellis"));
        dual.setVerificationStatus(TradespersonVerificationStatus.VERIFIED);
        // Only the one-time demo upgrade changes the original stand-in's discovery details.
        em.flush();
        jdbc.update("UPDATE tradespeople SET base_zip = '19147', service_radius = 20 WHERE id = ?", dual.getId());
        em.refresh(dual);

        Homeowner maya = homeowner("maya-patel", "Maya Patel");
        Homeowner andre = homeowner("andre-brooks", "Andre Brooks");
        Homeowner elena = homeowner("elena-rivera", "Elena Rivera");
        Homeowner ruth = homeowner("ruth-chen", "Ruth Chen");
        String[] tradeNames = {"Carpentry", "Plumbing", "Electrical", "Drywall", "Flooring", "Exterior Restoration"};
        String[] specialties = {"Decks and structural framing", "Bathroom rough-ins", "Residential panel upgrades",
                "Plaster repair and finishing", "Hardwood restoration", "Masonry and weatherproofing"};
        Trade[] trades = new Trade[tradeNames.length];
        for (int i = 0; i < trades.length; i++) {
            List<Trade> existing = em.createQuery("select t from Trade t where t.name = :name", Trade.class)
                    .setParameter("name", tradeNames[i]).getResultList();
            trades[i] = existing.isEmpty() ? save(new Trade(tradeNames[i])) : existing.get(0);
        }
        Tradesperson[] workers = {
            worker("caleb-morgan", "Caleb Morgan", "19125", trades[0], specialties[0]),
            worker("nina-alvarez", "Nina Alvarez", "19148", trades[1], specialties[1]),
            worker("marcus-reed", "Marcus Reed", "19104", trades[2], specialties[2]),
            worker("sofia-nguyen", "Sofia Nguyen", "19130", trades[3], specialties[3]),
            worker("darius-cole", "Darius Cole", "19143", trades[4], specialties[4]),
            worker("leah-bennett", "Leah Bennett", "19119", trades[5], specialties[5])
        };
        qualify(dual, trades[0], "Built-in cabinetry");
        Tradesperson competitor = worker("owen-price", "Owen Price", "19123", trades[0], "Finish carpentry");

        scenario(history, jordan, "Passyunk kitchen remodel", "19147",
                "Open up a narrow rowhouse kitchen, retain the original pine floor, and coordinate new sink and island circuits.",
                ProjectStatus.IN_PROGRESS, "2026-05-04", trades, workers, new int[]{0,1,2},
                new String[]{"Fit maple base cabinets", "Relocate sink supply and waste", "Wire island outlets"}, competitor);
        scenario(history, maya, "Cedar Park bathroom renovation", "19143",
                "Replace a leaking upstairs bath with a walk-in shower, moisture-resistant wall finish, and safe dedicated circuits.",
                ProjectStatus.COMPLETED, "2026-01-12", trades, workers, new int[]{1,3,2},
                new String[]{"Install shower valve and drain", "Finish moisture-resistant walls", "Install GFCI protection"}, null);
        scenario(history, andre, "Fishtown cedar deck", "19125",
                "Rebuild the rear deck with cedar boards, code-compliant guards, and flashed ledger connections.",
                ProjectStatus.IN_PROGRESS, "2026-06-01", trades, workers, new int[]{0,0,0},
                new String[]{"Replace ledger and joists", "Lay cedar decking", "Fit stair guards"}, competitor);
        scenario(history, elena, "Mount Airy attic framing", "19119",
                "Frame an attic office with a new partition and storage alcove while keeping the existing roof structure intact.",
                ProjectStatus.IN_PROGRESS, "2026-07-06", trades, workers, new int[]{0,0,3},
                new String[]{"Frame office partition", "Build storage alcove", "Hang attic drywall"}, null);
        scenario(history, ruth, "Fairmount plaster and drywall repairs", "19130",
                "Repair old water damage after roof remediation; protect original trim and blend the new finish into adjacent plaster.",
                ProjectStatus.PLANNING, "2026-08-03", trades, workers, new int[]{3,3,3},
                new String[]{"Patch stairwell ceiling", "Skim living room walls", "Finish window returns"}, null);
        scenario(history, maya, "Cedar Park oak floor restoration", "19143",
                "Restore the first-floor oak boards with matching repairs and a low-sheen finish suitable for a busy family home.",
                ProjectStatus.COMPLETED, "2026-03-02", trades, workers, new int[]{4,4,4},
                new String[]{"Replace damaged oak boards", "Sand and seal first floor", "Install matching thresholds"}, null);
        scenario(history, ruth, "Fairmount utility room coordination", "19130",
                "Coordinate laundry supply, a dedicated appliance circuit, and wall repairs before the stacked washer arrives.",
                ProjectStatus.IN_PROGRESS, "2026-08-10", trades, workers, new int[]{1,2,3},
                new String[]{"Move laundry shutoff valves", "Run dedicated laundry circuit", "Close utility wall"}, null);
        // Jordan performs paid work on another homeowner's project, never Jordan's own kitchen.
        Tradesperson[] exteriorCrew = workers.clone();
        exteriorCrew[0] = dual;
        scenario(history, andre, "Fishtown exterior water repairs", "19125",
                "Repoint the rear brickwork, restore damaged window trim, and seal the entry points found during spring rain.",
                ProjectStatus.COMPLETED, "2026-04-06", trades, exteriorCrew, new int[]{5,0,5},
                new String[]{"Repoint rear brick joints", "Restore rear window trim", "Seal masonry transitions"}, null);
        scenario(history, elena, "Mount Airy side porch replacement", "19119",
                "Proposed side porch replacement deferred after the survey identified a boundary issue; no work was commissioned.",
                ProjectStatus.CANCELLED, "2026-02-02", trades, workers, new int[]{0,0,5},
                new String[]{"Survey porch framing", "Replace porch boards", "Repair porch foundation"}, null);

        PortfolioItem external = portfolio.create(workers[5], "Germantown garden wall restoration",
                "Pre-platform work with a checked client reference; not completed through RH&P.",
                PortfolioProvenance.EXTERNALLY_VERIFIED, null, null, LocalDate.of(2025, 10, 24));
        stamp(history, "portfolio_items", external.getId(), "created_at", "2026-01-05T12:00:00Z");
        PortfolioItem self = portfolio.create(dual, "Walnut reading nook",
                "Self-reported workshop commission with fitted shelving and a window seat; no RH&P project or verified client claim.",
                PortfolioProvenance.SELF_REPORTED, null, null, LocalDate.of(2025, 11, 14));
        stamp(history, "portfolio_items", self.getId(), "created_at", "2026-01-06T12:00:00Z");
        em.flush();
        history.forEach(Runnable::run);
        jdbc.update("INSERT INTO demo_seed_versions(version) VALUES (?)", VERSION);
        seedWindowReturnsCarpentry();
        em.clear();
    }

    private void seedWindowReturnsCarpentry() {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_versions WHERE version = ?",
                Integer.class, WINDOW_RETURNS_VERSION) != 0) return;
        // Window returns need finish carpentry alongside the existing drywall scope.
        // Upgrade only the still-open fixture; retain its bids, history and user edits.
        jdbc.update("""
                INSERT INTO task_trades(task_id, trade_id)
                SELECT t.id, tr.id FROM tasks t
                JOIN projects p ON p.id = t.project_id
                JOIN homeowners h ON h.id = p.homeowner_id
                JOIN users u ON u.id = h.user_id
                JOIN trades tr ON tr.name = 'Carpentry'
                WHERE u.email = 'ruth-chen@demo.rockandhardplaces.local'
                  AND p.title = 'Fairmount plaster and drywall repairs' AND p.status = 'PLANNING'
                  AND t.title = 'Finish window returns' AND t.status = 'PLANNING'
                  AND NOT EXISTS (SELECT 1 FROM task_assignments a WHERE a.task_id = t.id)
                  AND NOT EXISTS (SELECT 1 FROM bids b WHERE b.task_id = t.id AND b.status = 'ACCEPTED')
                  AND NOT EXISTS (SELECT 1 FROM task_trades tt WHERE tt.task_id = t.id AND tt.trade_id = tr.id)
                """);
        // Do not restore a requirement a user later removes, or retry a changed fixture.
        jdbc.update("INSERT INTO demo_seed_versions(version) VALUES (?)", WINDOW_RETURNS_VERSION);
    }

    private void scenario(List<Runnable> history, Homeowner owner, String title, String zip, String description,
            ProjectStatus status, String start, Trade[] trades, Tradesperson[] workers, int[] roles,
            String[] taskTitles, Tradesperson competitor) {
        LocalDate date = LocalDate.parse(start);
        Project project = save(new Project(title, description, status, zip, owner));
        boolean commissioned = status != ProjectStatus.PLANNING && status != ProjectStatus.CANCELLED;
        Task parent = save(new Task("Deliver " + title.toLowerCase(), "Coordinate the trade scopes and homeowner walkthrough.",
                commissioned ? TaskStatus.IN_PROGRESS : TaskStatus.PLANNING, project, null));
        List<Task> work = new ArrayList<>();
        List<Bid> bids = new ArrayList<>();
        for (int i = 0; i < roles.length; i++) {
            int role = roles[i];
            Task task = save(new Task(taskTitles[i], taskTitles[i] + "; include protection, cleanup, and a homeowner walkthrough.",
                    commissioned ? TaskStatus.IN_PROGRESS : TaskStatus.PLANNING, project, parent));
            work.add(task);
            TaskTrade requirement = save(new TaskTrade(task, trades[role]));
            if (status == ProjectStatus.CANCELLED) {
                progress.cancel(task);
                continue;
            }
            if (competitor != null && i == 0) {
                Bid other = bidding.submit(task, requirement, competitor, new BigDecimal("4875.00"),
                        "Includes materials and cleanup. I can reserve two weeks once the scope is confirmed.");
                bids.add(other);
            }
            Bid bid = bidding.submit(task, requirement, workers[role],
                    BigDecimal.valueOf(2350L + role * 425L + i * 650L),
                    "Includes labor, standard materials, floor protection, and daily cleanup for " + taskTitles[i].toLowerCase() + ".");
            bids.add(bid);
            if (commissioned) acceptance.acceptBid(bid, owner);
        }
        for (Bid bid : bids) {
            stamp(history, "bids", bid.getId(), "created_at", at(date, 0, 9));
            stamp(history, "bids", bid.getId(), "updated_at", at(date, commissioned ? 2 : 0, 9));
        }
        if (status == ProjectStatus.CANCELLED) {
            progress.cancel(parent);
            return;
        }
        if (!commissioned) {
            MessageRequest request = communication.requestCommunication(owner.getUser(), workers[roles[0]].getUser(), project);
            Conversation conversation = communication.accept(request, workers[roles[0]].getUser());
            stamp(history, "message_requests", request.getId(), "created_at", at(date, 1, 9));
            stamp(history, "message_requests", request.getId(), "resolved_at", at(date, 1, 10));
            conversationHistory(history, conversation, date, 1);
            message(history, conversation, owner.getUser(), "The roof leak is resolved. Can you assess the plaster before we agree on the finish?", date, 1, 11);
            message(history, conversation, workers[roles[0]].getUser(), "Yes. I will check adhesion and moisture first, then confirm which areas need boarding rather than a skim coat.", date, 2, 9);
            message(history, conversation, owner.getUser(), "Tuesday morning works. Please use the side entrance; the hallway trim is original.", date, 2, 12);
            return;
        }
        if (title.contains("kitchen")) {
            Task cancelled = save(new Task("Add pantry niche", "Removed from scope to preserve the original chimney breast.",
                    TaskStatus.PLANNING, project, parent));
            save(new TaskTrade(cancelled, trades[0]));
            progress.cancel(cancelled);
        }
        Conversation team = communication.synchronizeProjectTeam(teams.findByProject(project).get(0));
        conversationHistory(history, team, date, 3);
        message(history, team, owner.getUser(), "Welcome to the " + title.toLowerCase() + " team. Please protect the occupied rooms and post schedule changes here.", date, 3, 9);
        message(history, team, workers[roles[0]].getUser(), "Materials are confirmed. We will start with " + taskTitles[0].toLowerCase() + " and leave a clear access route each evening.", date, 4, 10);
        message(history, team, workers[roles[1]].getUser(), "I have checked the handoff. Please leave the connections accessible until we have inspected them together.", date, 6, 14);
        Message photoMessage = message(history, team, workers[roles[0]].getUser(), "Progress photos attached for the walkthrough. Please check the finish and clearances before the next stage.", date, 10, 16);
        String slug = title.toLowerCase().replace(' ', '-');
        MessageAttachment photo = communication.attach(photoMessage, workers[roles[0]].getUser(), slug + "-work.jpg",
                "image/jpeg", 248320, "demo/rhp-023/" + slug + "/work.jpg");
        stamp(history, "message_attachments", photo.getId(), "created_at", at(date, 10, 16));
        MessageAttachment detail = communication.attach(photoMessage, workers[roles[0]].getUser(), slug + "-detail.jpg",
                "image/jpeg", 186420, "demo/rhp-023/" + slug + "/detail.jpg");
        stamp(history, "message_attachments", detail.getId(), "created_at", at(date, 10, 16));
        message(history, team, owner.getUser(), "Thanks for documenting the work. I will review the completed scope during Friday's walkthrough.", date, 11, 9);
        Conversation privateChat = communication.privateConversation(owner.getUser(), workers[roles[0]].getUser(), project);
        conversationHistory(history, privateChat, date, 5);
        message(history, privateChat, owner.getUser(), "Please confirm the arrival window so I can arrange access around school pickup.", date, 5, 9);
        message(history, privateChat, workers[roles[0]].getUser(), "We can arrive between 8 and 8:30. I will message here if the supplier delivery changes that.", date, 5, 10);
        for (int i = 0; i < work.size(); i++) {
            if (status == ProjectStatus.COMPLETED || i < 2) workflow.readyForReview(workers[roles[i]], work.get(i));
            if (status == ProjectStatus.COMPLETED || i == 0) workflow.approve(owner, work.get(i));
        }
        if (status == ProjectStatus.COMPLETED) {
            Tradesperson subject = workers[roles[0]];
            int rating = title.contains("floor") ? 4 : 5;
            Review review = reviews.createProjectReview(owner, subject, project, rating, 5, rating, 4, 5,
                    "The " + taskTitles[0].toLowerCase() + " scope was handled carefully. The rooms were left clean and the walkthrough was thorough. A delivery shifted the schedule, but we had advance notice.");
            stamp(history, "reviews", review.getId(), "created_at", at(date, 22, 18));
            ReviewResponse response = reviews.respond(subject, review,
                    "Thank you for making access easy and reviewing the details with us. I appreciate your flexibility around the delivery.");
            stamp(history, "review_responses", response.getId(), "created_at", at(date, 23, 10));
            PortfolioItem item = portfolio.create(subject, title, description, PortfolioProvenance.RHP_VERIFIED,
                    project, work.get(0), date.plusDays(20));
            stamp(history, "portfolio_items", item.getId(), "created_at", at(date, 24, 9));
            PortfolioPublicationRequest approved = portfolio.requestPublication(subject, item, photo);
            portfolio.decide(owner, approved, PublicationStatus.APPROVED);
            stamp(history, "portfolio_publication_requests", approved.getId(), "created_at", at(date, 24, 10));
            stamp(history, "portfolio_publication_requests", approved.getId(), "decided_at", at(date, 25, 10));
            PortfolioPublicationRequest restricted = portfolio.requestPublication(subject, item, detail);
            stamp(history, "portfolio_publication_requests", restricted.getId(), "created_at", at(date, 24, 10));
            if (title.contains("floor")) {
                portfolio.decide(owner, restricted, PublicationStatus.DECLINED);
                stamp(history, "portfolio_publication_requests", restricted.getId(), "decided_at", at(date, 25, 10));
            }
        } else {
            Review review = reviews.createTaskReview(owner, workers[roles[0]], work.get(0), 4, 5, 4, 4, 5,
                    "The " + taskTitles[0].toLowerCase() + " work passed our walkthrough. Good attention to the existing house; the remaining scopes are still underway.");
            stamp(history, "reviews", review.getId(), "created_at", at(date, 12, 18));
        }
    }

    private Homeowner homeowner(String slug, String name) {
        Homeowner profile = save(new Homeowner(user(slug), name));
        profile.setProfileImageReference(portrait(slug));
        return profile;
    }

    private User user(String slug) {
        String email = slug + "@demo.rockandhardplaces.local";
        List<User> existing = em.createQuery("select u from User u where u.email = :email", User.class)
                .setParameter("email", email).getResultList();
        if (!existing.isEmpty()) throw new IllegalStateException("Demo identity already exists without seed marker: " + email);
        return save(new User(email));
    }

    private Tradesperson worker(String slug, String name, String zip, Trade trade, String specialty) {
        Tradesperson profile = save(new Tradesperson(user(slug), name, zip, 25, AvailabilityStatus.AVAILABLE_SOON,
                LocalDate.of(2026, 9, 14)));
        profile.setVerificationStatus(TradespersonVerificationStatus.VERIFIED);
        profile.setProfileImageReference(portrait(slug));
        qualify(profile, trade, specialty);
        return profile;
    }

    private void qualify(Tradesperson profile, Trade trade, String name) {
        save(new PersonTrade(profile, trade));
        List<Specialty> existing = em.createQuery("select s from Specialty s where s.trade = :trade and s.name = :name", Specialty.class)
                .setParameter("trade", trade).setParameter("name", name).getResultList();
        Specialty specialty = existing.isEmpty() ? save(new Specialty(name, trade)) : existing.get(0);
        save(new PersonSpecialty(profile, specialty));
    }

    private <T> T save(T entity) { em.persist(entity); return entity; }
    private static String portrait(String slug) { return "demo/rhp-023/profiles/" + slug + ".jpg"; }
    private static String at(LocalDate date, int days, int hour) {
        return date.plusDays(days) + String.format("T%02d:00:00Z", hour);
    }

    private Message message(List<Runnable> history, Conversation conversation, User sender, String body,
            LocalDate date, int days, int hour) {
        Message message = communication.send(conversation, sender, body);
        stamp(history, "messages", message.getId(), "created_at", at(date, days, hour));
        return message;
    }

    private void conversationHistory(List<Runnable> history, Conversation conversation, LocalDate date, int days) {
        stamp(history, "conversations", conversation.getId(), "created_at", at(date, days, 8));
        history.add(() -> jdbc.update("UPDATE conversation_participants SET joined_at = ? WHERE conversation_id = ?",
                Instant.parse(at(date, days, 8)).toEpochMilli(), conversation.getId()));
    }

    // Constructors use wall-clock time. Backdate only the newly created seed rows after the final ORM flush,
    // keeping historical fixture concerns out of production entities and authorization APIs.
    private void stamp(List<Runnable> history, String table, Long id, String column, String instant) {
        history.add(() -> jdbc.update("UPDATE " + table + " SET " + column + " = ? WHERE id = ?",
                Instant.parse(instant).toEpochMilli(), id));
    }
}
