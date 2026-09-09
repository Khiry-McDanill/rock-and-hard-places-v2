package com.rockandhardplaces.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rockandhardplaces.account.Tradesperson;

public interface BidRepository extends JpaRepository<Bid, Long> {

    boolean existsByTaskTradeAndTradesperson(TaskTrade taskTrade, Tradesperson tradesperson);

    List<Bid> findByTask(Task task);

    List<Bid> findByTradesperson(Tradesperson tradesperson);

    List<Bid> findByTaskTrade(TaskTrade taskTrade);

    List<Bid> findByTaskTradeAndStatus(TaskTrade taskTrade, BidStatus status);

    Optional<Bid> findByTaskTradeAndStatusAndIdNot(TaskTrade taskTrade, BidStatus status, Long id);
}