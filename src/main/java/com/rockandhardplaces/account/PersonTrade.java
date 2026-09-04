package com.rockandhardplaces.account;

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
@Table(name = "person_trades", uniqueConstraints = {
        @UniqueConstraint(name = "uk_person_trades_person_trade", columnNames = { "tradesperson_id", "trade_id" })
})
public class PersonTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tradesperson_id", nullable = false)
    private Tradesperson tradesperson;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    protected PersonTrade() {
    }

    public PersonTrade(Tradesperson tradesperson, Trade trade) {
        this.tradesperson = tradesperson;
        this.trade = trade;
    }

    public Long getId() {
        return id;
    }

    public Tradesperson getTradesperson() {
        return tradesperson;
    }

    public Trade getTrade() {
        return trade;
    }
}