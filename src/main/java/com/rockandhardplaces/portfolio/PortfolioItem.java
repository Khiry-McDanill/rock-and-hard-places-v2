package com.rockandhardplaces.portfolio;
import java.time.*;
import com.rockandhardplaces.account.Tradesperson;
import com.rockandhardplaces.project.*;
import jakarta.persistence.*;
@Entity @Table(name="portfolio_items")
public class PortfolioItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="tradesperson_id",nullable=false) private Tradesperson tradesperson;
 @Column(nullable=false) private String title; @Column(length=5000) private String description;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private PortfolioProvenance provenance;
 @ManyToOne @JoinColumn(name="project_id") private Project project; @ManyToOne @JoinColumn(name="task_id") private Task task;
 @Column(name="completion_date") private LocalDate completionDate; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected PortfolioItem(){} PortfolioItem(Tradesperson owner,String title,String description,PortfolioProvenance provenance,Project project,Task task,LocalDate completionDate){if(title==null||title.isBlank())throw new IllegalArgumentException("Portfolio title is required");this.tradesperson=owner;this.title=title;this.description=description;this.provenance=provenance;this.project=project;this.task=task;this.completionDate=completionDate;createdAt=Instant.now();}
 public Long getId(){return id;} public Tradesperson getTradesperson(){return tradesperson;} public String getTitle(){return title;} public String getDescription(){return description;} public PortfolioProvenance getProvenance(){return provenance;} public Project getProject(){return project;} public Task getTask(){return task;} public LocalDate getCompletionDate(){return completionDate;} public Instant getCreatedAt(){return createdAt;} public boolean isRhpVerified(){return provenance==PortfolioProvenance.RHP_VERIFIED;}
}
