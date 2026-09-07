package com.rockandhardplaces.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class TaskProgressServiceTests {
    private final TaskRepository repository = mock(TaskRepository.class);
    private final TaskAssignmentRepository assignments = mock(TaskAssignmentRepository.class);
    private final TaskProgressService service = new TaskProgressService(repository, assignments);
    private final Project project = new Project("Project", "Description", ProjectStatus.IN_PROGRESS,
            "90210", null);

    @Test
    void standaloneTaskPercentage() {
        task(TaskStatus.COMPLETED);
        task(TaskStatus.IN_PROGRESS);
        assertThat(service.progressPercentage(project)).isEqualTo(50);
    }

    @Test
    void subtasksReplaceParent() {
        Task parent = task(TaskStatus.IN_PROGRESS);
        child(parent, TaskStatus.COMPLETED);
        child(parent, TaskStatus.IN_PROGRESS);
        assertThat(service.progressPercentage(project)).isEqualTo(50);
    }

    @Test
    void mixedWorkRoundsToNearestWholeNumber() {
        task(TaskStatus.COMPLETED);
        Task parent = task(TaskStatus.IN_PROGRESS);
        child(parent, TaskStatus.COMPLETED);
        child(parent, TaskStatus.IN_PROGRESS);
        assertThat(service.progressPercentage(project)).isEqualTo(67);
    }

    @Test
    void topLevelTaskCounts() {
        task(TaskStatus.COMPLETED);
        Task parent = task(TaskStatus.IN_PROGRESS);
        child(parent, TaskStatus.COMPLETED);
        task(TaskStatus.CANCELLED);
        assertThat(service.completedTopLevelTasks(project)).isEqualTo(1);
        assertThat(service.totalTopLevelTasks(project)).isEqualTo(2);
    }

    @Test
    void subtaskCounts() {
        task(TaskStatus.COMPLETED);
        Task parent = task(TaskStatus.IN_PROGRESS);
        child(parent, TaskStatus.COMPLETED);
        child(parent, TaskStatus.IN_PROGRESS);
        child(parent, TaskStatus.CANCELLED);
        assertThat(service.completedSubtasks(project)).isEqualTo(1);
        assertThat(service.totalSubtasks(project)).isEqualTo(2);
    }

    @Test
    void cancelledWorkExcluded() {
        task(TaskStatus.CANCELLED);
        Task parent = task(TaskStatus.IN_PROGRESS);
        child(parent, TaskStatus.COMPLETED);
        child(parent, TaskStatus.CANCELLED);
        assertThat(service.progressPercentage(project)).isEqualTo(100);
    }

    @Test
    void cancelledParentExcludesDescendants() {
        Task parent = task(TaskStatus.CANCELLED);
        child(parent, TaskStatus.IN_PROGRESS);
        task(TaskStatus.COMPLETED);
        assertThat(service.progressPercentage(project)).isEqualTo(100);
        assertThat(service.totalSubtasks(project)).isZero();
    }

    @Test
    void readyForReviewIsNotComplete() {
        Task task = task(TaskStatus.IN_PROGRESS);
        service.submitForReview(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.READY_FOR_REVIEW);
        assertThat(service.progressPercentage(project)).isZero();
        assertThat(service.isProjectComplete(project)).isFalse();
    }

    @Test
    void homeownerApproval() {
        Task task = task(TaskStatus.READY_FOR_REVIEW);
        service.approve(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(service.progressPercentage(project)).isEqualTo(100);
    }

    @Test
    void homeownerRejection() {
        Task task = task(TaskStatus.READY_FOR_REVIEW);
        service.reject(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(service.progressPercentage(project)).isZero();
    }

    @Test
    void parentCompletesOnlyAfterAllActiveChildren() {
        Task parent = task(TaskStatus.IN_PROGRESS);
        Task first = child(parent, TaskStatus.READY_FOR_REVIEW);
        Task second = child(parent, TaskStatus.READY_FOR_REVIEW);
        child(parent, TaskStatus.CANCELLED);
        service.approve(first);
        assertThat(parent.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        service.approve(second);
        assertThat(parent.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(service.isProjectComplete(project)).isTrue();
    }

    @Test
    void allCancelledSubtasksDoNotCompleteParent() {
        Task parent = task(TaskStatus.IN_PROGRESS);
        Task child = child(parent, TaskStatus.IN_PROGRESS);
        service.cancel(child);
        assertThat(parent.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(service.progressPercentage(project)).isZero();
        assertThat(service.isProjectComplete(project)).isFalse();
        assertThatThrownBy(() -> service.completeUnassigned(parent))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reopenDecreasesProgressAndReopensParent() {
        Task parent = task(TaskStatus.COMPLETED);
        Task child = child(parent, TaskStatus.COMPLETED);
        assertThat(service.progressPercentage(project)).isEqualTo(100);
        service.reopen(child);
        assertThat(service.progressPercentage(project)).isZero();
        assertThat(parent.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(service.isProjectComplete(project)).isFalse();
    }

    @Test
    void projectCompletionRequiresCompletedTopLevelTasks() {
        Task parent = task(TaskStatus.IN_PROGRESS);
        child(parent, TaskStatus.COMPLETED);
        task(TaskStatus.COMPLETED);
        task(TaskStatus.CANCELLED);
        assertThat(service.progressPercentage(project)).isEqualTo(100);
        assertThat(service.isProjectComplete(project)).isFalse();
        service.completeUnassigned(parent);
        assertThat(service.isProjectComplete(project)).isTrue();
    }

    @Test
    void emptyAndEntirelyCancelledProjectsAreNotComplete() {
        assertThat(service.progressPercentage(project)).isZero();
        assertThat(service.isProjectComplete(project)).isFalse();
        task(TaskStatus.CANCELLED);
        assertThat(service.progressPercentage(project)).isZero();
        assertThat(service.isProjectComplete(project)).isFalse();
    }

    @Test
    void unassignedWorkCanCompleteDirectly() {
        Task task = task(TaskStatus.PLANNING);
        service.completeUnassigned(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void assignedWorkCannotBypassApproval() {
        Task task = task(TaskStatus.IN_PROGRESS);
        when(assignments.findByTask(task)).thenReturn(List.of(new TaskAssignment(task, null)));
        assertThatThrownBy(() -> service.completeUnassigned(task))
                .isInstanceOf(IllegalArgumentException.class);
        service.submitForReview(task);
        service.approve(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void invalidWorkflowTransitionsAreRejected() {
        Task task = task(TaskStatus.PLANNING);
        assertThatThrownBy(() -> service.approve(task)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.reject(task)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.submitForReview(task)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.reopen(task)).isInstanceOf(IllegalArgumentException.class);
        service.cancel(task);
        assertThatThrownBy(() -> service.completeUnassigned(task)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parentApprovalCannotBypassIncompleteSubtasks() {
        Task parent = task(TaskStatus.READY_FOR_REVIEW);
        child(parent, TaskStatus.IN_PROGRESS);
        assertThatThrownBy(() -> service.approve(parent)).isInstanceOf(IllegalArgumentException.class);
        assertThat(parent.getStatus()).isEqualTo(TaskStatus.READY_FOR_REVIEW);
    }

    @Test
    void nestedWorkUsesOnlyLeavesAndPropagatesCompletion() {
        Task parent = task(TaskStatus.IN_PROGRESS);
        Task intermediate = child(parent, TaskStatus.IN_PROGRESS);
        Task leaf = child(intermediate, TaskStatus.READY_FOR_REVIEW);
        child(parent, TaskStatus.COMPLETED);
        assertThat(service.progressPercentage(project)).isEqualTo(50);
        service.approve(leaf);
        assertThat(intermediate.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(parent.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(service.progressPercentage(project)).isEqualTo(100);
    }

    @Test
    void taskProgressForStandaloneStatuses() {
        for (TaskStatus status : TaskStatus.values()) {
            assertThat(service.progressPercentage(task(status)))
                    .isEqualTo(status == TaskStatus.COMPLETED ? 100 : 0);
        }
    }

    @Test
    void taskProgressUsesNestedLeavesAndRoundsWithoutCountingParentsOrUnrelatedWork() {
        Task parent = task(TaskStatus.COMPLETED);
        Task intermediate = child(parent, TaskStatus.COMPLETED);
        child(intermediate, TaskStatus.COMPLETED);
        child(intermediate, TaskStatus.IN_PROGRESS);
        child(parent, TaskStatus.COMPLETED);
        task(TaskStatus.IN_PROGRESS);
        assertThat(service.progressPercentage(parent)).isEqualTo(67);
        assertThat(service.progressPercentage(intermediate)).isEqualTo(50);
    }

    @Test
    void taskProgressExcludesCancelledWorkAndEntireCancelledSubtrees() {
        Task parent = task(TaskStatus.IN_PROGRESS);
        child(parent, TaskStatus.COMPLETED);
        Task cancelled = child(parent, TaskStatus.CANCELLED);
        Task descendant = child(cancelled, TaskStatus.COMPLETED);
        child(descendant, TaskStatus.IN_PROGRESS);
        assertThat(service.progressPercentage(parent)).isEqualTo(100);
        assertThat(service.progressPercentage(cancelled)).isZero();
        assertThat(service.progressPercentage(descendant)).isZero();
    }

    @Test
    void taskProgressIsZeroWhenAllDescendantsAreCancelled() {
        Task parent = task(TaskStatus.COMPLETED);
        child(parent, TaskStatus.CANCELLED);
        assertThat(service.progressPercentage(parent)).isZero();
    }

    @Test
    void reopeningChildLowersTaskAndParentProgress() {
        Task parent = task(TaskStatus.COMPLETED);
        Task child = child(parent, TaskStatus.COMPLETED);
        child(parent, TaskStatus.COMPLETED);
        assertThat(service.progressPercentage(parent)).isEqualTo(100);
        service.reopen(child);
        assertThat(service.progressPercentage(child)).isZero();
        assertThat(service.progressPercentage(parent)).isEqualTo(50);
    }

    private Task task(TaskStatus status) {
        return child(null, status);
    }

    private Task child(Task parent, TaskStatus status) {
        return new Task("Task", "Description", status, project, parent);
    }
}
