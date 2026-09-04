package com.rockandhardplaces.project;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.rockandhardplaces.account.PersonTrade;
import com.rockandhardplaces.account.PersonTradeRepository;
import com.rockandhardplaces.account.Tradesperson;
import com.rockandhardplaces.catalog.Trade;

/**
 * Service for task assignment validation and creation.
 *
 * Enforces assignment eligibility rules:
 * 1. Tradesperson must have ACTIVE ProjectTeam membership for the Task's
 * Project.
 * 2. Tradesperson must have PersonTrade qualification matching at least one
 * TaskTrade requirement.
 * 3. Tradesperson must have ProjectTeamTrade role matching at least one
 * TaskTrade requirement for the project.
 * 4. Assignment does not implicitly create ProjectTeam or ProjectTeamTrade
 * records.
 */
@Service
public class TaskAssignmentService {

    private final ProjectTeamRepository projectTeamRepository;
    private final ProjectTeamTradeRepository projectTeamTradeRepository;
    private final PersonTradeRepository personTradeRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    public TaskAssignmentService(ProjectTeamRepository projectTeamRepository,
            ProjectTeamTradeRepository projectTeamTradeRepository,
            PersonTradeRepository personTradeRepository,
            TaskAssignmentRepository taskAssignmentRepository) {
        this.projectTeamRepository = projectTeamRepository;
        this.projectTeamTradeRepository = projectTeamTradeRepository;
        this.personTradeRepository = personTradeRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
    }

    /**
     * Validates that a Tradesperson is eligible to be assigned to a Task.
     *
     * Eligibility requires:
     * 1. ACTIVE ProjectTeam membership for the Task's Project.
     * 2. At least one PersonTrade qualification matching at least one
     * TaskTrade requirement.
     * 3. At least one ProjectTeamTrade role matching at least one TaskTrade
     * requirement.
     *
     * @param task the Task to assign to
     * @param tradesperson the Tradesperson to assign
     * @throws IllegalArgumentException if eligibility is not met
     */
    public void validateAssignmentEligibility(Task task, Tradesperson tradesperson) {
        // Rule 1: Check ACTIVE project membership
        ProjectTeam membership = projectTeamRepository
                .findActiveMembership(task.getProject(), tradesperson, ProjectTeamStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(
                                "Tradesperson %d cannot be assigned to Task %d: "
                                        + "no ACTIVE ProjectTeam membership in Project %d",
                                tradesperson.getId(), task.getId(), task.getProject().getId())));

        // Rule 2: Check qualification matching
        validateQualificationMatching(task, tradesperson);

        // Rule 3: Check project-specific role matching
        validateProjectTeamTradeMatching(task, membership);
    }

    /**
     * Validates that the Tradesperson has at least one PersonTrade
     * qualification matching at least one TaskTrade requirement.
     */
    private void validateQualificationMatching(Task task, Tradesperson tradesperson) {
        List<TaskTrade> requirements = task.getTaskTrades();

        // If no requirements are specified, any qualified/ACTIVE member can work
        if (requirements.isEmpty()) {
            return;
        }

        List<PersonTrade> qualifications = personTradeRepository.findByTradesperson(tradesperson);

        Set<Trade> requiredTrades = requirements.stream()
                .map(TaskTrade::getTrade)
                .collect(Collectors.toSet());

        Set<Trade> qualifiedTrades = qualifications.stream()
                .map(PersonTrade::getTrade)
                .collect(Collectors.toSet());

        boolean hasMatchingQualification = qualifiedTrades.stream()
                .anyMatch(requiredTrades::contains);

        if (!hasMatchingQualification) {
            String requiredTradeNames = requiredTrades.stream()
                    .map(Trade::getName)
                    .collect(Collectors.joining(", "));
            String qualifiedTradeNames = qualifiedTrades.isEmpty() ? "(none)"
                    : qualifiedTrades.stream()
                            .map(Trade::getName)
                            .collect(Collectors.joining(", "));

            throw new IllegalArgumentException(
                    String.format(
                            "Tradesperson %d cannot be assigned to Task %d: "
                                    + "no matching qualifications. Task requires: [%s]; Tradesperson qualified for: [%s]",
                            tradesperson.getId(), task.getId(), requiredTradeNames, qualifiedTradeNames));
        }
    }

    /**
     * Validates that the ProjectTeam has at least one ProjectTeamTrade role
     * matching at least one TaskTrade requirement.
     */
    private void validateProjectTeamTradeMatching(Task task, ProjectTeam membership) {
        List<TaskTrade> requirements = task.getTaskTrades();

        // If no requirements are specified, any ACTIVE member can work
        if (requirements.isEmpty()) {
            return;
        }

        List<ProjectTeamTrade> roles = projectTeamTradeRepository.findByProjectTeam(membership);

        Set<Trade> requiredTrades = requirements.stream()
                .map(TaskTrade::getTrade)
                .collect(Collectors.toSet());

        Set<Trade> assignedRoles = roles.stream()
                .map(ProjectTeamTrade::getTrade)
                .collect(Collectors.toSet());

        boolean hasMatchingRole = assignedRoles.stream()
                .anyMatch(requiredTrades::contains);

        if (!hasMatchingRole) {
            String requiredTradeNames = requiredTrades.stream()
                    .map(Trade::getName)
                    .collect(Collectors.joining(", "));
            String assignedRoleNames = assignedRoles.isEmpty() ? "(none)"
                    : assignedRoles.stream()
                            .map(Trade::getName)
                            .collect(Collectors.joining(", "));

            throw new IllegalArgumentException(
                    String.format(
                            "Tradesperson %d cannot be assigned to Task %d: "
                                    + "no matching project roles. Task requires: [%s]; Tradesperson has roles: [%s]",
                            membership.getTradesperson().getId(), task.getId(), requiredTradeNames, assignedRoleNames));
        }
    }

    /**
     * Creates and persists a TaskAssignment after validating eligibility.
     *
     * @param task the Task to assign to
     * @param tradesperson the Tradesperson to assign
     * @return the persisted TaskAssignment
     * @throws IllegalArgumentException if eligibility validation fails
     */
    public TaskAssignment createEligibleAssignment(Task task, Tradesperson tradesperson) {
        validateAssignmentEligibility(task, tradesperson);
        TaskAssignment assignment = new TaskAssignment(task, tradesperson);
        return taskAssignmentRepository.saveAndFlush(assignment);
    }

    /**
     * Checks if a Tradesperson is currently eligible to be assigned to a Task
     * (without throwing exception).
     */
    public boolean isEligibleForAssignment(Task task, Tradesperson tradesperson) {
        try {
            validateAssignmentEligibility(task, tradesperson);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Finds all TaskAssignments for a Tradesperson that are now ineligible due
     * to loss of ACTIVE project membership.
     *
     * Used when membership status changes to identify assignments that should
     * be cleaned up.
     */
    public List<TaskAssignment> findIneligibleAssignmentsByLostMembership(
            Tradesperson tradesperson, Project project) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTradesperson(tradesperson);

        return assignments.stream()
                .filter(assignment -> assignment.getTask().getProject().getId().equals(project.getId()))
                .filter(assignment -> {
                    boolean stillHasActiveMembership = projectTeamRepository
                            .findActiveMembership(project, tradesperson, ProjectTeamStatus.ACTIVE)
                            .isPresent();
                    return !stillHasActiveMembership;
                })
                .collect(Collectors.toList());
    }

    /**
     * Finds all TaskAssignments for a Tradesperson that may be ineligible due
     * to loss of qualifications.
     *
     * Returns assignments where the tradesperson no longer has a matching
     * qualification.
     */
    public List<TaskAssignment> findIneligibleAssignmentsByLostQualification(Tradesperson tradesperson) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTradesperson(tradesperson);
        List<PersonTrade> currentQualifications = personTradeRepository.findByTradesperson(tradesperson);

        Set<Trade> qualifiedTrades = currentQualifications.stream()
                .map(PersonTrade::getTrade)
                .collect(Collectors.toSet());

        return assignments.stream()
                .filter(assignment -> {
                    List<TaskTrade> taskRequirements = assignment.getTask().getTaskTrades();

                    // If no requirements, not ineligible
                    if (taskRequirements.isEmpty()) {
                        return false;
                    }

                    // Check if any required trade matches a qualified trade
                    boolean hasAnyMatch = taskRequirements.stream()
                            .map(TaskTrade::getTrade)
                            .anyMatch(qualifiedTrades::contains);

                    return !hasAnyMatch;
                })
                .collect(Collectors.toList());
    }
}
