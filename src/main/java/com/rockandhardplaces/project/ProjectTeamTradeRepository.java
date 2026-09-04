package com.rockandhardplaces.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rockandhardplaces.catalog.Trade;

public interface ProjectTeamTradeRepository extends JpaRepository<ProjectTeamTrade, Long> {

    List<ProjectTeamTrade> findByProjectTeam(ProjectTeam projectTeam);

    List<ProjectTeamTrade> findByTrade(Trade trade);

    Optional<ProjectTeamTrade> findByProjectTeamAndTrade(ProjectTeam projectTeam, Trade trade);
}