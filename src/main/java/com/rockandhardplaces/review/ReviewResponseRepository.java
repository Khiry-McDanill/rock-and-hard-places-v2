package com.rockandhardplaces.review;
import java.util.Optional;
import org.springframework.data.repository.Repository;
@org.springframework.stereotype.Repository
public interface ReviewResponseRepository extends Repository<ReviewResponse,Long>{
 ReviewResponse save(ReviewResponse response); Optional<ReviewResponse> findById(Long id); Optional<ReviewResponse> findByReview(Review review); long count();
}
