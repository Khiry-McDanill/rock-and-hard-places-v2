package com.rockandhardplaces.catalog;

import java.util.ArrayList;
import java.util.List;

import com.rockandhardplaces.account.PersonTrade;
import com.rockandhardplaces.project.TaskTrade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "trades", uniqueConstraints = {
        @UniqueConstraint(name = "uk_trades_name", columnNames = "name")
})
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "trade")
    private List<Specialty> specialties = new ArrayList<>();

    @OneToMany(mappedBy = "trade")
    private List<PersonTrade> personTrades = new ArrayList<>();

    @OneToMany(mappedBy = "trade")
    private List<TaskTrade> taskTrades = new ArrayList<>();

    protected Trade() {
    }

    public Trade(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Specialty> getSpecialties() {
        return specialties;
    }

    public List<PersonTrade> getPersonTrades() {
        return personTrades;
    }

    public List<TaskTrade> getTaskTrades() {
        return taskTrades;
    }
}