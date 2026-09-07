package com.rockandhardplaces.project;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.rockandhardplaces.account.Homeowner;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByHomeownerOrderByIdAsc(Homeowner homeowner);
}
