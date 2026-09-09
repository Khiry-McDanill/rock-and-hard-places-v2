package com.rockandhardplaces.account;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import com.rockandhardplaces.project.*;
import com.rockandhardplaces.review.*;
import com.rockandhardplaces.portfolio.*;
class SelfDealingBoundaryTests {
 User user=new User("dual@example.com");Homeowner homeowner=new Homeowner(user,"Jordan");Tradesperson worker=new Tradesperson(user,"Jordan");
 Project project=new Project("Own project","",ProjectStatus.IN_PROGRESS,"19147",homeowner);
 Task task=new Task("Own task","",TaskStatus.COMPLETED,project,null);
 @Test void selfReviewCannotCreateReputation() {
  var repo=mock(ReviewRepository.class);var service=new ReviewService(repo,mock(ReviewResponseRepository.class),mock(TaskAssignmentRepository.class),mock(TaskProgressService.class));
  assertThatThrownBy(()->service.createTaskReview(homeowner,worker,task,5,null,null,null,null,"Self")).hasMessageContaining("Self-review");
  assertThatThrownBy(()->service.createProjectReview(homeowner,worker,project,5,null,null,null,null,"Self")).hasMessageContaining("Self-review");verifyNoInteractions(repo);
 }
 @Test void selfAwardCannotCreateAssignment() {
  var repo=mock(BidRepository.class);var assignments=mock(TaskAssignmentRepository.class);
  var service=new BidAcceptanceService(repo,mock(PersonTradeRepository.class),mock(ProjectTeamRepository.class),mock(ProjectTeamTradeRepository.class),assignments,new AccountAuthorizationService());
  var bid=mock(Bid.class);when(bid.getTask()).thenReturn(task);when(bid.getTradesperson()).thenReturn(worker);
  assertThatThrownBy(()->service.acceptBid(bid)).isInstanceOf(SecurityException.class);verifyNoInteractions(repo,assignments);
 }
 @Test void selfWorkCannotManufactureVerifiedProvenance() {
  var repo=mock(PortfolioItemRepository.class);var service=new PortfolioService(repo,mock(PortfolioPublicationRequestRepository.class),mock(TaskAssignmentRepository.class),mock(TaskProgressService.class));
  assertThatThrownBy(()->service.create(worker,"Self work","",PortfolioProvenance.RHP_VERIFIED,project,task,null)).isInstanceOf(SecurityException.class);verifyNoInteractions(repo);
 }
}
