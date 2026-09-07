package com.rockandhardplaces.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
class TaskController {
    private final ActiveAccountContext accountContext;
    private final ApiAccessService access;
    private final BidSubmissionService bidSubmission;
    private final ProjectWorkflowService workflow;
    private final TaskAssignmentService assignmentService;
    private final TradespersonRepository tradespeople;
    private final TaskProgressService progress;

    TaskController(ActiveAccountContext accountContext, ApiAccessService access,
            BidSubmissionService bidSubmission, ProjectWorkflowService workflow,
            TaskAssignmentService assignmentService, TradespersonRepository tradespeople,
            TaskProgressService progress) {
        this.progress = progress;
        this.accountContext = accountContext;
        this.access = access;
        this.bidSubmission = bidSubmission;
        this.workflow = workflow; this.assignmentService = assignmentService; this.tradespeople = tradespeople;
    }

    @GetMapping("/{taskId}")
    ApiDtos.TaskResponse task(@PathVariable Long taskId) {
        return ApiDtos.TaskResponse.from(access.task(taskId, accountContext.activeProfile()), progress);
    }

    @GetMapping("/{taskId}/bids")
    List<ApiDtos.BidResponse> bids(@PathVariable Long taskId) {
        Object profile = accountContext.activeProfile();
        Task task = profile instanceof Tradesperson ? access.biddingTask(taskId) : access.task(taskId, profile);
        return access.visibleBids(task, profile).stream().map(ApiDtos.BidResponse::from).toList();
    }

    @PostMapping("/{taskId}/bids")
    @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.BidResponse submitBid(@PathVariable Long taskId,
            @Valid @RequestBody ApiDtos.BidRequest request) {
        Object profile = accountContext.activeProfile();
        if (!(profile instanceof Tradesperson tradesperson)) {
            throw new SecurityException("The active tradesperson profile is required");
        }
        Task task = access.biddingTask(taskId);
        TaskTrade taskTrade = access.taskTrade(request.taskTradeId(), task);
        return ApiDtos.BidResponse.from(
                bidSubmission.submit(task, taskTrade, tradesperson, request.amount(), request.message()));
    }

    @PostMapping("/{taskId}/ready-for-review")
    ApiDtos.TaskResponse ready(@PathVariable Long taskId) {
        Tradesperson actor = activeTradesperson(); Task task = access.biddingTask(taskId);
        workflow.readyForReview(actor, task); return ApiDtos.TaskResponse.from(task, progress);
    }

    @PostMapping("/{taskId}/approve")
    ApiDtos.TaskResponse approve(@PathVariable Long taskId) {
        Homeowner actor = activeHomeowner(); Task task = access.task(taskId, actor);
        workflow.approve(actor, task); return ApiDtos.TaskResponse.from(task, progress);
    }

    @PostMapping("/{taskId}/reject")
    ApiDtos.TaskResponse reject(@PathVariable Long taskId) {
        Homeowner actor = activeHomeowner(); Task task = access.task(taskId, actor);
        workflow.reject(actor, task); return ApiDtos.TaskResponse.from(task, progress);
    }

    @PostMapping("/{taskId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.AssignmentResponse assign(@PathVariable Long taskId,
            @Valid @RequestBody ApiDtos.AssignmentRequest request) {
        Homeowner actor = activeHomeowner(); Task task = access.task(taskId, actor);
        Tradesperson worker = tradespeople.findById(request.tradespersonId())
                .orElseThrow(ResourceNotFoundException::new);
        return ApiDtos.AssignmentResponse.from(assignmentService.createEligibleAssignment(task, worker));
    }

    private Homeowner activeHomeowner() {
        if (accountContext.activeProfile() instanceof Homeowner homeowner) return homeowner;
        throw new SecurityException("The active homeowner profile is required");
    }
    private Tradesperson activeTradesperson() {
        if (accountContext.activeProfile() instanceof Tradesperson tradesperson) return tradesperson;
        throw new SecurityException("The active tradesperson profile is required");
    }
}
