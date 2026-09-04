package com.rockandhardplaces.project;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskProgressService {

    private final TaskRepository tasks;
    private final TaskAssignmentRepository assignments;

    public TaskProgressService(TaskRepository tasks, TaskAssignmentRepository assignments) {
        this.tasks = tasks;
        this.assignments = assignments;
    }

    public int progressPercentage(Project project) {
        List<Task> units = project.getTasks().stream()
                .filter(this::isIncluded)
                .filter(task -> children(task).isEmpty()).toList();
        return units.isEmpty() ? 0 : (int) Math.round(100.0 * completed(units) / units.size());
    }

    public long completedTopLevelTasks(Project project) {
        return completed(topLevelTasks(project));
    }

    public long totalTopLevelTasks(Project project) {
        return topLevelTasks(project).size();
    }

    public long completedSubtasks(Project project) {
        return completed(subtasks(project));
    }

    public long totalSubtasks(Project project) {
        return subtasks(project).size();
    }

    public boolean isProjectComplete(Project project) {
        List<Task> topLevel = topLevelTasks(project);
        return !topLevel.isEmpty() && completed(topLevel) == topLevel.size();
    }

    @Transactional
    public void submitForReview(Task task) {
        requireStatus(task, TaskStatus.IN_PROGRESS);
        changeStatus(task, TaskStatus.READY_FOR_REVIEW);
    }

    @Transactional
    public void approve(Task task) {
        requireStatus(task, TaskStatus.READY_FOR_REVIEW);
        requireCompleteChildren(task);
        changeStatus(task, TaskStatus.COMPLETED);
    }

    @Transactional
    public void reject(Task task) {
        requireStatus(task, TaskStatus.READY_FOR_REVIEW);
        changeStatus(task, TaskStatus.IN_PROGRESS);
    }

    @Transactional
    public void completeUnassigned(Task task) {
        if (!assignments.findByTask(task).isEmpty()) {
            throw new IllegalArgumentException("Assigned work requires homeowner approval");
        }
        if (task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled work cannot be completed");
        }
        requireCompleteChildren(task);
        changeStatus(task, TaskStatus.COMPLETED);
    }

    @Transactional
    public void reopen(Task task) {
        requireStatus(task, TaskStatus.COMPLETED);
        changeStatus(task, TaskStatus.IN_PROGRESS);
    }

    @Transactional
    public void cancel(Task task) {
        changeStatus(task, TaskStatus.CANCELLED);
    }

    private void changeStatus(Task task, TaskStatus status) {
        task.setStatus(status);
        tasks.save(task);
        for (Task parent = task.getParentTask(); parent != null; parent = parent.getParentTask()) {
            if (parent.getStatus() == TaskStatus.CANCELLED) {
                continue;
            }
            if (allChildrenComplete(parent)) {
                parent.setStatus(TaskStatus.COMPLETED);
                tasks.save(parent);
            } else if (parent.getStatus() == TaskStatus.COMPLETED) {
                parent.setStatus(TaskStatus.IN_PROGRESS);
                tasks.save(parent);
            }
        }
    }

    private void requireCompleteChildren(Task task) {
        if (!children(task).isEmpty() && !allChildrenComplete(task)) {
            throw new IllegalArgumentException("All non-cancelled subtasks must be completed");
        }
    }

    private boolean allChildrenComplete(Task task) {
        List<Task> active = children(task).stream()
                .filter(child -> child.getStatus() != TaskStatus.CANCELLED).toList();
        return !active.isEmpty() && completed(active) == active.size();
    }

    private void requireStatus(Task task, TaskStatus expected) {
        if (task.getStatus() != expected) {
            throw new IllegalArgumentException("Task must be " + expected);
        }
    }

    private List<Task> children(Task task) {
        return task.getProject().getTasks().stream()
                .filter(candidate -> sameTask(candidate.getParentTask(), task)).toList();
    }

    private boolean sameTask(Task left, Task right) {
        return left == right || (left != null && left.getId() != null
                && left.getId().equals(right.getId()));
    }

    private boolean isIncluded(Task task) {
        for (Task current = task; current != null; current = current.getParentTask()) {
            if (current.getStatus() == TaskStatus.CANCELLED) {
                return false;
            }
        }
        return true;
    }

    private List<Task> topLevelTasks(Project project) {
        return project.getTasks().stream().filter(this::isIncluded)
                .filter(task -> task.getParentTask() == null).toList();
    }

    private List<Task> subtasks(Project project) {
        return project.getTasks().stream().filter(this::isIncluded)
                .filter(task -> task.getParentTask() != null).toList();
    }

    private long completed(List<Task> work) {
        return work.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
    }
}
