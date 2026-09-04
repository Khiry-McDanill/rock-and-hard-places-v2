package com.rockandhardplaces.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "specialties", uniqueConstraints = {
        @UniqueConstraint(name = "uk_specialties_trade_name", columnNames = { "trade_id", "name" })
})
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    protected Specialty() {
    }

    public Specialty(String name, Trade trade) {
        this.name = name;
        this.trade = trade;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Trade getTrade() {
        return trade;
    }
}