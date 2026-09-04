package com.rockandhardplaces.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProjectTeamPersistenceTests {

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
    private ProjectTeamRepository projectTeamRepository;

    @Autowired
    private ProjectTeamTradeRepository projectTeamTradeRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void allowsOneProjectToHaveMultipleTeamMembers() {
        Project project = createProject("Multi-member project", ProjectStatus.PLANNING);
        Tradesperson first = createTradesperson("team-first@example.com", "First worker");
        Tradesperson second = createTradesperson("team-second@example.com", "Second worker");

        projectTeamRepository.saveAndFlush(new ProjectTeam(project, first, ProjectTeamStatus.ACTIVE));
        projectTeamRepository.saveAndFlush(new ProjectTeam(project, second, ProjectTeamStatus.PENDING));

        assertThat(projectTeamRepository.findByProject(project))
                .extracting(ProjectTeam::getTradesperson)
                .extracting(Tradesperson::getDisplayName)
                .containsExactlyInAnyOrder("First worker", "Second worker");
    }

    @Test
    void allowsOneTradespersonToBelongToMultipleProjects() {
        Project firstProject = createProject("First team project", ProjectStatus.PLANNING);
        Project secondProject = createProject("Second team project", ProjectStatus.IN_PROGRESS);
        Tradesperson worker = createTradesperson("multi-project@example.com", "Shared worker");

        projectTeamRepository.saveAndFlush(new ProjectTeam(
                firstProject, worker, ProjectTeamStatus.ACTIVE));
        projectTeamRepository.saveAndFlush(new ProjectTeam(
                secondProject, worker, ProjectTeamStatus.INVITED));

        assertThat(projectTeamRepository.findByTradesperson(worker))
                .extracting(ProjectTeam::getProject)
                .extracting(Project::getTitle)
                .containsExactlyInAnyOrder("First team project", "Second team project");
    }

    @Test
    void rejectsDuplicateProjectTradespersonMembership() {
        Project project = createProject("Duplicate membership project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("duplicate-member@example.com", "Worker");
        projectTeamRepository.saveAndFlush(new ProjectTeam(project, worker, ProjectTeamStatus.PENDING));

        assertThatThrownBy(() -> projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE)))
                .hasMessageContaining("UNIQUE constraint failed");
    }

    @Test
    void persistsMembershipStatusIndependentlyFromProjectStatus() {
        Project project = createProject("Status project", ProjectStatus.IN_PROGRESS);
        Tradesperson worker = createTradesperson("status-member@example.com", "Worker");

        ProjectTeam membership = projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.PENDING));

        assertThat(projectRepository.findById(project.getId()).orElseThrow().getStatus())
                .isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(projectTeamRepository.findById(membership.getId()).orElseThrow().getStatus())
                .isEqualTo(ProjectTeamStatus.PENDING);
    }

    @Test
    void allowsOneTeamMemberToHaveMultipleTradeRoles() {
        Project project = createProject("Multiple roles project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("multiple-roles@example.com", "Multi-role worker");
        ProjectTeam membership = projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Trade plumbing = tradeRepository.saveAndFlush(new Trade("Plumbing"));

        projectTeamTradeRepository.saveAndFlush(new ProjectTeamTrade(membership, carpentry));
        projectTeamTradeRepository.saveAndFlush(new ProjectTeamTrade(membership, plumbing));

        assertThat(projectTeamTradeRepository.findByProjectTeam(membership))
                .extracting(ProjectTeamTrade::getTrade)
                .extracting(Trade::getName)
                .containsExactlyInAnyOrder("Carpentry", "Plumbing");
    }

    @Test
    void rejectsDuplicateProjectTeamTradeRole() {
        Project project = createProject("Duplicate role project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("duplicate-role@example.com", "Worker");
        ProjectTeam membership = projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        projectTeamTradeRepository.saveAndFlush(new ProjectTeamTrade(membership, carpentry));

        assertThatThrownBy(() -> projectTeamTradeRepository.saveAndFlush(
                new ProjectTeamTrade(membership, carpentry)))
                .hasMessageContaining("UNIQUE constraint failed");
    }

    @Test
    void projectTeamAndRolesHaveInverseMappings() {
        Project project = createProject("Inverse team project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("inverse-team@example.com", "Worker");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        ProjectTeam membership = projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));
        ProjectTeamTrade role = projectTeamTradeRepository.saveAndFlush(
                new ProjectTeamTrade(membership, carpentry));
        entityManager.clear();

        assertThat(projectRepository.findById(project.getId()).orElseThrow().getProjectTeams())
                .extracting(ProjectTeam::getId)
                .containsExactly(membership.getId());
        assertThat(tradespersonRepository.findById(worker.getId()).orElseThrow().getProjectTeams())
                .extracting(ProjectTeam::getId)
                .containsExactly(membership.getId());
        assertThat(projectTeamRepository.findById(membership.getId()).orElseThrow()
                .getProjectTeamTrades())
                .extracting(ProjectTeamTrade::getId)
                .containsExactly(role.getId());
        assertThat(tradeRepository.findById(carpentry.getId()).orElseThrow()
                .getProjectTeamTrades())
                .extracting(ProjectTeamTrade::getId)
                .containsExactly(role.getId());
    }

    @Test
    void personTradeQualificationRemainsIndependentFromProjectRole() {
        Project project = createProject("Qualification distinction project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("qualification-role@example.com", "Worker");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Trade plumbing = tradeRepository.saveAndFlush(new Trade("Plumbing"));
        personTradeRepository.saveAndFlush(new PersonTrade(worker, plumbing));
        ProjectTeam membership = projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));
        ProjectTeamTrade role = projectTeamTradeRepository.saveAndFlush(
                new ProjectTeamTrade(membership, carpentry));

        assertThat(personTradeRepository.findByTradesperson(worker))
                .extracting(PersonTrade::getTrade)
                .extracting(Trade::getName)
                .containsExactly("Plumbing");
        assertThat(projectTeamTradeRepository.findByProjectTeam(membership))
                .containsExactly(role);
    }

    @Test
    void taskAssignmentRemainsIndependentFromProjectTeam() {
        Project project = createProject("Assignment distinction project", ProjectStatus.PLANNING);
        Tradesperson worker = createTradesperson("assignment-team@example.com", "Worker");
        Task task = taskRepository.saveAndFlush(new Task(
                "Assigned task", "Perform assigned work", TaskStatus.PLANNING, project, null));
        TaskAssignment assignment = taskAssignmentRepository.saveAndFlush(
                new TaskAssignment(task, worker));

        assertThat(projectTeamRepository.findByProject(project)).isEmpty();
        ProjectTeam membership = projectTeamRepository.saveAndFlush(
                new ProjectTeam(project, worker, ProjectTeamStatus.ACTIVE));

        assertThat(taskAssignmentRepository.findByTask(task))
                .extracting(TaskAssignment::getId)
                .containsExactly(assignment.getId());
        assertThat(projectTeamRepository.findById(membership.getId())).isPresent();
    }

    @Test
        void projectTeamDoesNotStoreADirectTradeField() {
        assertThat(ProjectTeam.class.getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("trade", "tradeId");
    }

    private Project createProject(String title, ProjectStatus status) {
        User user = userRepository.saveAndFlush(new User(title.toLowerCase()
                .replace(' ', '.') + "@example.com"));
        Homeowner homeowner = homeownerRepository.saveAndFlush(new Homeowner(user, "Project owner"));
        return projectRepository.saveAndFlush(new Project(
                title, "Complete the project", status, "90210", homeowner));
    }

    private Tradesperson createTradesperson(String email, String displayName) {
        User user = userRepository.saveAndFlush(new User(email));
        return tradespersonRepository.saveAndFlush(new Tradesperson(user, displayName));
    }
}