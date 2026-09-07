package com.rockandhardplaces.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;
import com.rockandhardplaces.communication.*;
import com.rockandhardplaces.review.*;
import com.rockandhardplaces.portfolio.*;
import java.time.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

final class ApiDtos {
    private ApiDtos() {
    }

    record AccountResponse(Long userId, String email, AccountRole activeRole, ProfileResponse profile,
            List<ProfileResponse> profiles) {
        static AccountResponse from(ActiveAccountContext context) {
            User user = context.currentUser();
            return new AccountResponse(user.getId(), user.getEmail(), context.activeRole(),
                    ProfileResponse.from(context.activeProfile()),
                    context.availableProfiles().stream().map(ProfileResponse::from).toList());
        }
    }

    record ProfileResponse(Long id, AccountRole role, String displayName, String profileImageReference,
            AccountStatus accountStatus, TradespersonVerificationStatus verificationStatus, String baseZip,
            Integer serviceRadius, AvailabilityStatus availabilityStatus) {
        static ProfileResponse from(Object profile) {
            if (profile instanceof Homeowner homeowner) {
                return new ProfileResponse(homeowner.getId(), AccountRole.HOMEOWNER,
                        homeowner.getDisplayName(), homeowner.getProfileImageReference(),
                        homeowner.getAccountStatus(), null, null, null, null);
            }
            Tradesperson tradesperson = (Tradesperson) profile;
            return new ProfileResponse(tradesperson.getId(), AccountRole.TRADESPERSON,
                    tradesperson.getDisplayName(), tradesperson.getProfileImageReference(),
                    tradesperson.getAccountStatus(), tradesperson.getVerificationStatus(),
                    tradesperson.getBaseZip(), tradesperson.getServiceRadius(),
                    tradesperson.getAvailabilityStatus());
        }
    }

    record SwitchRoleRequest(@NotNull AccountRole role) {
    }

    record ProjectResponse(Long id, String title, String description, ProjectStatus status, String jobZip,
            int progressPercentage) {
        static ProjectResponse from(Project project, TaskProgressService progress) {
            return new ProjectResponse(project.getId(), project.getTitle(), project.getDescription(),
                    project.getStatus(), project.getJobZip(), progress.progressPercentage(project));
        }
    }

    record ProjectRequest(@NotNull String title, @NotNull String description, @NotNull String jobZip) {}
    record TaskRequest(@NotNull String title, @NotNull String description, Long parentTaskId,
            List<Long> requiredTradeIds) {}

    record TaskResponse(Long id, Long projectId, Long parentTaskId, String title, String description,
            TaskStatus status, List<TaskTradeResponse> requiredTrades, int progressPercentage) {
        static TaskResponse from(Task task, TaskProgressService progress) {
            return new TaskResponse(task.getId(), task.getProject().getId(),
                    task.getParentTask() == null ? null : task.getParentTask().getId(), task.getTitle(),
                    task.getDescription(), task.getStatus(),
                    task.getTaskTrades().stream().map(TaskTradeResponse::from).toList(),
                    progress.progressPercentage(task));
        }
    }

    record TaskTradeResponse(Long id, Long tradeId, String tradeName) {
        static TaskTradeResponse from(TaskTrade taskTrade) {
            return new TaskTradeResponse(taskTrade.getId(), taskTrade.getTrade().getId(),
                    taskTrade.getTrade().getName());
        }
    }

    record BidRequest(@NotNull Long taskTradeId, @NotNull @Positive BigDecimal amount, String message) {
    }

    record BidResponse(Long id, Long taskId, Long taskTradeId, Long tradespersonId, BigDecimal amount,
            String message, BidStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        static BidResponse from(Bid bid) {
            return new BidResponse(bid.getId(), bid.getTask().getId(), bid.getTaskTrade().getId(),
                    bid.getTradesperson().getId(), bid.getAmount(), bid.getMessage(), bid.getStatus(),
                    bid.getCreatedAt(), bid.getUpdatedAt());
        }
    }

    record AssignmentRequest(@NotNull Long tradespersonId) {}
    record AssignmentResponse(Long id, Long taskId, Long tradespersonId, String displayName) {
        static AssignmentResponse from(TaskAssignment a) { return new AssignmentResponse(a.getId(),
                a.getTask().getId(), a.getTradesperson().getId(), a.getTradesperson().getDisplayName()); }
    }
    record TeamResponse(Long id, Long tradespersonId, String displayName, ProjectTeamStatus status,
            List<String> trades) {
        static TeamResponse from(ProjectTeam team) { return new TeamResponse(team.getId(),
                team.getTradesperson().getId(), team.getTradesperson().getDisplayName(), team.getStatus(),
                team.getProjectTeamTrades().stream().map(t -> t.getTrade().getName()).toList()); }
    }
    record ConversationResponse(Long id, Long projectId, ConversationType type, Instant createdAt) {
        static ConversationResponse from(Conversation c) { return new ConversationResponse(c.getId(),
                c.getProject().getId(), c.getType(), c.getCreatedAt()); }
    }
    record MessageRequest(@NotNull String body) {}
    record MessageResponse(Long id, Long conversationId, Long senderId, String body, Instant createdAt,
            Instant editedAt, boolean removed) {
        static MessageResponse from(Message m) { return new MessageResponse(m.getId(),
                m.getConversation().getId(), m.getSender().getId(), m.isRemoved() ? null : m.getBody(),
                m.getCreatedAt(), m.getEditedAt(), m.isRemoved()); }
    }
    record ReviewRequest(@NotNull Long tradespersonId, @NotNull Long projectId, Long taskId,
            @NotNull ReviewLevel level, @NotNull @jakarta.validation.constraints.Min(1)
            @jakarta.validation.constraints.Max(5) Integer overallRating,
            Integer qualityRating, Integer communicationRating, Integer reliabilityRating,
            Integer professionalismRating, String body) {}
    record ReviewUpdateRequest(@NotNull @jakarta.validation.constraints.Min(1)
            @jakarta.validation.constraints.Max(5) Integer overallRating,
            Integer qualityRating, Integer communicationRating, Integer reliabilityRating,
            Integer professionalismRating, String body) {}
    record ReviewResponseDto(Long id, Long homeownerId, Long tradespersonId, Long projectId,
            Long taskId, ReviewLevel level, Integer overallRating, Integer qualityRating,
            Integer communicationRating, Integer reliabilityRating, Integer professionalismRating,
            String body, boolean withdrawn) {
        static ReviewResponseDto from(Review r) { return new ReviewResponseDto(r.getId(),
                r.getHomeowner().getId(), r.getTradesperson().getId(), r.getProject().getId(),
                r.getTask() == null ? null : r.getTask().getId(), r.getLevel(), r.getOverallRating(),
                r.getQualityRating(), r.getCommunicationRating(), r.getReliabilityRating(),
                r.getProfessionalismRating(), r.getBody(), r.isWithdrawn()); }
    }
    record ReviewResponseRequest(@NotNull String body) {}
    record ReviewReplyResponse(Long id, Long reviewId, Long tradespersonId, String body) {
        static ReviewReplyResponse from(com.rockandhardplaces.review.ReviewResponse r) {
            return new ReviewReplyResponse(r.getId(), r.getReview().getId(), r.getTradesperson().getId(), r.getBody()); }
    }
    record PortfolioResponse(Long id, Long tradespersonId, String title, String description,
            PortfolioProvenance provenance, Long projectId, Long taskId, LocalDate completionDate,
            List<Long> approvedAttachmentIds) {
        static PortfolioResponse from(PortfolioItem item, List<PortfolioPublicationRequest> approvals) {
            return new PortfolioResponse(item.getId(), item.getTradesperson().getId(), item.getTitle(),
                    item.getDescription(), item.getProvenance(), item.getProject() == null ? null : item.getProject().getId(),
                    item.getTask() == null ? null : item.getTask().getId(), item.getCompletionDate(),
                    approvals.stream().map(a -> a.getAttachment().getId()).toList()); }
    }
}
