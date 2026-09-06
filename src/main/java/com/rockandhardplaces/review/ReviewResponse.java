package com.rockandhardplaces.review;
import java.time.Instant;
import com.rockandhardplaces.account.Tradesperson;
import jakarta.persistence.*;
@Entity @Table(name="review_responses")
public class ReviewResponse {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(optional=false) @JoinColumn(name="review_id",nullable=false,unique=true) private Review review;
 @ManyToOne(optional=false) @JoinColumn(name="tradesperson_id",nullable=false) private Tradesperson tradesperson;
 @Column(nullable=false,length=10000) private String body;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="edited_at") private Instant editedAt;
 @Column(name="moderation_hidden_at") private Instant moderationHiddenAt;
 protected ReviewResponse(){} ReviewResponse(Review review,Tradesperson tradesperson,String body){requireBody(body);this.review=review;this.tradesperson=tradesperson;this.body=body;createdAt=Instant.now();}
 void edit(String body){requireBody(body);this.body=body;editedAt=Instant.now();} void hideForModeration(){if(moderationHiddenAt==null)moderationHiddenAt=Instant.now();}
 private static void requireBody(String body){if(body==null||body.isBlank())throw new IllegalArgumentException("Response body is required");}
 public Long getId(){return id;} public Review getReview(){return review;} public Tradesperson getTradesperson(){return tradesperson;} public String getBody(){return body;}
 public Instant getCreatedAt(){return createdAt;} public Instant getEditedAt(){return editedAt;} public Instant getModerationHiddenAt(){return moderationHiddenAt;} public boolean isHidden(){return moderationHiddenAt!=null;}
}
