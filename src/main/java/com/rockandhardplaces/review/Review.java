package com.rockandhardplaces.review;

import java.time.Instant;
import com.rockandhardplaces.account.Homeowner;
import com.rockandhardplaces.account.Tradesperson;
import com.rockandhardplaces.project.Project;
import com.rockandhardplaces.project.Task;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name="reviews", uniqueConstraints={
 @UniqueConstraint(name="uk_reviews_task_context",columnNames={"homeowner_id","tradesperson_id","task_id"})})
public class Review {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="homeowner_id",nullable=false) private Homeowner homeowner;
 @ManyToOne(optional=false) @JoinColumn(name="tradesperson_id",nullable=false) private Tradesperson tradesperson;
 @ManyToOne(optional=false) @JoinColumn(name="project_id",nullable=false) private Project project;
 @ManyToOne @JoinColumn(name="task_id") private Task task;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private ReviewLevel level;
 @NotNull @Min(1) @Max(5) @Column(name="overall_rating",nullable=false) private Integer overallRating;
 @Min(1) @Max(5) @Column(name="quality_rating") private Integer qualityRating;
 @Min(1) @Max(5) @Column(name="communication_rating") private Integer communicationRating;
 @Min(1) @Max(5) @Column(name="reliability_rating") private Integer reliabilityRating;
 @Min(1) @Max(5) @Column(name="professionalism_rating") private Integer professionalismRating;
 @Column(length=10000) private String body;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="edited_at") private Instant editedAt;
 @Column(name="withdrawn_at") private Instant withdrawnAt;
 @Column(name="moderation_hidden_at") private Instant moderationHiddenAt;
 protected Review() {}
 Review(Homeowner homeowner, Tradesperson tradesperson, Project project, Task task, ReviewLevel level,
   int overall, Integer quality, Integer communication, Integer reliability, Integer professionalism, String body) {
  validateRatings(overall,quality,communication,reliability,professionalism);
  this.homeowner=homeowner; this.tradesperson=tradesperson; this.project=project; this.task=task; this.level=level;
  this.overallRating=overall; this.qualityRating=quality; this.communicationRating=communication;
  this.reliabilityRating=reliability; this.professionalismRating=professionalism; this.body=body; this.createdAt=Instant.now();
 }
 void edit(int overall,Integer quality,Integer communication,Integer reliability,Integer professionalism,String body){
  if(isWithdrawn()) throw new IllegalStateException("A withdrawn review cannot be edited");
  validateRatings(overall,quality,communication,reliability,professionalism);
  this.overallRating=overall;this.qualityRating=quality;this.communicationRating=communication;
  this.reliabilityRating=reliability;this.professionalismRating=professionalism;this.body=body;this.editedAt=Instant.now();
 }
 void withdraw(){if(withdrawnAt==null)withdrawnAt=Instant.now();} void hideForModeration(){if(moderationHiddenAt==null)moderationHiddenAt=Instant.now();}
 private static void validateRatings(Integer... ratings){for(Integer rating:ratings)if(rating!=null&&(rating<1||rating>5))throw new IllegalArgumentException("Every rating must be between 1 and 5");}
 public Long getId(){return id;} public Homeowner getHomeowner(){return homeowner;} public Tradesperson getTradesperson(){return tradesperson;}
 public Project getProject(){return project;} public Task getTask(){return task;} public ReviewLevel getLevel(){return level;}
 public Integer getOverallRating(){return overallRating;} public Integer getQualityRating(){return qualityRating;}
 public Integer getCommunicationRating(){return communicationRating;} public Integer getReliabilityRating(){return reliabilityRating;}
 public Integer getProfessionalismRating(){return professionalismRating;} public String getBody(){return body;}
 public Instant getCreatedAt(){return createdAt;} public Instant getEditedAt(){return editedAt;} public Instant getWithdrawnAt(){return withdrawnAt;}
 public Instant getModerationHiddenAt(){return moderationHiddenAt;} public boolean isWithdrawn(){return withdrawnAt!=null;}
 public boolean isHidden(){return withdrawnAt!=null||moderationHiddenAt!=null;}
}
