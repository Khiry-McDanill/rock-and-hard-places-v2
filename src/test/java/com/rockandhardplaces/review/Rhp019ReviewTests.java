package com.rockandhardplaces.review;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;
import org.junit.jupiter.api.*;
class Rhp019ReviewTests {
 ReviewRepository reviews=mock(ReviewRepository.class); ReviewResponseRepository responses=mock(ReviewResponseRepository.class);
 TaskAssignmentRepository assignments=mock(TaskAssignmentRepository.class); TaskProgressService progress=mock(TaskProgressService.class);
 ReviewService service=new ReviewService(reviews,responses,assignments,progress);
 User ownerUser=new User("owner@test.com"), tradeUser=new User("trade@test.com"), otherUser=new User("other@test.com");
 Homeowner owner=new Homeowner(ownerUser,"Owner"), otherOwner=new Homeowner(otherUser,"Other");
 Tradesperson trade=new Tradesperson(tradeUser,"Trade"), otherTrade=new Tradesperson(otherUser,"Other trade");
 Project project=new Project("P","D",ProjectStatus.IN_PROGRESS,"00000",owner);
 @BeforeEach void saves(){when(reviews.save(any())).thenAnswer(i->i.getArgument(0));when(responses.save(any())).thenAnswer(i->i.getArgument(0));}
 Task task(TaskStatus status){return new Task("T","D",status,project,null);}
 Review taskReview(){Task task=task(TaskStatus.COMPLETED);when(assignments.findByTaskAndTradesperson(task,trade)).thenReturn(Optional.of(new TaskAssignment(task,trade)));return service.createTaskReview(owner,trade,task,5,4,3,2,1,"Good");}
 @Test void validTaskReview(){assertThat(taskReview().getLevel()).isEqualTo(ReviewLevel.TASK);}
 @Test void incompleteTaskRejected(){assertThatThrownBy(()->service.createTaskReview(owner,trade,task(TaskStatus.IN_PROGRESS),5,null,null,null,null,null)).isInstanceOf(IllegalArgumentException.class);}
 @Test void nonPerformerRejected(){assertThatThrownBy(()->service.createTaskReview(owner,trade,task(TaskStatus.COMPLETED),5,null,null,null,null,null)).isInstanceOf(IllegalArgumentException.class);}
 @Test void duplicateTaskRejected(){Task task=task(TaskStatus.COMPLETED);when(assignments.findByTaskAndTradesperson(task,trade)).thenReturn(Optional.of(new TaskAssignment(task,trade)));when(reviews.existsByHomeownerAndTradespersonAndTask(owner,trade,task)).thenReturn(true);assertThatThrownBy(()->service.createTaskReview(owner,trade,task,5,null,null,null,null,null)).isInstanceOf(IllegalStateException.class);}
 @Test void validProjectReview(){Task task=task(TaskStatus.COMPLETED);when(progress.isProjectComplete(project)).thenReturn(true);when(assignments.findByTaskAndTradesperson(task,trade)).thenReturn(Optional.of(new TaskAssignment(task,trade)));assertThat(service.createProjectReview(owner,trade,project,5,null,null,null,null,null).getTask()).isNull();}
 @Test void incompleteProjectRejected(){assertThatThrownBy(()->service.createProjectReview(owner,trade,project,5,null,null,null,null,null)).isInstanceOf(IllegalArgumentException.class);}
 @Test void unrelatedHomeownerRejected(){assertThatThrownBy(()->service.createTaskReview(otherOwner,trade,task(TaskStatus.COMPLETED),5,null,null,null,null,null)).isInstanceOf(IllegalArgumentException.class);}
 @Test void eachInvalidRatingRejected(){for(int[] values:new int[][]{{0,1,1,1,1},{5,6,1,1,1},{5,1,0,1,1},{5,1,1,6,1},{5,1,1,1,0}})assertThatThrownBy(()->new Review(owner,trade,project,null,ReviewLevel.PROJECT,values[0],values[1],values[2],values[3],values[4],null)).isInstanceOf(IllegalArgumentException.class);}
 @Test void editSetsMarker(){Review review=taskReview();service.edit(owner,review,4,null,null,null,null,"Edited");assertThat(review.getEditedAt()).isNotNull();assertThat(review.getOverallRating()).isEqualTo(4);}
 @Test void withdrawalPreservesRecord(){Review review=taskReview();service.withdraw(owner,review);assertThat(review.isWithdrawn()).isTrue();assertThat(review.getBody()).isEqualTo("Good");verify(reviews,never()).findById(any());}
 @Test void subjectCanRespond(){assertThat(service.respond(trade,taskReview(),"Thanks").getTradesperson()).isSameAs(trade);}
 @Test void unrelatedTradeCannotRespond(){assertThatThrownBy(()->service.respond(otherTrade,taskReview(),"No")).isInstanceOf(IllegalArgumentException.class);}
 @Test void duplicateResponseRejected(){Review review=taskReview();when(responses.findByReview(review)).thenReturn(Optional.of(mock(ReviewResponse.class)));assertThatThrownBy(()->service.respond(trade,review,"Again")).isInstanceOf(IllegalStateException.class);}
 @Test void responseEditSetsMarker(){ReviewResponse response=service.respond(trade,taskReview(),"Thanks");service.editResponse(trade,response,"Updated");assertThat(response.getEditedAt()).isNotNull();}
}
