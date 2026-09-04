package com.rockandhardplaces.project;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rockandhardplaces.account.Tradesperson;

public interface ProjectTeamRepository extends JpaRepository<ProjectTeam, Long> {

    List<ProjectTeam> findByProject(Project project);

    List<ProjectTeam> findByTradesperson(Tradesperson tradesperson);
}