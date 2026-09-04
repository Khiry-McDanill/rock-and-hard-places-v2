package com.rockandhardplaces.project;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rockandhardplaces.catalog.Trade;

public interface TaskTradeRepository extends JpaRepository<TaskTrade, Long> {

    List<TaskTrade> findByTask(Task task);

    List<TaskTrade> findByTrade(Trade trade);
}