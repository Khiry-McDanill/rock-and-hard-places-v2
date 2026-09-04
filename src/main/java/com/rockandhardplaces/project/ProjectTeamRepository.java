package com.rockandhardplaces.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rockandhardplaces.account.Tradesperson;

public interface ProjectTeamRepository extends JpaRepository<ProjectTeam, Long> {

    List<ProjectTeam> findByProject(Project project);

    List<ProjectTeam> findByTradesperson(Tradesperson tradesperson);

    @Query("SELECT pt FROM ProjectTeam pt WHERE pt.project = :project AND pt.tradesperson = :tradesperson AND pt.status = :status")
    Optional<ProjectTeam> findActiveMembership(@Param("project") Project project,
            @Param("tradesperson") Tradesperson tradesperson,
            @Param("status") ProjectTeamStatus status);
}