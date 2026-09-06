package com.rockandhardplaces.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.rockandhardplaces.account.AccountAuthorizationService;
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
@Import({
        BidAcceptanceService.class,
        AccountAuthorizationService.class
})

class BidAcceptanceServiceTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HomeownerRepository homeownerRepository;

    @Autowired
    private TradespersonRepository tradespersonRepository;

    @Autowired
    private PersonTradeRepository personTradeRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskTradeRepository taskTradeRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private ProjectTeamRepository projectTeamRepository;

    @Autowired
    private ProjectTeamTradeRepository projectTeamTradeRepository;

    @Autowired
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private BidAcceptanceService bidAcceptanceService;

    @Test
    void acceptsValidSubmittedBidAndCreatesRelationships() {
        Task task = createTask("Accept valid bid");
        Tradesperson worker = createTradesperson("valid-accept@example.com");
        TaskTrade taskTrade = createQualifiedTaskTrade(task, worker, "Carpentry");
        Bid bid = createBid(task, taskTrade, worker);

        bidAcceptanceService.acceptBid(bid);

        assertThat(bid.getStatus()).isEqualTo(BidStatus.ACCEPTED);
        assertThat(projectTeamRepository.findByProjectAndTradesperson(task.getProject(), worker))
                .get().extracting(ProjectTeam::getStatus).isEqualTo(ProjectTeamStatus.ACTIVE);
        assertThat(projectTeamTradeRepository.findByProjectTeam(
                projectTeamRepository.findByProjectAndTradesperson(task.getProject(), worker).orElseThrow()))
                .extracting(ProjectTeamTrade::getTrade).extracting(Trade::getName)
                .containsExactly("Carpentry");
        assertThat(taskAssignmentRepository.findByTaskAndTradesperson(task, worker)).isPresent();
    }

    @Test
    void activatesInvitedMembership() {
        Task task = createTask("Activate invited membership");
        Tradesperson worker = createTradesperson("invited-accept@example.com");
        TaskTrade taskTrade = createQualifiedTaskTrade(task, worker, "Carpentry");
        ProjectTeam membership = projectTeamRepository.saveAndFlush(
                new ProjectTeam(task.getProject(), worker, ProjectTeamStatus.INVITED));

        bidAcceptanceService.acceptBid(createBid(task, taskTrade, worker));

        assertThat(projectTeamRepository.findById(membership.getId()).orElseThrow().getStatus())
                .isEqualTo(ProjectTeamStatus.ACTIVE);
    }

    @Test
    void activatesPendingMembership() {
        Task task = createTask("Activate pending membership");
        Tradesperson worker = createTradesperson("pending-accept@example.com");
        TaskTrade taskTrade = createQualifiedTaskTrade(task, worker, "Carpentry");
        ProjectTeam membership = projectTeamRepository.saveAndFlush(
                new ProjectTeam(task.getProject(), worker, ProjectTeamStatus.PENDING));

        bidAcceptanceService.acceptBid(createBid(task, taskTrade, worker));

        assertThat(projectTeamRepository.findById(membership.getId()).orElseThrow().getStatus())
                .isEqualTo(ProjectTeamStatus.ACTIVE);
    }

    @Test
    void reusesActiveMembership() {
        Task task = createTask("Reuse active membership");
        Tradesperson worker = createTradesperson("active-accept@example.com");
        TaskTrade taskTrade = createQualifiedTaskTrade(task, worker, "Carpentry");
        ProjectTeam membership = projectTeamRepository.saveAndFlush(
                new ProjectTeam(task.getProject(), worker, ProjectTeamStatus.ACTIVE));

        bidAcceptanceService.acceptBid(createBid(task, taskTrade, worker));

        assertThat(projectTeamRepository.findByProjectAndTradesperson(task.getProject(), worker))
                .get().extracting(ProjectTeam::getId).isEqualTo(membership.getId());
        assertThat(projectTeamRepository.findByProject(task.getProject())).hasSize(1);
    }

    @Test
    void rejectsSuspendedMembership() {
        Task task = createTask("Reject suspended membership");
        Tradesperson worker = createTradesperson("suspended-accept@example.com");
        TaskTrade taskTrade = createQualifiedTaskTrade(task, worker, "Carpentry");
        projectTeamRepository.saveAndFlush(
                new ProjectTeam(task.getProject(), worker, ProjectTeamStatus.SUSPENDED));
        Bid bid = createBid(task, taskTrade, worker);

        assertThatThrownBy(() -> bidAcceptanceService.acceptBid(bid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Suspended");
        assertThat(bid.getStatus()).isEqualTo(BidStatus.SUBMITTED);
    }

    @Test
    void doesNotAddUnrelatedTradeRole() {
        Task task = createTask("Keep unrelated role");
        Tradesperson worker = createTradesperson("unrelated-role@example.com");
        Trade plumbing = createTrade("Plumbing");
        TaskTrade taskTrade = createQualifiedTaskTrade(task, worker, "Carpentry");
        ProjectTeam membership = projectTeamRepository.saveAndFlush(
                new ProjectTeam(task.getProject(), worker, ProjectTeamStatus.ACTIVE));
        projectTeamTradeRepository.saveAndFlush(new ProjectTeamTrade(membership, plumbing));

        bidAcceptanceService.acceptBid(createBid(task, taskTrade, worker));

        assertThat(projectTeamTradeRepository.findByProjectTeam(membership))
                .extracting(ProjectTeamTrade::getTrade).extracting(Trade::getName)
                .containsExactlyInAnyOrder("Plumbing", "Carpentry");
    }

    @Test
    void rejectsCompetingBidsOnlyForTheSameTaskTrade() {
        Task task = createTask("Reject competing bids");
        Tradesperson firstWorker = createTradesperson("first-competition@example.com");
        Tradesperson secondWorker = createTradesperson("second-competition@example.com");
        TaskTrade carpentry = createQualifiedTaskTrade(task, firstWorker, "Carpentry");
        personTradeRepository.saveAndFlush(new PersonTrade(secondWorker, carpentry.getTrade()));
        TaskTrade plumbing = createQualifiedTaskTrade(task, firstWorker, "Plumbing");
        personTradeRepository.saveAndFlush(new PersonTrade(secondWorker, plumbing.getTrade()));
        Bid selected = createBid(task, carpentry, firstWorker);
        Bid competing = createBid(task, carpentry, secondWorker);
        Bid otherScope = createBid(task, plumbing, secondWorker);

        bidAcceptanceService.acceptBid(selected);

        assertThat(competing.getStatus()).isEqualTo(BidStatus.REJECTED);
        assertThat(otherScope.getStatus()).isEqualTo(BidStatus.SUBMITTED);
    }

    @Test
    void preventsSecondAcceptedBidForTaskTrade() {
        Task task = createTask("Prevent second accepted bid");
        Tradesperson firstWorker = createTradesperson("first-accepted@example.com");
        Tradesperson secondWorker = createTradesperson("second-accepted@example.com");
        TaskTrade taskTrade = createQualifiedTaskTrade(task, firstWorker, "Carpentry");
        personTradeRepository.saveAndFlush(new PersonTrade(secondWorker, taskTrade.getTrade()));
        Bid firstBid = createBid(task, taskTrade, firstWorker);

        bidAcceptanceService.acceptBid(firstBid);
        Bid secondBid = createBid(task, taskTrade, secondWorker);

        assertThatThrownBy(() -> bidAcceptanceService.acceptBid(secondBid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has an accepted bid");
    }

    @Test
    void repeatedAcceptanceDoesNotCreateDuplicateRelationships() {
        Task task = createTask("Repeat acceptance");
        Tradesperson worker = createTradesperson("repeat-accept@example.com");
        TaskTrade taskTrade = createQualifiedTaskTrade(task, worker, "Carpentry");
        Bid bid = createBid(task, taskTrade, worker);

        bidAcceptanceService.acceptBid(bid);
        bidAcceptanceService.acceptBid(bid);

        ProjectTeam membership = projectTeamRepository
                .findByProjectAndTradesperson(task.getProject(), worker).orElseThrow();
        assertThat(projectTeamRepository.findByProject(task.getProject())).hasSize(1);
        assertThat(projectTeamTradeRepository.findByProjectTeam(membership)).hasSize(1);
        assertThat(taskAssignmentRepository.findByTask(task)).hasSize(1);
        assertThat(bidRepository.findByTaskTradeAndStatus(taskTrade, BidStatus.ACCEPTED)).hasSize(1);
    }

    @Test
    void rejectsUnqualifiedTradesperson() {
        Task task = createTask("Reject unqualified bid");
        Tradesperson worker = createTradesperson("unqualified-accept@example.com");
        TaskTrade taskTrade = createTaskTrade(task, "Carpentry");
        Bid bid = createBid(task, taskTrade, worker);

        assertThatThrownBy(() -> bidAcceptanceService.acceptBid(bid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not qualified");
        assertThat(bid.getStatus()).isEqualTo(BidStatus.SUBMITTED);
        assertThat(projectTeamRepository.findByProject(task.getProject())).isEmpty();
    }

    @Test
    void rejectsNonSubmittedBid() {
        Task task = createTask("Reject withdrawn bid");
        Tradesperson worker = createTradesperson("withdrawn-accept@example.com");
        TaskTrade taskTrade = createQualifiedTaskTrade(task, worker, "Carpentry");
        Bid bid = createBid(task, taskTrade, worker);
        bid.withdraw();
        bidRepository.saveAndFlush(bid);

        assertThatThrownBy(() -> bidAcceptanceService.acceptBid(bid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only a submitted bid");
    }

    private Bid createBid(Task task, TaskTrade taskTrade, Tradesperson worker) {
        return bidRepository.saveAndFlush(
                new Bid(task, taskTrade, worker, new BigDecimal("100.00"), "Offer"));
    }

    private TaskTrade createQualifiedTaskTrade(Task task, Tradesperson worker, String tradeName) {
        Trade trade = createTrade(tradeName);
        personTradeRepository.saveAndFlush(new PersonTrade(worker, trade));
        return taskTradeRepository.saveAndFlush(new TaskTrade(task, trade));
    }

    private TaskTrade createTaskTrade(Task task, String tradeName) {
        return taskTradeRepository.saveAndFlush(new TaskTrade(task, createTrade(tradeName)));
    }

    private Trade createTrade(String name) {
        return tradeRepository.saveAndFlush(new Trade(name));
    }

    private Task createTask(String title) {
        User user = userRepository.saveAndFlush(new User(title.toLowerCase().replace(' ', '.') + "@example.com"));
        Homeowner homeowner = homeownerRepository.saveAndFlush(new Homeowner(user, "Project owner"));
        Project project = projectRepository.saveAndFlush(new Project(
                title + " project", "Complete the project", ProjectStatus.PLANNING, "90210", homeowner));
        return taskRepository.saveAndFlush(new Task(
                title, "Complete the task", TaskStatus.PLANNING, project, null));
    }

    private Tradesperson createTradesperson(String email) {
        User user = userRepository.saveAndFlush(new User(email));
        return tradespersonRepository.saveAndFlush(new Tradesperson(user, "Bidder"));
    }
}