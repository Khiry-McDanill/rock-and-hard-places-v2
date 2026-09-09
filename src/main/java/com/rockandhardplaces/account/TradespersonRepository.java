package com.rockandhardplaces.account;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TradespersonRepository extends JpaRepository<Tradesperson, Long> {
    Optional<Tradesperson> findByUser(User user);
    List<Tradesperson> findByAccountStatus(AccountStatus accountStatus);
    List<Tradesperson> findByAccountStatusAndUserNot(AccountStatus accountStatus, User user);
}
