package com.rockandhardplaces.api;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.rockandhardplaces.account.ActiveAccountContext;
import com.rockandhardplaces.account.Homeowner;
import com.rockandhardplaces.project.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects")
class ProjectController {
    private final ActiveAccountContext accountContext;
    private final ApiAccessService access;
    private final ProjectWorkflowService workflow;
    private final TaskProgressService progress;
    private final ProjectTeamRepository teams;

    ProjectController(ActiveAccountContext accountContext, ApiAccessService access,
            ProjectWorkflowService workflow, TaskProgressService progress, ProjectTeamRepository teams) {
        this.accountContext = accountContext;
        this.access = access;
        this.workflow = workflow; this.progress = progress; this.teams = teams;
    }

    @GetMapping
    List<ApiDtos.ProjectResponse> projects() {
        return access.projectsFor(accountContext.activeProfile()).stream()
                .map(project -> ApiDtos.ProjectResponse.from(project, progress)).toList();
    }

    @GetMapping("/{projectId}")
    ApiDtos.ProjectResponse project(@PathVariable Long projectId) {
        return ApiDtos.ProjectResponse.from(access.project(projectId, accountContext.activeProfile()), progress);
    }

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    ApiDtos.ProjectResponse create(@Valid @RequestBody ApiDtos.ProjectRequest request) {
        return ApiDtos.ProjectResponse.from(workflow.create(activeHomeowner(), request.title(),
                request.description(), request.jobZip()), progress);
    }

    @PatchMapping("/{projectId}")
    ApiDtos.ProjectResponse update(@PathVariable Long projectId,
            @Valid @RequestBody ApiDtos.ProjectRequest request) {
        Project project = access.project(projectId, accountContext.activeProfile());
        return ApiDtos.ProjectResponse.from(workflow.update(activeHomeowner(), project, request.title(),
                request.description(), request.jobZip()), progress);
    }

    @GetMapping("/{projectId}/tasks")
    List<ApiDtos.TaskResponse> tasks(@PathVariable Long projectId) {
        var project = access.project(projectId, accountContext.activeProfile());
        return project.getTasks().stream().map(ApiDtos.TaskResponse::from).toList();
    }

    @PostMapping("/{projectId}/tasks")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    ApiDtos.TaskResponse createTask(@PathVariable Long projectId,
            @Valid @RequestBody ApiDtos.TaskRequest request) {
        Project project = access.project(projectId, accountContext.activeProfile());
        Task parent = request.parentTaskId() == null ? null
                : access.task(request.parentTaskId(), accountContext.activeProfile());
        return ApiDtos.TaskResponse.from(workflow.createTask(activeHomeowner(), project,
                request.title(), request.description(), parent, request.requiredTradeIds()));
    }

    @GetMapping("/{projectId}/team")
    List<ApiDtos.TeamResponse> team(@PathVariable Long projectId) {
        Project project = access.project(projectId, accountContext.activeProfile());
        return teams.findByProject(project).stream().map(ApiDtos.TeamResponse::from).toList();
    }

    private Homeowner activeHomeowner() {
        if (accountContext.activeProfile() instanceof Homeowner homeowner) return homeowner;
        throw new SecurityException("The active homeowner profile is required");
    }
}
