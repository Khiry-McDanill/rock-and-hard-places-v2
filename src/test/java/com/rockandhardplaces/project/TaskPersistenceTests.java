package com.rockandhardplaces.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.rockandhardplaces.account.Homeowner;
import com.rockandhardplaces.account.HomeownerRepository;
import com.rockandhardplaces.account.User;
import com.rockandhardplaces.account.UserRepository;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskPersistenceTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HomeownerRepository homeownerRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void allowsOneProjectToContainMultipleTasks() {
        Project project = createProject();

        taskRepository.saveAndFlush(new Task(
                "Remove cabinets", "Remove the existing kitchen cabinets",
                TaskStatus.PLANNING, project, null));
        taskRepository.saveAndFlush(new Task(
                "Install cabinets", "Install the replacement kitchen cabinets",
                TaskStatus.IN_PROGRESS, project, null));

        assertThat(taskRepository.findAll())
                .extracting(Task::getProject)
                .allMatch(taskProject -> taskProject.getId().equals(project.getId()));
        assertThat(taskRepository.count()).isEqualTo(2);
    }

    @Test
    void persistsTaskWithoutParentOrAssignment() {
        Project project = createProject();

        Task savedTask = taskRepository.saveAndFlush(new Task(
                "Measure kitchen", "Record the kitchen dimensions",
                TaskStatus.PLANNING, project, null));

        assertThat(taskRepository.findById(savedTask.getId()))
                .get()
                .satisfies(task -> {
                    assertThat(task.getParentTask()).isNull();
                    assertThat(task.getProject().getId()).isEqualTo(project.getId());
                });
    }

    @Test
    void persistsParentAndSubtaskRelationship() {
        Project project = createProject();
        Task parentTask = taskRepository.saveAndFlush(new Task(
                "Prepare kitchen", "Complete the kitchen preparation work",
                TaskStatus.IN_PROGRESS, project, null));
        Task subtask = taskRepository.saveAndFlush(new Task(
                "Protect flooring", "Cover the flooring before demolition",
                TaskStatus.COMPLETED, project, parentTask));

        assertThat(taskRepository.findById(subtask.getId()))
                .get()
                .extracting(Task::getParentTask)
                .extracting(Task::getId)
                .isEqualTo(parentTask.getId());
        assertThat(projectRepository.findById(project.getId()))
                .get()
                .extracting(Project::getTasks)
                .asList()
                .hasSize(2);
    }

    @Test
    void persistsControlledTaskStatus() {
        Project project = createProject();

        Task savedTask = taskRepository.saveAndFlush(new Task(
                "Complete installation", "Finish the cabinet installation",
                TaskStatus.COMPLETED, project, null));

        assertThat(taskRepository.findById(savedTask.getId()))
                .get()
                .extracting(Task::getStatus)
                .isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void taskHasNoDirectWorkerTradeOrBidFields() {
        assertThat(Task.class.getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("tradesperson", "tradespersonId", "worker", "workerId",
                        "trade", "tradeId", "bid", "bidId", "bids");
    }

    private Project createProject() {
        User user = userRepository.saveAndFlush(new User("owner@example.com"));
        Homeowner homeowner = homeownerRepository.saveAndFlush(new Homeowner(user, "Project owner"));
        return projectRepository.saveAndFlush(new Project(
                "Kitchen remodel", "Update the kitchen", ProjectStatus.PLANNING,
                "90210", homeowner));
    }
}