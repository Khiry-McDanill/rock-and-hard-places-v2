package com.rockandhardplaces.portfolio;
import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
import java.time.LocalDate; import java.util.Optional;
import com.rockandhardplaces.account.*; import com.rockandhardplaces.communication.*; import com.rockandhardplaces.project.*; import org.junit.jupiter.api.*;
class Rhp019PortfolioTests {
 PortfolioItemRepository items=mock(PortfolioItemRepository.class);PortfolioPublicationRequestRepository publications=mock(PortfolioPublicationRequestRepository.class);TaskAssignmentRepository assignments=mock(TaskAssignmentRepository.class);TaskProgressService progress=mock(TaskProgressService.class);PortfolioService service=new PortfolioService(items,publications,assignments,progress);
 User ownerUser=new User("owner@test.com"),otherUser=new User("other@test.com"),tradeUser=new User("trade@test.com");Homeowner owner=new Homeowner(ownerUser,"Owner"),other=new Homeowner(otherUser,"Other");Tradesperson trade=new Tradesperson(tradeUser,"Trade");Project project=new Project("P","D",ProjectStatus.IN_PROGRESS,"0",owner);
 @BeforeEach void save(){when(items.save(any())).thenAnswer(i->i.getArgument(0));when(publications.save(any())).thenAnswer(i->i.getArgument(0));}
 Task completed(){return new Task("T","D",TaskStatus.COMPLETED,project,null);}
 PortfolioItem verified(){Task task=completed();when(assignments.findByTaskAndTradesperson(task,trade)).thenReturn(Optional.of(new TaskAssignment(task,trade)));return service.create(trade,"Work","Done",PortfolioProvenance.RHP_VERIFIED,project,task,LocalDate.now());}
 MessageAttachment photo(){Conversation c=new Conversation(ConversationType.PROJECT_TEAM,project);Message m=new Message(c,tradeUser,"photo");return new MessageAttachment(m,"work.jpg","image/jpeg",10,"key",tradeUser);}
 @Test void completedAssignedWorkIsVerified(){assertThat(verified().isRhpVerified()).isTrue();}
 @Test void arbitraryVerifiedItemRejected(){assertThatThrownBy(()->service.create(trade,"Claim",null,PortfolioProvenance.RHP_VERIFIED,null,null,null)).isInstanceOf(IllegalArgumentException.class);}
 @Test void selfReportedRemainsDistinct(){PortfolioItem item=service.create(trade,"Outside",null,PortfolioProvenance.SELF_REPORTED,null,null,null);assertThat(item.isRhpVerified()).isFalse();assertThat(item.getProvenance()).isEqualTo(PortfolioProvenance.SELF_REPORTED);}
 @Test void externallyVerifiedHasNoRhpReviewContext(){PortfolioItem item=service.create(trade,"Outside",null,PortfolioProvenance.EXTERNALLY_VERIFIED,null,null,null);assertThat(item.getProject()).isNull();assertThat(item.getTask()).isNull();}
 @Test void requestStartsPending(){assertThat(service.requestPublication(trade,verified(),photo()).getStatus()).isEqualTo(PublicationStatus.PENDING);}
 @Test void ownerApproves(){PortfolioPublicationRequest request=service.requestPublication(trade,verified(),photo());service.decide(owner,request,PublicationStatus.APPROVED);assertThat(request.isPublic()).isTrue();assertThat(request.getDecidedBy()).isSameAs(owner);}
 @Test void unrelatedHomeownerCannotApprove(){PortfolioPublicationRequest request=service.requestPublication(trade,verified(),photo());assertThatThrownBy(()->service.decide(other,request,PublicationStatus.APPROVED)).isInstanceOf(IllegalArgumentException.class);}
 @Test void declinedStaysPrivateAndWorkVerified(){PortfolioItem item=verified();PortfolioPublicationRequest request=service.requestPublication(trade,item,photo());service.decide(owner,request,PublicationStatus.DECLINED);assertThat(request.isPublic()).isFalse();assertThat(item.isRhpVerified()).isTrue();assertThat(item.getTask().getStatus()).isEqualTo(TaskStatus.COMPLETED);}
}
