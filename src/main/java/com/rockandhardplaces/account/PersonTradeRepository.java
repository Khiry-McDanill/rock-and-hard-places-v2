package com.rockandhardplaces.account;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rockandhardplaces.catalog.Trade;

public interface PersonTradeRepository extends JpaRepository<PersonTrade, Long> {

    List<PersonTrade> findByTradesperson(Tradesperson tradesperson);

    List<PersonTrade> findByTrade(Trade trade);
}