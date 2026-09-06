package com.rockandhardplaces.review;
import java.util.List;
import com.rockandhardplaces.account.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class ModerationService {
 private static final List<ModerationStatus> ACTIVE=List.of(ModerationStatus.OPEN,ModerationStatus.UNDER_REVIEW);
 private final ModerationReportRepository reports; private final ReviewRepository reviews; private final ReviewResponseRepository responses;
 public ModerationService(ModerationReportRepository reports,ReviewRepository reviews,ReviewResponseRepository responses){this.reports=reports;this.reviews=reviews;this.responses=responses;}
 public ModerationReport report(User reporter,Review review,String reason){if(reports.existsByReporterAndReviewAndStatusIn(reporter,review,ACTIVE))throw new IllegalStateException("An active report already exists");return reports.save(new ModerationReport(reporter,review,reason));}
 public ModerationReport report(User reporter,ReviewResponse response,String reason){if(reports.existsByReporterAndResponseAndStatusIn(reporter,response,ACTIVE))throw new IllegalStateException("An active report already exists");return reports.save(new ModerationReport(reporter,response,reason));}
 public void hideForPolicyViolation(ModerationReport report){if(report.getTargetType()==ModerationTargetType.REVIEW){report.getReview().hideForModeration();reviews.save(report.getReview());}else{report.getResponse().hideForModeration();responses.save(report.getResponse());}report.setStatus(ModerationStatus.RESOLVED);reports.save(report);}
 public ModerationReport updateStatus(ModerationReport report,ModerationStatus status){report.setStatus(status);return reports.save(report);}
}
