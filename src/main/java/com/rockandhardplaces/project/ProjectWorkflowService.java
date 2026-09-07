package com.rockandhardplaces.project;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.catalog.TradeRepository;
import java.util.List;

@Service
@Transactional
public class ProjectWorkflowService {
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final TaskAssignmentRepository assignments;
    private final TaskProgressService progress;
    private final AccountAuthorizationService authorization;
    private final TaskTradeRepository taskTrades;
    private final TradeRepository trades;

    public ProjectWorkflowService(ProjectRepository projects, TaskRepository tasks,
            TaskAssignmentRepository assignments, TaskProgressService progress,
            AccountAuthorizationService authorization, TaskTradeRepository taskTrades,
            TradeRepository trades) {
        this.projects = projects; this.tasks = tasks; this.assignments = assignments;
        this.progress = progress; this.authorization = authorization;
        this.taskTrades = taskTrades; this.trades = trades;
    }

    public Project create(Homeowner actor, String title, String description, String jobZip) {
        authorization.requireActive(actor);
        return projects.saveAndFlush(new Project(title, description, ProjectStatus.PLANNING, jobZip, actor));
    }

    public Project update(Homeowner actor, Project project, String title, String description, String jobZip) {
        requireOwner(actor, project); project.updateDetails(title, description, jobZip);
        return projects.saveAndFlush(project);
    }

    public Task createTask(Homeowner actor, Project project, String title, String description, Task parent,
            List<Long> requiredTradeIds) {
        requireOwner(actor, project);
        if (parent != null && !Objects.equals(parent.getProject().getId(), project.getId()))
            throw new IllegalArgumentException("Parent task must belong to the project");
        Task task = tasks.saveAndFlush(new Task(title, description, TaskStatus.PLANNING, project, parent));
        if (requiredTradeIds != null) requiredTradeIds.stream().distinct().forEach(id ->
                taskTrades.saveAndFlush(new TaskTrade(task, trades.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Required trade does not exist")))));
        return task;
    }

    public void readyForReview(Tradesperson actor, Task task) {
        authorization.requireActive(actor);
        if (assignments.findByTaskAndTradesperson(task, actor).isEmpty())
            throw new SecurityException("Only an assigned tradesperson may submit this task for review");
        progress.submitForReview(task);
    }

    public void approve(Homeowner actor, Task task) { requireOwner(actor, task.getProject()); progress.approve(task); }
    public void reject(Homeowner actor, Task task) { requireOwner(actor, task.getProject()); progress.reject(task); }

    private void requireOwner(Homeowner actor, Project project) {
        authorization.requireActive(actor);
        if (actor != project.getHomeowner() && (actor.getId() == null
                || !Objects.equals(actor.getId(), project.getHomeowner().getId())))
            throw new SecurityException("Only the project homeowner may change this resource");
    }
}
