package com.rockandhardplaces.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    @OneToMany(mappedBy = "task")
    private List<TaskTrade> taskTrades = new ArrayList<>();

    @OneToMany(mappedBy = "task")
    private List<TaskAssignment> taskAssignments = new ArrayList<>();

    @OneToMany(mappedBy = "task")
    private List<Bid> bids = new ArrayList<>();

    protected Task() {
    }

    public Task(String title, String description, TaskStatus status,
            Project project, Task parentTask) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.project = project;
        this.parentTask = parentTask;
        project.addTask(this);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Project getProject() {
        return project;
    }

    public Task getParentTask() {
        return parentTask;
    }

    public List<TaskTrade> getTaskTrades() {
        return taskTrades;
    }

    public List<TaskAssignment> getTaskAssignments() {
        return taskAssignments;
    }

    public List<Bid> getBids() {
        return bids;
    }
}