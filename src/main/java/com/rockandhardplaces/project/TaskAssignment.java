package com.rockandhardplaces.project;

import com.rockandhardplaces.account.Tradesperson;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "task_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_task_assignments_task_tradesperson",
                columnNames = { "task_id", "tradesperson_id" })
})
public class TaskAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tradesperson_id", nullable = false)
    private Tradesperson tradesperson;

    protected TaskAssignment() {
    }

    public TaskAssignment(Task task, Tradesperson tradesperson) {
        this.task = task;
        this.tradesperson = tradesperson;
    }

    public Long getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public Tradesperson getTradesperson() {
        return tradesperson;
    }
}