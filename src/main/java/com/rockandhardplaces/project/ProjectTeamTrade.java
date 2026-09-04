package com.rockandhardplaces.project;

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
@Table(name = "project_team_trades", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_team_trades_team_trade",
                columnNames = { "project_team_id", "trade_id" })
})
public class ProjectTeamTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_team_id", nullable = false)
    private ProjectTeam projectTeam;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    protected ProjectTeamTrade() {
    }

    public ProjectTeamTrade(ProjectTeam projectTeam, Trade trade) {
        if (projectTeam.getStatus() != ProjectTeamStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "ProjectTeamTrade requires an ACTIVE project team membership");
        }
        this.projectTeam = projectTeam;
        this.trade = trade;
    }

    public Long getId() {
        return id;
    }

    public ProjectTeam getProjectTeam() {
        return projectTeam;
    }

    public Trade getTrade() {
        return trade;
    }
}