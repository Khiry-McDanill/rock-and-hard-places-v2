package com.rockandhardplaces.api;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;

@Service
@Transactional(readOnly = true)
class ApiAccessService {
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final BidRepository bids;
    private final TaskTradeRepository taskTrades;
    private final ProjectTeamRepository teams;

    ApiAccessService(ProjectRepository projects, TaskRepository tasks, BidRepository bids,
            TaskTradeRepository taskTrades, ProjectTeamRepository teams) {
        this.projects = projects;
        this.tasks = tasks;
        this.bids = bids;
        this.taskTrades = taskTrades;
        this.teams = teams;
    }

    List<Project> projectsFor(Object profile) {
        if (profile instanceof Homeowner homeowner) {
            return projects.findByHomeownerOrderByIdAsc(homeowner);
        }
        Tradesperson tradesperson = (Tradesperson) profile;
        return teams.findByTradesperson(tradesperson).stream()
                .filter(team -> team.getStatus() == ProjectTeamStatus.ACTIVE)
                .map(ProjectTeam::getProject).distinct().toList();
    }

    Project project(Long id, Object profile) {
        Project project = projects.findById(id).orElseThrow(ResourceNotFoundException::new);
        requireProjectAccess(project, profile);
        return project;
    }

    Task task(Long id, Object profile) {
        Task task = tasks.findById(id).orElseThrow(ResourceNotFoundException::new);
        requireProjectAccess(task.getProject(), profile);
        return task;
    }

    Task biddingTask(Long id) {
        return tasks.findById(id).orElseThrow(ResourceNotFoundException::new);
    }

    TaskTrade taskTrade(Long id, Task task) {
        TaskTrade trade = taskTrades.findById(id).orElseThrow(ResourceNotFoundException::new);
        if (!Objects.equals(trade.getTask().getId(), task.getId())) {
            throw new IllegalArgumentException("TaskTrade does not belong to the requested task");
        }
        return trade;
    }

    Bid bid(Long id) {
        return bids.findById(id).orElseThrow(ResourceNotFoundException::new);
    }

    List<Bid> visibleBids(Task task, Object profile) {
        if (profile instanceof Homeowner) {
            requireProjectAccess(task.getProject(), profile);
            return bids.findByTask(task);
        }
        Tradesperson tradesperson = (Tradesperson) profile;
        return bids.findByTask(task).stream()
                .filter(bid -> Objects.equals(bid.getTradesperson().getId(), tradesperson.getId()))
                .toList();
    }

    private void requireProjectAccess(Project project, Object profile) {
        boolean allowed = profile instanceof Homeowner homeowner
                ? Objects.equals(project.getHomeowner().getId(), homeowner.getId())
                : teams.findByProject(project).stream().anyMatch(team ->
                        team.getStatus() == ProjectTeamStatus.ACTIVE
                                && Objects.equals(team.getTradesperson().getId(),
                                        ((Tradesperson) profile).getId()));
        if (!allowed) {
            throw new SecurityException("The active profile cannot access this project");
        }
    }
}
