package com.rockandhardplaces.project;

import com.rockandhardplaces.account.Tradesperson;

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
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_teams", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_teams_project_tradesperson",
                columnNames = { "project_id", "tradesperson_id" })
})
public class ProjectTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tradesperson_id", nullable = false)
    private Tradesperson tradesperson;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(nullable = false)
    private ProjectTeamStatus status;

    @OneToMany(mappedBy = "projectTeam")
    private List<ProjectTeamTrade> projectTeamTrades = new ArrayList<>();

    protected ProjectTeam() {
    }

    public ProjectTeam(Project project, Tradesperson tradesperson, ProjectTeamStatus status) {
        this.project = project;
        this.tradesperson = tradesperson;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public Tradesperson getTradesperson() {
        return tradesperson;
    }

    public ProjectTeamStatus getStatus() {
        return status;
    }

    public List<ProjectTeamTrade> getProjectTeamTrades() {
        return projectTeamTrades;
    }
}