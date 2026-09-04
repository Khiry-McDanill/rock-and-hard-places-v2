package com.rockandhardplaces.account;

import java.util.ArrayList;
import java.util.List;

import com.rockandhardplaces.project.TaskAssignment;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "tradespeople")
public class Tradesperson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotBlank
    private String displayName;

    @OneToMany(mappedBy = "tradesperson")
    private List<PersonTrade> personTrades = new ArrayList<>();

    @OneToMany(mappedBy = "tradesperson")
    private List<TaskAssignment> taskAssignments = new ArrayList<>();

    protected Tradesperson() {
    }

    public Tradesperson(User user, String displayName) {
        this.user = user;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<PersonTrade> getPersonTrades() {
        return personTrades;
    }

    public List<TaskAssignment> getTaskAssignments() {
        return taskAssignments;
    }
}