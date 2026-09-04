package com.rockandhardplaces.project;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import com.rockandhardplaces.account.Tradesperson;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_trade_id", nullable = false)
    private TaskTrade taskTrade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tradesperson_id", nullable = false)
    private Tradesperson tradesperson;

    @Positive
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Bid() {
    }

    public Bid(Task task, TaskTrade taskTrade, Tradesperson tradesperson, BigDecimal amount, String message) {
        if (!Objects.equals(task.getId(), taskTrade.getTask().getId())) {
            throw new IllegalArgumentException("TaskTrade must belong to the bid task");
        }
        this.task = task;
        this.taskTrade = taskTrade;
        this.tradesperson = tradesperson;
        this.amount = amount;
        this.message = message;
        this.status = BidStatus.SUBMITTED;
    }

    public Long getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public TaskTrade getTaskTrade() {
        return taskTrade;
    }

    public Tradesperson getTradesperson() {
        return tradesperson;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMessage() {
        return message;
    }

    public BidStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void withdraw() {
        if (status != BidStatus.SUBMITTED) {
            throw new IllegalStateException("Only a submitted bid can be withdrawn");
        }
        status = BidStatus.WITHDRAWN;
    }

    public void accept() {
        if (status != BidStatus.SUBMITTED) {
            throw new IllegalStateException("Only a submitted bid can be accepted");
        }
        status = BidStatus.ACCEPTED;
    }

    public void reject() {
        if (status != BidStatus.SUBMITTED) {
            throw new IllegalStateException("Only a submitted bid can be rejected");
        }
        status = BidStatus.REJECTED;
    }
}