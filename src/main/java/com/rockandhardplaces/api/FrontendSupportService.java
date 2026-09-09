package com.rockandhardplaces.api;

import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.catalog.*;
import com.rockandhardplaces.project.*;

@Service
@Transactional(readOnly = true)
class FrontendSupportService {
    private final ActiveAccountContext account;
    private final ApiAccessService access;
    private final AccountAuthorizationService authorization;
    private final TaskProgressService progress;
    private final TradeRepository trades;
    private final SpecialtyRepository specialties;
    private final PersonTradeRepository qualifications;
    private final PersonSpecialtyRepository personSpecialties;
    private final TradespersonRepository people;
    private final TaskRepository tasks;
    private final TaskAssignmentRepository assignments;
    private final ProjectTeamRepository teams;
    private final BidRepository bids;
    private final TaskTradeRepository taskTrades;

    FrontendSupportService(ActiveAccountContext account, ApiAccessService access,
            AccountAuthorizationService authorization, TaskProgressService progress,
            TradeRepository trades, SpecialtyRepository specialties, PersonTradeRepository qualifications,
            PersonSpecialtyRepository personSpecialties, TradespersonRepository people, TaskRepository tasks,
            TaskAssignmentRepository assignments, ProjectTeamRepository teams, BidRepository bids, TaskTradeRepository taskTrades) {
        this.account = account; this.access = access; this.authorization = authorization;
        this.progress = progress; this.trades = trades; this.specialties = specialties;
        this.qualifications = qualifications; this.personSpecialties = personSpecialties;
        this.people = people; this.tasks = tasks; this.assignments = assignments;
        this.teams = teams; this.bids = bids; this.taskTrades = taskTrades;
    }

    List<FrontendDtos.TradeSummary> catalog() {
        activeProfile();
        return tradeCatalog().values().stream().toList();
    }

    private Map<Long, FrontendDtos.TradeSummary> tradeCatalog() {
        Map<Long, List<FrontendDtos.SpecialtySummary>> byTrade = new HashMap<>();
        specialties.findAll(Sort.by("id")).forEach(s -> byTrade
                .computeIfAbsent(s.getTrade().getId(), id -> new ArrayList<>())
                .add(new FrontendDtos.SpecialtySummary(s.getId(), s.getName())));
        Map<Long, FrontendDtos.TradeSummary> catalog = new LinkedHashMap<>();
        trades.findAll(Sort.by("id")).forEach(t -> catalog.put(t.getId(),
                new FrontendDtos.TradeSummary(t.getId(), t.getName(), byTrade.getOrDefault(t.getId(), List.of()))));
        return catalog;
    }

    List<FrontendDtos.PersonSummary> discoverPeople(Long tradeId, String query, AvailabilityStatus availability) {
        homeowner(); // Public discovery may include the viewer; marketplace eligibility still excludes self-dealing.
        Map<Long, FrontendDtos.TradeSummary> catalog = tradeCatalog();
        String search = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return people.findByAccountStatus(AccountStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(Tradesperson::getId))
                .filter(p -> p.getDisplayName().toLowerCase(Locale.ROOT).contains(search))
                .filter(p -> availability == null || p.getAvailabilityStatus() == availability)
                .map(p -> new FrontendDtos.PersonSummary(ApiDtos.ProfileResponse.from(p),
                        qualifications.findByTradesperson(p).stream().map(q -> catalog.get(q.getTrade().getId())).toList(),
                        personSpecialties.findByTradesperson(p).stream().map(s ->
                            new FrontendDtos.SpecialtySummary(s.getSpecialty().getId(), s.getSpecialty().getName())).toList()))
                .filter(p -> tradeId == null || p.qualifications().stream().anyMatch(q -> Objects.equals(q.id(), tradeId)))
                .toList();
    }

    FrontendDtos.HomeownerDashboard homeownerDashboard() {
        List<FrontendDtos.ProjectSummary> summaries = access.projectsFor(homeowner()).stream().map(p -> {
            List<ApiDtos.TaskResponse> review = p.getTasks().stream().filter(this::included)
                    .filter(t -> t.getStatus() == TaskStatus.READY_FOR_REVIEW).map(this::task).toList();
            List<FrontendDtos.NeededTrade> needed = p.getTasks().stream().filter(this::openTask)
                    .flatMap(t -> t.getTaskTrades().stream().filter(this::unfilled)
                        .map(r -> new FrontendDtos.NeededTrade(t.getId(), t.getTitle(), ApiDtos.TaskTradeResponse.from(r))))
                    .toList();
            String next = !openProject(p) ? "PROJECT_CLOSED" : !review.isEmpty() ? "REVIEW_WORK"
                    : p.getTasks().stream().noneMatch(this::included) ? "PLAN_TASKS" : !needed.isEmpty() ? "FIND_TRADESPEOPLE" : "MONITOR_WORK";
            return new FrontendDtos.ProjectSummary(project(p), progress.totalTopLevelTasks(p),
                    progress.completedTopLevelTasks(p), progress.totalSubtasks(p), progress.completedSubtasks(p),
                    teams.findByProject(p).stream().map(ApiDtos.TeamResponse::from).toList(), review, needed, next);
        }).toList();
        return new FrontendDtos.HomeownerDashboard(summaries, summaries.isEmpty() ? "CREATE_PROJECT"
                : summaries.stream().anyMatch(p -> p.nextAction().equals("REVIEW_WORK")) ? "REVIEW_WORK" : "VIEW_PROJECTS");
    }

    FrontendDtos.TradespersonDashboard tradespersonDashboard() {
        Tradesperson actor = worker();
        List<TaskAssignment> work = visibleAssignments(actor);
        Set<Long> qualified = qualifiedTrades(actor);
        return new FrontendDtos.TradespersonDashboard(ApiDtos.ProfileResponse.from(actor), bidding(actor),
                work.stream().filter(a -> openProject(a.getTask().getProject()) && included(a.getTask()))
                    .filter(a -> a.getTask().getStatus() != TaskStatus.COMPLETED).map(this::work).toList(),
                bids.findByTradesperson(actor).stream().filter(b -> b.getStatus() == BidStatus.SUBMITTED)
                    .map(b -> bidSummary(b, actor, qualified)).toList(),
                work.stream().filter(a -> included(a.getTask()) && a.getTask().getStatus() == TaskStatus.COMPLETED).count(),
                access.projectsFor(actor).stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count(),
                opportunities(actor, null, null));
    }

    List<FrontendDtos.BidSummary> myBids() {
        Tradesperson actor=worker();
        Set<Long> qualified=qualifiedTrades(actor);
        return bids.findByTradesperson(actor).stream().map(b->bidSummary(b,actor,qualified)).toList();
    }

    List<FrontendDtos.WorkSummary> myAssignments() {
        return visibleAssignments(worker()).stream().map(this::work).toList();
    }

    List<ApiDtos.AssignmentResponse> taskAssignments(Long taskId) {
        Task task = access.task(taskId, activeProfile());
        return assignments.findByTask(task).stream().map(ApiDtos.AssignmentResponse::from).toList();
    }

    List<FrontendDtos.Opportunity> opportunities(Long tradeId, String jobZip) {
        return opportunities(worker(), tradeId, jobZip);
    }

    FrontendDtos.Opportunity opportunity(Long requirementId) {
        Tradesperson actor = worker();
        TaskTrade requirement = taskTrades.findById(requirementId).orElseThrow(ResourceNotFoundException::new);
        if (!discoverable(requirement, actor, qualifiedTrades(actor))) throw new ResourceNotFoundException();
        return opportunity(requirement, actor);
    }

    private Set<Long> qualifiedTrades(Tradesperson actor) {
        Set<Long> qualified = new HashSet<>();
        qualifications.findByTradesperson(actor).forEach(q -> qualified.add(q.getTrade().getId()));
        return qualified;
    }

    private boolean sameUser(Project project, Tradesperson actor) {
        return Objects.equals(project.getHomeowner().getUser().getId(), actor.getUser().getId());
    }

    private boolean suspended(Project project, Tradesperson actor) {
        return project.getProjectTeams().stream().anyMatch(t ->
                Objects.equals(t.getTradesperson().getId(), actor.getId()) && t.getStatus() == ProjectTeamStatus.SUSPENDED);
    }

    private boolean discoverable(TaskTrade requirement, Tradesperson actor, Set<Long> qualified) {
        Project project = requirement.getTask().getProject();
        return openTask(requirement.getTask()) && project.getHomeowner().getAccountStatus() == AccountStatus.ACTIVE
                && !sameUser(project, actor) && !suspended(project, actor)
                && qualified.contains(requirement.getTrade().getId()) && unfilled(requirement);
    }

    private FrontendDtos.BidSummary bidSummary(Bid bid, Tradesperson actor, Set<Long> qualified) {
        Project project = bid.getTask().getProject();
        boolean visible = !sameUser(project, actor) && !suspended(project, actor)
                && (project.getProjectTeams().stream().anyMatch(t -> t.getStatus() == ProjectTeamStatus.ACTIVE
                    && Objects.equals(t.getTradesperson().getId(), actor.getId()))
                    || discoverable(bid.getTaskTrade(), actor, qualified));
        return new FrontendDtos.BidSummary(ApiDtos.BidResponse.from(bid),
                visible ? project(project) : null, visible ? task(bid.getTask()) : null);
    }

    private FrontendDtos.Opportunity opportunity(TaskTrade requirement, Tradesperson actor) {
        Task task = requirement.getTask();
        return new FrontendDtos.Opportunity(project(task.getProject()), task(task), ApiDtos.TaskTradeResponse.from(requirement),
                task.getProject().getHomeowner().getDisplayName(), bidding(actor),
                bids.findByTaskTrade(requirement).stream().filter(b -> Objects.equals(b.getTradesperson().getId(), actor.getId()))
                    .map(ApiDtos.BidResponse::from).toList());
    }

    private List<FrontendDtos.Opportunity> opportunities(Tradesperson actor, Long tradeId, String jobZip) {
        Set<Long> qualified = qualifiedTrades(actor);
        return tasks.findAll(Sort.by("id")).stream()
                .filter(t -> jobZip == null || jobZip.equals(t.getProject().getJobZip()))
                .flatMap(t -> t.getTaskTrades().stream())
                .filter(r -> tradeId == null || Objects.equals(tradeId, r.getTrade().getId()))
                .filter(r -> discoverable(r, actor, qualified))
                .map(r -> opportunity(r, actor)).toList();
    }

    // Assignments are task-wide, so a trade is covered only by an active assigned member
    // holding both that qualification and that project role, or an accepted requirement bid.
    private boolean unfilled(TaskTrade requirement) {
        if (!bids.findByTaskTradeAndStatus(requirement, BidStatus.ACCEPTED).isEmpty()) return false;
        Long tradeId = requirement.getTrade().getId();
        return assignments.findByTask(requirement.getTask()).stream().noneMatch(a ->
            qualifications.findByTradesperson(a.getTradesperson()).stream().anyMatch(q -> Objects.equals(q.getTrade().getId(), tradeId))
            && teams.findByProject(requirement.getTask().getProject()).stream().anyMatch(team ->
                team.getStatus() == ProjectTeamStatus.ACTIVE
                && Objects.equals(team.getTradesperson().getId(), a.getTradesperson().getId())
                && team.getProjectTeamTrades().stream().anyMatch(r -> Objects.equals(r.getTrade().getId(), tradeId))));
    }

    private List<TaskAssignment> visibleAssignments(Tradesperson actor) {
        Set<Long> visible = new HashSet<>();
        access.projectsFor(actor).forEach(p -> visible.add(p.getId()));
        return assignments.findByTradesperson(actor).stream()
                .filter(a -> visible.contains(a.getTask().getProject().getId())).toList();
    }
    private FrontendDtos.WorkSummary work(TaskAssignment a) {
        return new FrontendDtos.WorkSummary(ApiDtos.AssignmentResponse.from(a), project(a.getTask().getProject()), task(a.getTask()));
    }
    private FrontendDtos.Eligibility bidding(Tradesperson actor) {
        try {
            authorization.requireVerifiedBidder(actor);
            return new FrontendDtos.Eligibility(true, null);
        } catch (SecurityException e) {
            return new FrontendDtos.Eligibility(false, e.getMessage());
        }
    }
    private boolean openProject(Project p) {
        return p.getStatus() == ProjectStatus.PLANNING || p.getStatus() == ProjectStatus.IN_PROGRESS;
    }
    private boolean included(Task t) {
        for (Task current = t; current != null; current = current.getParentTask())
            if (current.getStatus() == TaskStatus.CANCELLED) return false;
        return true;
    }
    private boolean openTask(Task t) {
        return openProject(t.getProject()) && included(t)
                && (t.getStatus() == TaskStatus.PLANNING || t.getStatus() == TaskStatus.IN_PROGRESS);
    }
    private ApiDtos.TaskResponse task(Task t) { return ApiDtos.TaskResponse.from(t, progress); }
    private ApiDtos.ProjectResponse project(Project p) { return ApiDtos.ProjectResponse.from(p, progress); }
    private Object activeProfile() {
        Object p = account.activeProfile();
        if (p instanceof Homeowner h) authorization.requireActive(h);
        else authorization.requireActive((Tradesperson) p);
        return p;
    }
    private Homeowner homeowner() {
        if (activeProfile() instanceof Homeowner h) return h;
        throw new SecurityException("The active homeowner profile is required");
    }
    private Tradesperson worker() {
        if (activeProfile() instanceof Tradesperson t) return t;
        throw new SecurityException("The active tradesperson profile is required");
    }
}
