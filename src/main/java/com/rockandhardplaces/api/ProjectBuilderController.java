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

    ProjectBuilderController(ActiveAccountContext account, ProjectPlanningService planning) {
        this.account = account; this.planning = planning;
    }

    @PostMapping("/plan")
    ValidatedProjectPlan plan(@Valid @RequestBody PlanRequest request) {
        if (!(account.activeProfile() instanceof Homeowner homeowner))
            throw new SecurityException("The active homeowner profile is required");
        return planning.plan(homeowner, request.idea());
    }

    record PlanRequest(@NotBlank @Size(max = 8000) String idea) {}
}
