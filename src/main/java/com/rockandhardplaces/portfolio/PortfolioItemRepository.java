package com.rockandhardplaces.portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.rockandhardplaces.account.Tradesperson;
public interface PortfolioItemRepository extends JpaRepository<PortfolioItem,Long>{
 List<PortfolioItem> findByTradespersonOrderByCreatedAtDesc(Tradesperson tradesperson);
}
