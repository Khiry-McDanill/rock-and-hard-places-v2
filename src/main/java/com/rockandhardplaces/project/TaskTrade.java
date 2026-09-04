package com.rockandhardplaces.project;

import com.rockandhardplaces.catalog.Trade;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "task_trades", uniqueConstraints = {
        @UniqueConstraint(name = "uk_task_trades_task_trade", columnNames = { "task_id", "trade_id" })
})
public class TaskTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    protected TaskTrade() {
    }

    public TaskTrade(Task task, Trade trade) {
        this.task = task;
        this.trade = trade;
    }

    public Long getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public Trade getTrade() {
        return trade;
    }
}