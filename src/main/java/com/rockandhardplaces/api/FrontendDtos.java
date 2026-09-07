package com.rockandhardplaces.api;

import java.util.List;
import com.rockandhardplaces.account.*;

/** Read models for the first UI; domain entities never cross the API boundary. */
final class FrontendDtos {
    private FrontendDtos() {}
    record SpecialtySummary(Long id, String name) {}
    /** Specialties are the global catalog options for this trade. */
    record TradeSummary(Long id, String name, List<SpecialtySummary> specialties) {}
    /** Top-level specialties are this person's selected specialties. */
    record PersonSummary(ApiDtos.ProfileResponse profile, List<TradeSummary> qualifications,
            List<SpecialtySummary> specialties) {}
    record Eligibility(boolean allowed, String reason) {}
    record Opportunity(ApiDtos.ProjectResponse project, ApiDtos.TaskResponse task,
            ApiDtos.TaskTradeResponse requiredTrade, String homeownerDisplayName,
            Eligibility bidding, List<ApiDtos.BidResponse> ownBids) {}
    record NeededTrade(Long taskId, String taskTitle, ApiDtos.TaskTradeResponse requiredTrade) {}
    record ProjectSummary(ApiDtos.ProjectResponse project, long totalTasks, long completedTasks,
            long totalSubtasks, long completedSubtasks, List<ApiDtos.TeamResponse> team,
            List<ApiDtos.TaskResponse> awaitingReview, List<NeededTrade> tradesNeeded, String nextAction) {}
    record HomeownerDashboard(List<ProjectSummary> projects, String nextAction) {}
    record WorkSummary(ApiDtos.AssignmentResponse assignment, ApiDtos.ProjectResponse project,
            ApiDtos.TaskResponse task) {}
    /** Own bid is retained; live project/task are null when current scope access is denied. */
    record BidSummary(ApiDtos.BidResponse bid, ApiDtos.ProjectResponse project, ApiDtos.TaskResponse task) {}
    record TradespersonDashboard(ApiDtos.ProfileResponse profile, Eligibility bidding,
            List<WorkSummary> activeWork, List<BidSummary> activeBids, long completedWorkCount,
            long completedProjectCount, List<Opportunity> opportunities) {}
}
