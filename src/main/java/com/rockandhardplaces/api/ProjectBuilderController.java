package com.rockandhardplaces.api;

import com.rockandhardplaces.account.ActiveAccountContext;
import com.rockandhardplaces.account.Homeowner;
import com.rockandhardplaces.planning.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/project-builder")
class ProjectBuilderController {
    private final ActiveAccountContext account;
    private final ProjectPlanningService planning;
    private final ApprovedProjectCreationService creation;
    private final ApiAccessService access;
    private final com.rockandhardplaces.project.TaskProgressService progress;

    ProjectBuilderController(ActiveAccountContext account, ProjectPlanningService planning,
            ApprovedProjectCreationService creation, ApiAccessService access,
            com.rockandhardplaces.project.TaskProgressService progress) {
        this.account = account; this.planning = planning;
        this.creation = creation; this.access = access; this.progress = progress;
    }

    @PostMapping("/create")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    ApiDtos.ProjectResponse create(@RequestHeader("Idempotency-Key") String submissionKey,
            @Valid @RequestBody ApprovedProjectDraft draft) {
        if (!(account.activeProfile() instanceof Homeowner homeowner))
            throw new SecurityException("The active homeowner profile is required");
        long id = creation.create(homeowner, submissionKey, draft);
        return ApiDtos.ProjectResponse.from(access.project(id, homeowner), progress);
    }

    @PostMapping("/plan")
    ValidatedProjectPlan plan(@Valid @RequestBody PlanRequest request) {
        if (!(account.activeProfile() instanceof Homeowner homeowner))
            throw new SecurityException("The active homeowner profile is required");
        return planning.plan(homeowner, request.idea());
    }

    record PlanRequest(@NotBlank @Size(max = 8000) String idea) {}
}
