package com.rockandhardplaces.project;

import com.rockandhardplaces.account.Homeowner;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @NotBlank
    @Column(name = "job_zip", nullable = false)
    private String jobZip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "homeowner_id", nullable = false)
    private Homeowner homeowner;

    @OneToMany(mappedBy = "project")
    private List<Task> tasks = new ArrayList<>();

    protected Project() {
    }

    public Project(String title, String description, ProjectStatus status,
            String jobZip, Homeowner homeowner) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.jobZip = jobZip;
        this.homeowner = homeowner;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public String getJobZip() {
        return jobZip;
    }

    public Homeowner getHomeowner() {
        return homeowner;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    void addTask(Task task) {
        tasks.add(task);
    }
}