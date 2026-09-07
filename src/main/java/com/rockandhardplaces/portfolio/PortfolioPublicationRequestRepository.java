package com.rockandhardplaces.portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import com.rockandhardplaces.communication.MessageAttachment;
import java.util.List;
public interface PortfolioPublicationRequestRepository extends JpaRepository<PortfolioPublicationRequest,Long>{
 boolean existsByPortfolioItemAndAttachment(PortfolioItem item,MessageAttachment attachment);
 List<PortfolioPublicationRequest> findByPortfolioItemAndStatus(PortfolioItem item,PublicationStatus status);
}
