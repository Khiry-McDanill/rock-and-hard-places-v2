package com.rockandhardplaces.account;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HomeownerRepository extends JpaRepository<Homeowner, Long> {
    Optional<Homeowner> findByUser(User user);
}
