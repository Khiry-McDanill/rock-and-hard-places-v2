package com.rockandhardplaces.review;
import java.time.Instant;
import com.rockandhardplaces.account.User;
import jakarta.persistence.*;
@Entity @Table(name="moderation_reports")
public class ModerationReport {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="reporter_id",nullable=false) private User reporter;
 @Enumerated(EnumType.STRING) @Column(name="target_type",nullable=false) private ModerationTargetType targetType;
 @ManyToOne @JoinColumn(name="review_id") private Review review;
 @ManyToOne @JoinColumn(name="response_id") private ReviewResponse response;
 @Column(nullable=false,length=2000) private String reason;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private ModerationStatus status;
 protected ModerationReport(){} ModerationReport(User reporter,Review review,String reason){init(reporter,reason);this.review=review;targetType=ModerationTargetType.REVIEW;}
 ModerationReport(User reporter,ReviewResponse response,String reason){init(reporter,reason);this.response=response;targetType=ModerationTargetType.REVIEW_RESPONSE;}
 private void init(User reporter,String reason){if(reason==null||reason.isBlank())throw new IllegalArgumentException("Report reason is required");this.reporter=reporter;this.reason=reason;createdAt=Instant.now();status=ModerationStatus.OPEN;}
 public void setStatus(ModerationStatus status){this.status=status;} public boolean isActive(){return status==ModerationStatus.OPEN||status==ModerationStatus.UNDER_REVIEW;}
 public Long getId(){return id;} public User getReporter(){return reporter;} public ModerationTargetType getTargetType(){return targetType;} public Review getReview(){return review;}
 public ReviewResponse getResponse(){return response;} public String getReason(){return reason;} public Instant getCreatedAt(){return createdAt;} public ModerationStatus getStatus(){return status;}
}
