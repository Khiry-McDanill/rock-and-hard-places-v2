package com.rockandhardplaces.review;
import java.util.*;
import com.rockandhardplaces.account.User;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ModerationReportRepository extends JpaRepository<ModerationReport,Long>{
 boolean existsByReporterAndReviewAndStatusIn(User reporter,Review review,Collection<ModerationStatus> statuses);
 boolean existsByReporterAndResponseAndStatusIn(User reporter,ReviewResponse response,Collection<ModerationStatus> statuses);
}
