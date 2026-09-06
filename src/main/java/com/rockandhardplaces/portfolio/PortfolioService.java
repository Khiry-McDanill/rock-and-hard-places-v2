package com.rockandhardplaces.portfolio;
import java.time.LocalDate;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.communication.MessageAttachment;
import com.rockandhardplaces.project.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class PortfolioService {
 private final PortfolioItemRepository items; private final PortfolioPublicationRequestRepository publications; private final TaskAssignmentRepository assignments; private final TaskProgressService progress;
 private final AccountAuthorizationService authorization;
 @Autowired public PortfolioService(PortfolioItemRepository items,PortfolioPublicationRequestRepository publications,TaskAssignmentRepository assignments,TaskProgressService progress,AccountAuthorizationService authorization){this.items=items;this.publications=publications;this.assignments=assignments;this.progress=progress;this.authorization=authorization;}
 public PortfolioService(PortfolioItemRepository items,PortfolioPublicationRequestRepository publications,TaskAssignmentRepository assignments,TaskProgressService progress){this(items,publications,assignments,progress,new AccountAuthorizationService());}
 public PortfolioItem create(Tradesperson owner,String title,String description,PortfolioProvenance provenance,Project project,Task task,LocalDate completionDate){
  if(owner==null||provenance==null)throw new IllegalArgumentException("Portfolio owner and provenance are required");
  authorization.requireActive(owner);
  if(task!=null&&(project==null||task.getProject()!=project))throw new IllegalArgumentException("Task must belong to the source project");
  if(project!=null)authorization.requireDifferentUsers(owner.getUser(),project.getHomeowner().getUser());
  if(provenance==PortfolioProvenance.RHP_VERIFIED)requireVerifiedWork(owner,project,task);
  else if(project!=null||task!=null)throw new IllegalArgumentException("Outside-work provenance cannot claim an RH&P project or task");
  return items.save(new PortfolioItem(owner,title,description,provenance,project,task,completionDate));
 }
 public PortfolioPublicationRequest requestPublication(Tradesperson actor,PortfolioItem item,MessageAttachment attachment){
  authorization.requireActive(actor);
  if(!same(actor,item.getTradesperson()))throw new IllegalArgumentException("Only the portfolio owner may select a photo");
  if(!item.isRhpVerified()||item.getProject()==null)throw new IllegalArgumentException("RH&P publication requires verified project work");
  authorization.requireDifferentUsers(actor.getUser(),item.getProject().getHomeowner().getUser());
  if(attachment==null||attachment.getContentType()==null||!attachment.getContentType().startsWith("image/")||attachment.getMessage().getConversation().getProject()!=item.getProject())throw new IllegalArgumentException("Photo does not originate from the portfolio project's authoritative media");
  if(publications.existsByPortfolioItemAndAttachment(item,attachment))throw new IllegalStateException("This photo already has a publication request");
  return publications.save(new PortfolioPublicationRequest(item,attachment));
 }
 public PortfolioPublicationRequest decide(Homeowner actor,PortfolioPublicationRequest request,PublicationStatus decision){
  authorization.requireActive(actor);
  authorization.requireDifferentUsers(actor.getUser(),request.getPortfolioItem().getTradesperson().getUser());
  if(!same(actor,request.getPortfolioItem().getProject().getHomeowner()))throw new IllegalArgumentException("Only the source project homeowner may decide publication");
  request.decide(decision,actor);return publications.save(request);
 }
 public PortfolioPublicationRequest cancel(Tradesperson actor,PortfolioPublicationRequest request){authorization.requireActive(actor);if(!same(actor,request.getPortfolioItem().getTradesperson()))throw new IllegalArgumentException("Only the portfolio owner may cancel publication");request.cancel();return publications.save(request);}
 private boolean same(Tradesperson a,Tradesperson b){return a==b||(a!=null&&b!=null&&a.getId()!=null&&a.getId().equals(b.getId()));}
 private boolean same(Homeowner a,Homeowner b){return a==b||(a!=null&&b!=null&&a.getId()!=null&&a.getId().equals(b.getId()));}
 private void requireVerifiedWork(Tradesperson owner,Project project,Task task){
  if(project==null)throw new IllegalArgumentException("RH&P verified work requires a source project");
  if(task!=null){if(task.getStatus()!=TaskStatus.COMPLETED||assignments.findByTaskAndTradesperson(task,owner).isEmpty())throw new IllegalArgumentException("RH&P verified task must be completed work performed by this tradesperson");return;}
  if(!progress.isProjectComplete(project))throw new IllegalArgumentException("RH&P verified project must be complete");
  boolean performed=project.getTasks().stream().filter(t->t.getStatus()==TaskStatus.COMPLETED).anyMatch(t->assignments.findByTaskAndTradesperson(t,owner).isPresent());
  if(!performed)throw new IllegalArgumentException("Tradesperson did not perform completed work on this project");
 }
}
