package com.rockandhardplaces.portfolio;
import java.time.Instant;
import com.rockandhardplaces.account.Homeowner;
import com.rockandhardplaces.communication.MessageAttachment;
import jakarta.persistence.*;
@Entity @Table(name="portfolio_publication_requests",uniqueConstraints=@UniqueConstraint(name="uk_portfolio_publication_item_attachment",columnNames={"portfolio_item_id","attachment_id"}))
public class PortfolioPublicationRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="portfolio_item_id",nullable=false) private PortfolioItem portfolioItem;
 @ManyToOne(optional=false) @JoinColumn(name="attachment_id",nullable=false) private MessageAttachment attachment;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private PublicationStatus status;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="decided_at") private Instant decidedAt;
 @ManyToOne @JoinColumn(name="decided_by_homeowner_id") private Homeowner decidedBy;
 protected PortfolioPublicationRequest(){} PortfolioPublicationRequest(PortfolioItem item,MessageAttachment attachment){this.portfolioItem=item;this.attachment=attachment;status=PublicationStatus.PENDING;createdAt=Instant.now();}
 void decide(PublicationStatus decision,Homeowner homeowner){if(status!=PublicationStatus.PENDING)throw new IllegalStateException("Publication request is no longer pending");if(decision!=PublicationStatus.APPROVED&&decision!=PublicationStatus.DECLINED)throw new IllegalArgumentException("Homeowner decision must approve or decline");status=decision;decidedBy=homeowner;decidedAt=Instant.now();}
 void cancel(){if(status!=PublicationStatus.PENDING)throw new IllegalStateException("Publication request is no longer pending");status=PublicationStatus.CANCELLED;decidedAt=Instant.now();}
 public Long getId(){return id;} public PortfolioItem getPortfolioItem(){return portfolioItem;} public MessageAttachment getAttachment(){return attachment;} public PublicationStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public Instant getDecidedAt(){return decidedAt;} public Homeowner getDecidedBy(){return decidedBy;} public boolean isPublic(){return status==PublicationStatus.APPROVED;}
}
