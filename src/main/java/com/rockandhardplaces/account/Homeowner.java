package com.rockandhardplaces.account;

import java.util.ArrayList;
import java.util.List;

import com.rockandhardplaces.project.Project;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "homeowners")
public class Homeowner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotBlank
    private String displayName;

    @OneToMany(mappedBy = "homeowner")
    private List<Project> projects = new ArrayList<>();

    protected Homeowner() {
    }

    public Homeowner(User user, String displayName) {
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

    public List<Project> getProjects() {
        return projects;
    }
}