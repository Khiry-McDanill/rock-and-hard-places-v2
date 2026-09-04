package com.rockandhardplaces.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.rockandhardplaces.account.Homeowner;
import com.rockandhardplaces.account.HomeownerRepository;
import com.rockandhardplaces.account.Tradesperson;
import com.rockandhardplaces.account.TradespersonRepository;
import com.rockandhardplaces.account.User;
import com.rockandhardplaces.account.UserRepository;
import com.rockandhardplaces.catalog.Trade;
import com.rockandhardplaces.catalog.TradeRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BidPersistenceTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HomeownerRepository homeownerRepository;

    @Autowired
    private TradespersonRepository tradespersonRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

        @Autowired
        private TradeRepository tradeRepository;

        @Autowired
        private TaskTradeRepository taskTradeRepository;

    @Autowired
    private BidRepository bidRepository;

    @Test
    void persistsValidBidWithStatusAndTimestamps() {
        Task task = createTask("Valid bid task");
        TaskTrade taskTrade = createTaskTrade(task, "Carpentry");
        Tradesperson tradesperson = createTradesperson("valid@example.com", "Valid bidder");

        Bid savedBid = bidRepository.saveAndFlush(
                new Bid(task, taskTrade, tradesperson, new BigDecimal("1250.00"), "Can start next week"));

        assertThat(savedBid.getId()).isNotNull();
        assertThat(savedBid.getTask()).isEqualTo(task);
        assertThat(savedBid.getTaskTrade()).isEqualTo(taskTrade);
        assertThat(savedBid.getTradesperson()).isEqualTo(tradesperson);
        assertThat(savedBid.getAmount()).isEqualByComparingTo("1250.00");
        assertThat(savedBid.getStatus()).isEqualTo(BidStatus.SUBMITTED);
        assertThat(savedBid.getCreatedAt()).isNotNull();
        assertThat(savedBid.getUpdatedAt()).isNotNull();
    }

    @Test
    void requiresPositiveAmount() {
        Task task = createTask("Invalid amount task");
        TaskTrade taskTrade = createTaskTrade(task, "Carpentry");
        Tradesperson tradesperson = createTradesperson("invalid-amount@example.com", "Bidder");

        assertThatThrownBy(() -> bidRepository.saveAndFlush(
                new Bid(task, taskTrade, tradesperson, BigDecimal.ZERO, null)))
                .hasMessageContaining("greater than 0");
    }

    @Test
    void allowsOptionalMessage() {
        Task task = createTask("No message task");
        TaskTrade taskTrade = createTaskTrade(task, "Carpentry");
        Tradesperson tradesperson = createTradesperson("no-message@example.com", "Bidder");

        Bid savedBid = bidRepository.saveAndFlush(
                new Bid(task, taskTrade, tradesperson, new BigDecimal("500"), null));

        assertThat(savedBid.getMessage()).isNull();
    }

    @Test
    void allowsMultipleTradespeopleToBidOnOneTask() {
        Task task = createTask("Multiple bidders task");
        TaskTrade taskTrade = createTaskTrade(task, "Carpentry");
        Tradesperson firstBidder = createTradesperson("first@example.com", "First bidder");
        Tradesperson secondBidder = createTradesperson("second@example.com", "Second bidder");

        bidRepository.saveAndFlush(new Bid(task, taskTrade, firstBidder, new BigDecimal("100"), null));
        bidRepository.saveAndFlush(new Bid(task, taskTrade, secondBidder, new BigDecimal("200"), null));

        assertThat(bidRepository.findByTask(task)).hasSize(2);
    }

    @Test
    void allowsOneTradespersonToBidOnMultipleTasks() {
        Task firstTask = createTask("First task for bidder");
        Task secondTask = createTask("Second task for bidder");
        TaskTrade firstTaskTrade = createTaskTrade(firstTask, "Carpentry");
        TaskTrade secondTaskTrade = createTaskTrade(secondTask, "Plumbing");
        Tradesperson tradesperson = createTradesperson("multi-task@example.com", "Bidder");

        bidRepository.saveAndFlush(new Bid(firstTask, firstTaskTrade, tradesperson, new BigDecimal("100"), null));
        bidRepository.saveAndFlush(new Bid(secondTask, secondTaskTrade, tradesperson, new BigDecimal("200"), null));

        assertThat(bidRepository.findByTradesperson(tradesperson)).hasSize(2);
    }

    @Test
        void rejectsTaskTradeFromAnotherTask() {
                Task task = createTask("Bid task");
                Task otherTask = createTask("Other bid task");
                TaskTrade otherTaskTrade = createTaskTrade(otherTask, "Carpentry");
        Tradesperson tradesperson = createTradesperson("duplicate-bid@example.com", "Bidder");

        assertThatThrownBy(() -> bidRepository.saveAndFlush(
                                new Bid(task, otherTaskTrade, tradesperson, new BigDecimal("200"), "Offer")))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("TaskTrade must belong to the bid task");
        }

        @Test
        void preventsDuplicateSubmittedBidForTaskTradeAndTradesperson() {
                Task task = createTask("Duplicate submitted task");
                TaskTrade taskTrade = createTaskTrade(task, "Carpentry");
                Tradesperson tradesperson = createTradesperson("duplicate-bid@example.com", "Bidder");
                bidRepository.saveAndFlush(new Bid(task, taskTrade, tradesperson, new BigDecimal("100"), null));

                assertThatThrownBy(() -> bidRepository.saveAndFlush(
                                new Bid(task, taskTrade, tradesperson, new BigDecimal("200"), "Updated offer")))
                                .hasMessageContaining("UNIQUE constraint failed");
        }

        @Test
        void allowsSameTradespersonToBidOnDifferentTaskTrades() {
                Task task = createTask("Multiple scopes task");
                TaskTrade carpentry = createTaskTrade(task, "Carpentry");
                TaskTrade plumbing = createTaskTrade(task, "Plumbing");
                Tradesperson tradesperson = createTradesperson("multiple-scopes@example.com", "Bidder");

                bidRepository.saveAndFlush(new Bid(task, carpentry, tradesperson, new BigDecimal("100"), null));
                bidRepository.saveAndFlush(new Bid(task, plumbing, tradesperson, new BigDecimal("200"), null));

                assertThat(bidRepository.findByTask(task)).hasSize(2);
    }

    @Test
    void withdrawnBidAllowsNewSubmittedBid() {
        Task task = createTask("Resubmission task");
        TaskTrade taskTrade = createTaskTrade(task, "Carpentry");
        Tradesperson tradesperson = createTradesperson("resubmit@example.com", "Bidder");
        Bid withdrawnBid = bidRepository.saveAndFlush(
                new Bid(task, taskTrade, tradesperson, new BigDecimal("100"), "Original offer"));

        withdrawnBid.withdraw();
        bidRepository.saveAndFlush(withdrawnBid);
        Bid replacementBid = bidRepository.saveAndFlush(
                new Bid(task, taskTrade, tradesperson, new BigDecimal("125"), "New offer"));

        assertThat(replacementBid.getStatus()).isEqualTo(BidStatus.SUBMITTED);
        assertThat(bidRepository.findByTask(task)).extracting(Bid::getStatus)
                .containsExactlyInAnyOrder(BidStatus.WITHDRAWN, BidStatus.SUBMITTED);
    }

    @Test
    void keepsWithdrawnBidStoredAsHistory() {
        Task task = createTask("Historical bid task");
        TaskTrade taskTrade = createTaskTrade(task, "Carpentry");
        Tradesperson tradesperson = createTradesperson("history@example.com", "Bidder");
        Bid bid = bidRepository.saveAndFlush(
                new Bid(task, taskTrade, tradesperson, new BigDecimal("100"), "Historical offer"));

        bid.withdraw();
        bidRepository.saveAndFlush(bid);

        assertThat(bidRepository.findById(bid.getId())).get()
                .extracting(Bid::getStatus)
                .isEqualTo(BidStatus.WITHDRAWN);
    }

    private Task createTask(String title) {
        User user = userRepository.saveAndFlush(new User(title.toLowerCase()
                .replace(' ', '.') + "@example.com"));
        Homeowner homeowner = homeownerRepository.saveAndFlush(new Homeowner(user, "Project owner"));
        Project project = projectRepository.saveAndFlush(new Project(
                title + " project", "Complete the project", ProjectStatus.PLANNING,
                "90210", homeowner));
        return taskRepository.saveAndFlush(new Task(
                title, "Complete the task", TaskStatus.PLANNING, project, null));
    }

    private Tradesperson createTradesperson(String email, String displayName) {
        User user = userRepository.saveAndFlush(new User(email));
        return tradespersonRepository.saveAndFlush(new Tradesperson(user, displayName));
    }

        private TaskTrade createTaskTrade(Task task, String tradeName) {
                Trade trade = tradeRepository.saveAndFlush(new Trade(tradeName));
                return taskTradeRepository.saveAndFlush(new TaskTrade(task, trade));
        }
}