package com.rockandhardplaces.api;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;

@RestController
@RequestMapping("/api")
class FrontendSupportController {
    private final FrontendSupportService support;
    private final ActiveAccountContext account;
    private final ApiAccessService access;
    private final ProjectWorkflowService workflow;
    private final TaskProgressService progress;

    FrontendSupportController(FrontendSupportService support, ActiveAccountContext account,
            ApiAccessService access, ProjectWorkflowService workflow, TaskProgressService progress) {
        this.support = support; this.account = account; this.access = access;
        this.workflow = workflow; this.progress = progress;
    }
    @GetMapping("/catalog/trades")
    List<FrontendDtos.TradeSummary> catalog() { return support.catalog(); }
    @GetMapping("/discovery/tradespeople")
    List<FrontendDtos.PersonSummary> people(@RequestParam(required = false) Long tradeId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AvailabilityStatus availability) {
        return support.discoverPeople(tradeId, q, availability);
    }
    @GetMapping("/dashboard/homeowner")
    FrontendDtos.HomeownerDashboard homeowner() { return support.homeownerDashboard(); }
    @GetMapping("/dashboard/tradesperson")
    FrontendDtos.TradespersonDashboard tradesperson() { return support.tradespersonDashboard(); }
    @GetMapping("/opportunities")
    List<FrontendDtos.Opportunity> opportunities(@RequestParam(required = false) Long tradeId,
            @RequestParam(required = false) String jobZip) { return support.opportunities(tradeId, jobZip); }
    @GetMapping("/opportunities/{taskTradeId}")
    FrontendDtos.Opportunity opportunity(@PathVariable Long taskTradeId) { return support.opportunity(taskTradeId); }
    @GetMapping("/assignments")
    List<FrontendDtos.WorkSummary> assignments() { return support.myAssignments(); }
    @GetMapping("/tasks/{taskId}/assignments")
    List<ApiDtos.AssignmentResponse> assignments(@PathVariable Long taskId) { return support.taskAssignments(taskId); }
    @PostMapping("/tasks/{taskId}/start")
    ApiDtos.TaskResponse start(@PathVariable Long taskId) {
        if (!(account.activeProfile() instanceof Tradesperson actor))
            throw new SecurityException("The active tradesperson profile is required");
        Task task = access.task(taskId, actor);
        workflow.start(actor, task);
        return ApiDtos.TaskResponse.from(task, progress);
    }
}
