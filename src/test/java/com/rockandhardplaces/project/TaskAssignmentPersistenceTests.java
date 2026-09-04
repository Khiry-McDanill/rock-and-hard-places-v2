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
class TaskAssignmentPersistenceTests {

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
    private TaskAssignmentRepository taskAssignmentRepository;

    @Autowired
    private TaskTradeRepository taskTradeRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsTaskWithZeroAssignments() {
        Task task = createTask("Unassigned task");

        assertThat(taskAssignmentRepository.findByTask(task)).isEmpty();
        assertThat(taskRepository.findById(task.getId()).orElseThrow().getTaskAssignments())
                .isEmpty();
    }

    @Test
    void allowsOneTaskToHaveMultipleTradespeopleAssigned() {
        Task task = createTask("Multiple workers task");
        Tradesperson carpenter = createTradesperson("carpenter@example.com", "Carpenter");
        Tradesperson plumber = createTradesperson("plumber@example.com", "Plumber");

        taskAssignmentRepository.saveAndFlush(new TaskAssignment(task, carpenter));
        taskAssignmentRepository.saveAndFlush(new TaskAssignment(task, plumber));

        assertThat(taskAssignmentRepository.findByTask(task))
                .extracting(TaskAssignment::getTradesperson)
                .extracting(Tradesperson::getDisplayName)
                .containsExactlyInAnyOrder("Carpenter", "Plumber");
    }

    @Test
    void allowsOneTradespersonToBeAssignedToMultipleTasks() {
        Task firstTask = createTask("First assigned task");
        Task secondTask = createTask("Second assigned task");
        Tradesperson worker = createTradesperson("worker@example.com", "Worker");

        taskAssignmentRepository.saveAndFlush(new TaskAssignment(firstTask, worker));
        taskAssignmentRepository.saveAndFlush(new TaskAssignment(secondTask, worker));

        assertThat(taskAssignmentRepository.findByTradesperson(worker))
                .extracting(TaskAssignment::getTask)
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder("First assigned task", "Second assigned task");
    }

    @Test
    void rejectsDuplicateTaskTradespersonAssignment() {
        Task task = createTask("Duplicate assignment task");
        Tradesperson worker = createTradesperson("duplicate@example.com", "Worker");
        taskAssignmentRepository.saveAndFlush(new TaskAssignment(task, worker));

        assertThatThrownBy(() -> taskAssignmentRepository
                .saveAndFlush(new TaskAssignment(task, worker)))
                .hasMessageContaining("UNIQUE constraint failed");
    }

    @Test
    void retrievesAssignmentsThroughBothInverseMappings() {
        Task task = createTask("Inverse assignment task");
        Tradesperson worker = createTradesperson("inverse@example.com", "Worker");
        TaskAssignment assignment = taskAssignmentRepository
                .saveAndFlush(new TaskAssignment(task, worker));
        entityManager.clear();

        assertThat(taskRepository.findById(task.getId()).orElseThrow().getTaskAssignments())
                .extracting(TaskAssignment::getId)
                .containsExactly(assignment.getId());
        assertThat(tradespersonRepository.findById(worker.getId()).orElseThrow()
                .getTaskAssignments())
                .extracting(TaskAssignment::getId)
                .containsExactly(assignment.getId());
    }

    @Test
    void assignmentRemainsIndependentFromTaskTradeAndPersonTrade() {
        Task task = createTask("Independent assignment task");
        Tradesperson worker = createTradesperson("independent@example.com", "Worker");
        Trade carpentry = tradeRepository.saveAndFlush(new Trade("Carpentry"));

        TaskTrade requirement = new TaskTrade(task, carpentry);
        PersonTrade qualification = new PersonTrade(worker, carpentry);
        TaskAssignment assignment = new TaskAssignment(task, worker);

        taskAssignmentRepository.saveAndFlush(assignment);
        assertThat(taskTradeRepository.findByTask(task)).isEmpty();
        assertThat(personTradeRepository.findByTradesperson(worker)).isEmpty();
        assertThat(taskAssignmentRepository.findByTask(task))
                .extracting(TaskAssignment::getTradesperson)
                .containsExactly(worker);

        taskTradeRepository.saveAndFlush(requirement);
        personTradeRepository.saveAndFlush(qualification);
        entityManager.clear();

        assertThat(taskAssignmentRepository.findByTask(task)).hasSize(1);
        assertThat(taskRepository.findById(task.getId()).orElseThrow().getTaskTrades())
                .extracting(TaskTrade::getId)
                .containsExactly(requirement.getId());
        assertThat(tradespersonRepository.findById(worker.getId()).orElseThrow()
                .getPersonTrades())
                .extracting(PersonTrade::getId)
                .containsExactly(qualification.getId());
    }

    @Test
    void taskHasNoDirectTradespersonField() {
        assertThat(Task.class.getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("tradesperson", "tradespersonId", "worker", "workerId");
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
}