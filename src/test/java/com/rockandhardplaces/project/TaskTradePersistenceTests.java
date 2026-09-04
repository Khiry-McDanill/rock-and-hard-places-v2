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
import com.rockandhardplaces.account.User;
import com.rockandhardplaces.account.UserRepository;
import com.rockandhardplaces.catalog.Specialty;
import com.rockandhardplaces.catalog.SpecialtyRepository;
import com.rockandhardplaces.catalog.Trade;
import com.rockandhardplaces.catalog.TradeRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskTradePersistenceTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HomeownerRepository homeownerRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private TaskTradeRepository taskTradeRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

        @PersistenceContext
        private EntityManager entityManager;

    @Test
    void allowsOneTaskToRequireMultipleTrades() {
        Task task = createTask("Kitchen remodel task");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Trade plumbing = tradeRepository.saveAndFlush(new Trade("Plumbing"));

        taskTradeRepository.saveAndFlush(new TaskTrade(task, carpentry));
        taskTradeRepository.saveAndFlush(new TaskTrade(task, plumbing));

        assertThat(taskTradeRepository.findByTask(task))
                .extracting(TaskTrade::getTrade)
                .extracting(Trade::getName)
                .containsExactlyInAnyOrder("Carpentry", "Plumbing");
    }

    @Test
    void allowsOneTradeToBeRequiredByMultipleTasks() {
        Task firstTask = createTask("Remove cabinets task");
        Task secondTask = createTask("Install cabinets task");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));

        taskTradeRepository.saveAndFlush(new TaskTrade(firstTask, carpentry));
        taskTradeRepository.saveAndFlush(new TaskTrade(secondTask, carpentry));

        assertThat(taskTradeRepository.findByTrade(carpentry))
                .extracting(TaskTrade::getTask)
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Remove cabinets task", "Install cabinets task");
    }

    @Test
    void rejectsDuplicateTaskTradeRequirement() {
        Task task = createTask("Duplicate requirement task");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        taskTradeRepository.saveAndFlush(new TaskTrade(task, carpentry));

        assertThatThrownBy(() -> taskTradeRepository.saveAndFlush(new TaskTrade(task, carpentry)))
                .hasMessageContaining("UNIQUE constraint failed");
    }

    @Test
    void retrievesRequirementsThroughBothInverseMappings() {
        Task task = createTask("Retrieve requirements task");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        TaskTrade requirement = taskTradeRepository.saveAndFlush(new TaskTrade(task, carpentry));
        entityManager.clear();

        assertThat(taskRepository.findById(task.getId()).orElseThrow().getTaskTrades())
                .extracting(TaskTrade::getId)
                .containsExactly(requirement.getId());
        assertThat(tradeRepository.findById(carpentry.getId()).orElseThrow().getTaskTrades())
                .extracting(TaskTrade::getId)
                .containsExactly(requirement.getId());
    }

    @Test
    void taskTradeDoesNotRepresentWorkerAssignment() {
        assertThat(TaskTrade.class.getDeclaredFields())
                .extracting(Field::getName)
                .containsExactlyInAnyOrder("id", "task", "trade");
        assertThat(Task.class.getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("tradesperson", "tradespersonId", "worker", "workerId",
                        "assignment", "assignments");
    }

    @Test
    void existingTradeSpecialtyRelationshipRemainsIntact() {
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));
        Specialty specialty = specialtyRepository.saveAndFlush(new Specialty("Framing", carpentry));

        assertThat(specialtyRepository.findById(specialty.getId()))
                .get()
                .extracting(Specialty::getTrade)
                .extracting(Trade::getId)
                .isEqualTo(carpentry.getId());
    }

    private Task createTask(String title) {
        User user = userRepository.saveAndFlush(new User(title.toLowerCase().replace(' ', '.') + "@example.com"));
        Homeowner homeowner = homeownerRepository.saveAndFlush(new Homeowner(user, "Project owner"));
        Project project = projectRepository.saveAndFlush(new Project(
                title + " project", "Complete the project", ProjectStatus.PLANNING,
                "90210", homeowner));
        return taskRepository.saveAndFlush(new Task(
                title, "Complete the task", TaskStatus.PLANNING, project, null));
    }
}