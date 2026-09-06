package com.rockandhardplaces.review;
import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
import com.rockandhardplaces.account.*; import com.rockandhardplaces.project.*; import org.junit.jupiter.api.*;
class Rhp019ModerationTests {
 ModerationReportRepository reports=mock(ModerationReportRepository.class);ReviewRepository reviews=mock(ReviewRepository.class);ReviewResponseRepository responses=mock(ReviewResponseRepository.class);ModerationService service=new ModerationService(reports,reviews,responses);
 User reporter=new User("report@test.com");Tradesperson trade=new Tradesperson(new User("trade@test.com"),"Trade");Homeowner owner=new Homeowner(new User("owner@test.com"),"Owner");Project project=new Project("P","D",ProjectStatus.COMPLETED,"0",owner);Review review=new Review(owner,trade,project,null,ReviewLevel.PROJECT,5,null,null,null,null,"Text");ReviewResponse response=new ReviewResponse(review,trade,"Reply");
 @BeforeEach void save(){when(reports.save(any())).thenAnswer(i->i.getArgument(0));}
 @Test void reportReviewDoesNotHide(){ModerationReport report=service.report(reporter,review,"Policy");assertThat(report.getTargetType()).isEqualTo(ModerationTargetType.REVIEW);assertThat(review.isHidden()).isFalse();}
 @Test void reportResponseDoesNotHide(){ModerationReport report=service.report(reporter,response,"Policy");assertThat(report.getTargetType()).isEqualTo(ModerationTargetType.REVIEW_RESPONSE);assertThat(response.isHidden()).isFalse();}
 @Test void duplicateActiveReportRejected(){when(reports.existsByReporterAndReviewAndStatusIn(eq(reporter),eq(review),any())).thenReturn(true);assertThatThrownBy(()->service.report(reporter,review,"Again")).isInstanceOf(IllegalStateException.class);}
 @Test void moderationHidePreservesReview(){ModerationReport report=service.report(reporter,review,"Policy");service.hideForPolicyViolation(report);assertThat(review.isHidden()).isTrue();assertThat(review.getBody()).isEqualTo("Text");assertThat(report.getStatus()).isEqualTo(ModerationStatus.RESOLVED);verify(reviews).save(review);}
 @Test void moderationHidePreservesResponse(){ModerationReport report=service.report(reporter,response,"Policy");service.hideForPolicyViolation(report);assertThat(response.isHidden()).isTrue();assertThat(response.getBody()).isEqualTo("Reply");verify(responses).save(response);}
}
