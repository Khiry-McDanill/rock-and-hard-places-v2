package com.rockandhardplaces.project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rockandhardplaces.account.Tradesperson;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    List<TaskAssignment> findByTask(Task task);

    List<TaskAssignment> findByTradesperson(Tradesperson tradesperson);

    Optional<TaskAssignment> findByTaskAndTradesperson(Task task, Tradesperson tradesperson);
}