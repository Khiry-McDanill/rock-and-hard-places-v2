package com.rockandhardplaces.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.rockandhardplaces.account.Homeowner;
import com.rockandhardplaces.account.HomeownerRepository;
import com.rockandhardplaces.account.PersonTrade;
import com.rockandhardplaces.account.PersonTradeRepository;
import com.rockandhardplaces.account.Tradesperson;
import com.rockandhardplaces.account.TradespersonRepository;
import com.rockandhardplaces.account.User;
import com.rockandhardplaces.account.UserRepository;
import com.rockandhardplaces.catalog.Trade;
import com.rockandhardplaces.catalog.TradeRepository;

/**
 * Focused tests for TaskAssignmentService eligibility validation.
 *
 * These tests verify that assignment eligibility rules are properly enforced:
 * 1. ACTIVE ProjectTeam membership is required.
 * 2. PersonTrade qualification matching TaskTrade is required (if requirements
 * exist).
 * 3. Multiple workers can satisfy different trade requirements.
 * 4. One worker can satisfy multiple trade requirements.
 * 5. Unassigned tasks remain valid.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TaskAssignmentService.class)
class TaskAssignmentServiceTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HomeownerRepository homeownerRepository;

    @Autowired
    private TradespersonRepository tradespersonRepository;

    @Autowired
    private PersonTradeRepository personTradeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private ProjectTeamRepository projectTeamRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private TaskTradeRepository taskTradeRepository;

    @Autowired
    private TaskAssignmentService taskAssignmentService;

    @PersistenceContext
    private EntityManager entityManager;

    // ===== Test: ACTIVE Membership Required =====

    @Test
    void assignmentRequiresActiveMembership() {
        Project project = createProject("Membership required project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("active-member@example.com", "Active Worker");
        Task task = createTask(project, "Membership task", TaskStatus.PLANNING);

        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));

        // Should not throw when ACTIVE
        taskAssignmentService.validateAssignmentEligibility(task, worker);
    }

    @Test
    void rejectsAssignmentWhenMembershipInvited() {
        Project project = createProject("Invited member project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("invited@example.com", "Invited Worker");
        Task task = createTask(project, "Task for invited", TaskStatus.PLANNING);

        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.INVITED));

        assertThatThrownBy(
                () -> taskAssignmentService.validateAssignmentEligibility(task, worker))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no ACTIVE ProjectTeam membership");
    }

    @Test
    void rejectsAssignmentWhenMembershipPending() {
        Project project = createProject("Pending member project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("pending@example.com", "Pending Worker");
        Task task = createTask(project, "Task for pending", TaskStatus.PLANNING);

        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.PENDING));

        assertThatThrownBy(
                () -> taskAssignmentService.validateAssignmentEligibility(task, worker))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no ACTIVE ProjectTeam membership");
    }

    @Test
    void rejectsAssignmentWhenMembershipSuspended() {
        Project project = createProject("Suspended member project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("suspended@example.com", "Suspended Worker");
        Task task = createTask(project, "Task for suspended", TaskStatus.PLANNING);

        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.SUSPENDED));

        assertThatThrownBy(
                () -> taskAssignmentService.validateAssignmentEligibility(task, worker))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no ACTIVE ProjectTeam membership");
    }

    @Test
    void rejectsAssignmentWhenNoMembership() {
        Project project = createProject("No membership project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("nonmember@example.com", "Non-member Worker");
        Task task = createTask(project, "Task for non-member", TaskStatus.PLANNING);

        assertThatThrownBy(
                () -> taskAssignmentService.validateAssignmentEligibility(task, worker))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no ACTIVE ProjectTeam membership");
    }

    // ===== Test: Qualification Matching =====

    @Test
    void assignmentAllowedWhenNoTaskRequirements() {
        Project project = createProject("No requirements project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("unqualified@example.com", "Unqualified");
        Task task = createTask(project, "Task with no requirements", TaskStatus.PLANNING);

        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));

        // Should not throw when no requirements exist
        taskAssignmentService.validateAssignmentEligibility(task, worker);
    }

    @Test
    void assignmentAllowedWhenQualificationMatches() {
        Project project = createProject("Qualified project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("qualified@example.com", "Qualified");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Task task = createTask(project, "Carpentry task", TaskStatus.PLANNING);

        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));
        personTradeRepository.saveAndFlush(new PersonTrade(worker, carpentry));
        taskTradeRepository.saveAndFlush(new TaskTrade(task, carpentry));

        // Should not throw when qualification matches
        taskAssignmentService.validateAssignmentEligibility(task, worker);
    }

    @Test
    void rejectsAssignmentWhenNoMatchingQualification() {
        Project project = createProject("Mismatched qualifications project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("mismatch@example.com", "Mismatched");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Trade plumbing = tradeRepository.saveAndFlush(new Trade("Plumbing"));
        Task task = createTask(project, "Carpentry task", TaskStatus.PLANNING);

        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));
        personTradeRepository.saveAndFlush(new PersonTrade(worker, plumbing));
        taskTradeRepository.saveAndFlush(new TaskTrade(task, carpentry));
        entityManager.clear();

        Task refreshedTask = taskRepository.findById(task.getId()).orElseThrow();

        assertThatThrownBy(
                () -> taskAssignmentService.validateAssignmentEligibility(refreshedTask, worker))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no matching qualifications");
    }

    // ===== Test: Multi-Trade Scenarios =====

    @Test
    void multipleWorkersSatisfyingDifferentTradesOnOneTask() {
        Project project = createProject("Multi-trade project", ProjectStatus.PLANNING);
        Tradesperson carpenter = createTradesperson("carpenter@example.com", "Carpenter");
        Tradesperson plumber = createTradesperson("plumber@example.com", "Plumber");

        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Trade plumbing = tradeRepository.saveAndFlush(new Trade("Plumbing"));

        Task task = createTask(project, "Kitchen remodel", TaskStatus.PLANNING);

        // Setup memberships
        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, carpenter, ProjectTeamStatus.ACTIVE));
        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, plumber, ProjectTeamStatus.ACTIVE));

        // Setup qualifications
        personTradeRepository.saveAndFlush(new PersonTrade(carpenter, carpentry));
        personTradeRepository.saveAndFlush(new PersonTrade(plumber, plumbing));

        // Setup task requirements
        taskTradeRepository.saveAndFlush(new TaskTrade(task, carpentry));
        taskTradeRepository.saveAndFlush(new TaskTrade(task, plumbing));

        // Both workers should be eligible
        assertThat(taskAssignmentService.isEligibleForAssignment(task, carpenter)).isTrue();
        assertThat(taskAssignmentService.isEligibleForAssignment(task, plumber)).isTrue();

        // Both can be assigned
        taskAssignmentService.createEligibleAssignment(task, carpenter);
        taskAssignmentService.createEligibleAssignment(task, plumber);

        assertThat(taskAssignmentRepository.findByTask(task))
                .extracting(TaskAssignment::getTradesperson)
                .extracting(Tradesperson::getDisplayName)
                .containsExactlyInAnyOrder("Carpenter", "Plumber");
    }

    @Test
    void oneWorkerSatisfyingMultipleTradesOnOneTask() {
        Project project = createProject("Multi-qualified project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("multi-skilled@example.com", "Multi-skilled");

        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Trade plumbing = tradeRepository.saveAndFlush(new Trade("Plumbing"));

        Task task = createTask(project, "Kitchen full remodel", TaskStatus.PLANNING);

        // Setup membership
        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));

        // Setup multiple qualifications
        personTradeRepository.saveAndFlush(new PersonTrade(worker, carpentry));
        personTradeRepository.saveAndFlush(new PersonTrade(worker, plumbing));

        // Setup task requirements
        taskTradeRepository.saveAndFlush(new TaskTrade(task, carpentry));
        taskTradeRepository.saveAndFlush(new TaskTrade(task, plumbing));

        // Worker should be eligible (qualifies for at least one required trade)
        assertThat(taskAssignmentService.isEligibleForAssignment(task, worker)).isTrue();

        // Single assignment covers all work
        taskAssignmentService.createEligibleAssignment(task, worker);
        assertThat(taskAssignmentRepository.findByTask(task))
                .hasSize(1)
                .extracting(TaskAssignment::getTradesperson)
                .extracting(Tradesperson::getId)
                .containsExactly(worker.getId());
    }

    // ===== Test: Cross-Project Prevention =====

    @Test
    void preventsCrossProjectAssignment() {
        Project project1 = createProject("Project 1", ProjectStatus.PLANNING);
        Project project2 = createProject("Project 2", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("worker@example.com", "Worker");

        Task task1 = createTask(project1, "Task in project 1", TaskStatus.PLANNING);

        // Worker is member of project1, NOT project2
        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project1, worker, ProjectTeamStatus.ACTIVE));

        // Attempting to assign to task in project1 should succeed
        assertThat(taskAssignmentService.isEligibleForAssignment(task1, worker)).isTrue();

        // Cannot assign to project2 task (not a member)
        Task task2 = createTask(project2, "Task in project 2", TaskStatus.PLANNING);
        assertThat(taskAssignmentService.isEligibleForAssignment(task2, worker)).isFalse();
    }

    // ===== Test: Unassigned Tasks =====

    @Test
    void unassignedTasksRemainValid() {
        Project project = createProject("Unassigned task project", ProjectStatus.PLANNING);
        Task task = createTask(project, "Unassigned work", TaskStatus.PLANNING);

        // Task should persist with no assignments
        assertThat(taskRepository.findById(task.getId()).orElseThrow().getTaskAssignments())
                .isEmpty();

        // Task should be queryable and valid
        assertThat(task.getTitle()).isEqualTo("Unassigned work");
        assertThat(task.getProject()).isNotNull();
    }

    // ===== Test: Service Methods =====

    @Test
    void createEligibleAssignmentPersistsAssignment() {
        Project project = createProject("Create assignment project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("create@example.com", "Creator");
        Task task = createTask(project, "Create assignment task", TaskStatus.PLANNING);

        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));

        TaskAssignment created = taskAssignmentService.createEligibleAssignment(task, worker);

        assertThat(created).isNotNull();
        assertThat(taskAssignmentRepository.findByTask(task))
                .extracting(TaskAssignment::getId)
                .containsExactly(created.getId());
    }

    @Test
    void isEligibleForAssignmentReturnsTrueWhenEligible() {
        Project project = createProject("Eligible check project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("eligible@example.com", "Eligible");
        Task task = createTask(project, "Eligible task", TaskStatus.PLANNING);

        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));

        assertThat(taskAssignmentService.isEligibleForAssignment(task, worker)).isTrue();
    }

    @Test
    void isEligibleForAssignmentReturnsFalseWhenIneligible() {
        Project project = createProject("Ineligible check project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("ineligible@example.com", "Ineligible");
        Task task = createTask(project, "Ineligible task", TaskStatus.PLANNING);

        // No membership created
        assertThat(taskAssignmentService.isEligibleForAssignment(task, worker)).isFalse();
    }

    @Test
    void findIneligibleAssignmentsByLostMembership() {
        Project project = createProject("Lost membership project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("lost-member@example.com", "Lost Member");
        Task task1 = createTask(project, "Task 1", TaskStatus.PLANNING);
        Task task2 = createTask(project, "Task 2", TaskStatus.PLANNING);

        // Create membership and assignments
        ProjectTeam membership = projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));
        taskAssignmentRepository.saveAndFlush(new TaskAssignment(task1, worker));
        taskAssignmentRepository.saveAndFlush(new TaskAssignment(task2, worker));

        // Suspend membership
        membership.setStatus(ProjectTeamStatus.SUSPENDED);
        projectTeamRepository.saveAndFlush(membership);
        entityManager.clear();

        // Both assignments should now be ineligible
        List<TaskAssignment> ineligible = taskAssignmentService
                .findIneligibleAssignmentsByLostMembership(worker, project);

        assertThat(ineligible).hasSize(2);
    }

    @Test
    void findIneligibleAssignmentsByLostQualification() {
        Project project = createProject("Lost qualification project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("unqualified@example.com", "Unqualified");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Task task1 = createTask(project, "Carpentry task", TaskStatus.PLANNING);
        Task task2 = createTask(project, "Generic task", TaskStatus.PLANNING);

        // Create membership, qualification, and assignments
        projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));
        PersonTrade qualification = personTradeRepository.saveAndFlush(
                new PersonTrade(worker, carpentry));
        taskTradeRepository.saveAndFlush(new TaskTrade(task1, carpentry));
        taskAssignmentRepository.saveAndFlush(new TaskAssignment(task1, worker));
        taskAssignmentRepository.saveAndFlush(new TaskAssignment(task2, worker));
        
        entityManager.flush();
        entityManager.clear();

        // Remove qualification
        personTradeRepository.delete(qualification);
        entityManager.flush();
        entityManager.clear();

        // Reload worker to get fresh state
        Tradesperson refreshedWorker = tradespersonRepository.findById(worker.getId()).orElseThrow();

        // Only task1 assignment should be ineligible (task2 has no requirements)
        List<TaskAssignment> ineligible = taskAssignmentService
                .findIneligibleAssignmentsByLostQualification(refreshedWorker);

        assertThat(ineligible).hasSize(1)
                .extracting(TaskAssignment::getTask)
                .extracting(Task::getId)
                .containsExactly(task1.getId());
    }

    // ===== Helper Methods =====

    private Project createProject(String title, ProjectStatus status) {
        User user = userRepository.saveAndFlush(new User(title.toLowerCase().replace(' ', '.') + "@example.com"));
        Homeowner homeowner = homeownerRepository.saveAndFlush(new Homeowner(user, "Project owner"));
        return projectRepository.saveAndFlush(
                new Project(title, "Project description", status, "90210", homeowner));
    }

    private Tradesperson createTradesperson(String email, String displayName) {
        User user = userRepository.saveAndFlush(new User(email));
        return tradespersonRepository.saveAndFlush(new Tradesperson(user, displayName));
    }

    private Task createTask(Project project, String title, TaskStatus status) {
        return taskRepository.saveAndFlush(
                new Task(title, "Task description", status, project, null));
    }
}
