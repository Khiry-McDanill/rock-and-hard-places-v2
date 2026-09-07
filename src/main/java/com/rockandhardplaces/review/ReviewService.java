package com.rockandhardplaces.review;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class ReviewService {
 private final ReviewRepository reviews; private final ReviewResponseRepository responses; private final TaskAssignmentRepository assignments; private final TaskProgressService progress; private final AccountAuthorizationService authorization;
 @org.springframework.beans.factory.annotation.Autowired public ReviewService(ReviewRepository reviews,ReviewResponseRepository responses,TaskAssignmentRepository assignments,TaskProgressService progress,AccountAuthorizationService authorization){this.reviews=reviews;this.responses=responses;this.assignments=assignments;this.progress=progress;this.authorization=authorization;}
 public ReviewService(ReviewRepository reviews,ReviewResponseRepository responses,TaskAssignmentRepository assignments,TaskProgressService progress){this(reviews,responses,assignments,progress,new AccountAuthorizationService());}
 public Review createTaskReview(Homeowner author,Tradesperson subject,Task task,int overall,Integer quality,Integer communication,Integer reliability,Integer professionalism,String body){
  authorization.requireActive(author); authorization.requireActive(subject); requireOwner(author,task.getProject()); requireNotSelf(author,subject);
  if(task.getStatus()!=TaskStatus.COMPLETED)throw new IllegalArgumentException("Task must be completed through the RH&P workflow");
  if(assignments.findByTaskAndTradesperson(task,subject).isEmpty())throw new IllegalArgumentException("Tradesperson did not perform this task");
  if(reviews.existsByHomeownerAndTradespersonAndTask(author,subject,task))throw new IllegalStateException("This task review already exists");
  return reviews.save(new Review(author,subject,task.getProject(),task,ReviewLevel.TASK,overall,quality,communication,reliability,professionalism,body));
 }
 public Review createProjectReview(Homeowner author,Tradesperson subject,Project project,int overall,Integer quality,Integer communication,Integer reliability,Integer professionalism,String body){
  authorization.requireActive(author); authorization.requireActive(subject); requireOwner(author,project);requireNotSelf(author,subject);
  if(!progress.isProjectComplete(project))throw new IllegalArgumentException("Project is not complete");
  boolean performed=project.getTasks().stream().filter(t->t.getStatus()==TaskStatus.COMPLETED).anyMatch(t->assignments.findByTaskAndTradesperson(t,subject).isPresent());
  if(!performed)throw new IllegalArgumentException("Tradesperson did not perform work on this project");
  if(reviews.existsByHomeownerAndTradespersonAndProjectAndLevel(author,subject,project,ReviewLevel.PROJECT))throw new IllegalStateException("This project review already exists");
  return reviews.save(new Review(author,subject,project,null,ReviewLevel.PROJECT,overall,quality,communication,reliability,professionalism,body));
 }
 public Review edit(Homeowner actor,Review review,int overall,Integer quality,Integer communication,Integer reliability,Integer professionalism,String body){authorization.requireActive(actor);requireAuthor(actor,review);review.edit(overall,quality,communication,reliability,professionalism,body);return reviews.save(review);}
 public Review withdraw(Homeowner actor,Review review){authorization.requireActive(actor);requireAuthor(actor,review);review.withdraw();return reviews.save(review);}
 public ReviewResponse respond(Tradesperson actor,Review review,String body){authorization.requireActive(actor);requireSubject(actor,review);if(responses.findByReview(review).isPresent())throw new IllegalStateException("Review already has a response");return responses.save(new ReviewResponse(review,actor,body));}
 public ReviewResponse editResponse(Tradesperson actor,ReviewResponse response,String body){requireSubject(actor,response.getReview());response.edit(body);return responses.save(response);}
 private void requireOwner(Homeowner actor,Project project){if(actor==null||!same(actor,project.getHomeowner()))throw new IllegalArgumentException("Only the project homeowner may review");}
 private void requireAuthor(Homeowner actor,Review review){if(!same(actor,review.getHomeowner()))throw new IllegalArgumentException("Only the author may change this review");}
 private void requireSubject(Tradesperson actor,Review review){if(!same(actor,review.getTradesperson()))throw new IllegalArgumentException("Only the reviewed tradesperson may respond");}
 private void requireNotSelf(Homeowner author,Tradesperson subject){if(same(author.getUser(),subject.getUser()))throw new IllegalArgumentException("Self-review is not allowed");}
 private boolean same(Homeowner a,Homeowner b){return a==b||(a!=null&&b!=null&&a.getId()!=null&&a.getId().equals(b.getId()));}
 private boolean same(Tradesperson a,Tradesperson b){return a==b||(a!=null&&b!=null&&a.getId()!=null&&a.getId().equals(b.getId()));}
 private boolean same(User a,User b){return a==b||(a!=null&&b!=null&&a.getId()!=null&&a.getId().equals(b.getId()));}
}
