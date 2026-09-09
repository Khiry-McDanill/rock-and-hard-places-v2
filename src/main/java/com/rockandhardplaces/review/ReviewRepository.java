package com.rockandhardplaces.review;
import java.util.Optional;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;
import org.springframework.data.repository.Repository;
@org.springframework.stereotype.Repository
public interface ReviewRepository extends Repository<Review,Long> {
 java.util.List<Review> findByHomeownerAndTaskOrderByIdAsc(Homeowner homeowner, Task task);
 Review save(Review review); Optional<Review> findById(Long id); long count();
 boolean existsByHomeownerAndTradespersonAndTask(Homeowner homeowner,Tradesperson tradesperson,Task task);
 boolean existsByHomeownerAndTradespersonAndProjectAndLevel(Homeowner homeowner,Tradesperson tradesperson,Project project,ReviewLevel level);
}
