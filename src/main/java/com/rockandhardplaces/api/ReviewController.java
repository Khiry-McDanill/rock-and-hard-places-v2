package com.rockandhardplaces.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;
import com.rockandhardplaces.review.*;

@RestController
@RequestMapping("/api/reviews")
class ReviewController {
    private final ActiveAccountContext account;
    private final ApiAccessService access;
    private final TradespersonRepository tradespeople;
    private final ReviewRepository reviews;
    private final ReviewService service;

    ReviewController(ActiveAccountContext account, ApiAccessService access,
            TradespersonRepository tradespeople, ReviewRepository reviews, ReviewService service) {
        this.account = account; this.access = access; this.tradespeople = tradespeople;
        this.reviews = reviews; this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.ReviewResponseDto create(@Valid @RequestBody ApiDtos.ReviewRequest request) {
        Homeowner actor = homeowner();
        Tradesperson subject = tradespeople.findById(request.tradespersonId())
                .orElseThrow(ResourceNotFoundException::new);
        Project project = access.project(request.projectId(), actor);
        Review review;
        if (request.level() == ReviewLevel.TASK) {
            if (request.taskId() == null) throw new IllegalArgumentException("Task review requires taskId");
            Task task = access.task(request.taskId(), actor);
            if (!task.getProject().getId().equals(project.getId()))
                throw new IllegalArgumentException("Task must belong to review project");
            review = service.createTaskReview(actor, subject, task, request.overallRating(),
                    request.qualityRating(), request.communicationRating(), request.reliabilityRating(),
                    request.professionalismRating(), request.body());
        } else {
            if (request.taskId() != null) throw new IllegalArgumentException("Project review cannot include taskId");
            review = service.createProjectReview(actor, subject, project, request.overallRating(),
                    request.qualityRating(), request.communicationRating(), request.reliabilityRating(),
                    request.professionalismRating(), request.body());
        }
        return ApiDtos.ReviewResponseDto.from(review);
    }

    @PatchMapping("/{reviewId}")
    ApiDtos.ReviewResponseDto edit(@PathVariable Long reviewId,
            @Valid @RequestBody ApiDtos.ReviewUpdateRequest request) {
        Review review = review(reviewId);
        return ApiDtos.ReviewResponseDto.from(service.edit(homeowner(), review, request.overallRating(),
                request.qualityRating(), request.communicationRating(), request.reliabilityRating(),
                request.professionalismRating(), request.body()));
    }

    @PostMapping("/{reviewId}/withdraw")
    ApiDtos.ReviewResponseDto withdraw(@PathVariable Long reviewId) {
        return ApiDtos.ReviewResponseDto.from(service.withdraw(homeowner(), review(reviewId)));
    }

    @PostMapping("/{reviewId}/response")
    @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.ReviewReplyResponse respond(@PathVariable Long reviewId,
            @Valid @RequestBody ApiDtos.ReviewResponseRequest request) {
        return ApiDtos.ReviewReplyResponse.from(service.respond(tradesperson(), review(reviewId), request.body()));
    }

    private Review review(Long id) { return reviews.findById(id).orElseThrow(ResourceNotFoundException::new); }
    private Homeowner homeowner() { if (account.activeProfile() instanceof Homeowner h) return h;
        throw new SecurityException("The active homeowner profile is required"); }
    private Tradesperson tradesperson() { if (account.activeProfile() instanceof Tradesperson t) return t;
        throw new SecurityException("The active tradesperson profile is required"); }
}
