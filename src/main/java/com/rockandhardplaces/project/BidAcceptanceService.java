package com.rockandhardplaces.project;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rockandhardplaces.account.PersonTradeRepository;
import com.rockandhardplaces.account.AccountAuthorizationService;
import com.rockandhardplaces.account.Tradesperson;
import com.rockandhardplaces.account.Homeowner;

@Service
public class BidAcceptanceService {

    private final BidRepository bidRepository;
    private final PersonTradeRepository personTradeRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final ProjectTeamTradeRepository projectTeamTradeRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final AccountAuthorizationService authorization;

    public BidAcceptanceService(BidRepository bidRepository,
            PersonTradeRepository personTradeRepository,
            ProjectTeamRepository projectTeamRepository,
            ProjectTeamTradeRepository projectTeamTradeRepository,
            TaskAssignmentRepository taskAssignmentRepository,
            AccountAuthorizationService authorization) {
        this.bidRepository = bidRepository;
        this.personTradeRepository = personTradeRepository;
        this.projectTeamRepository = projectTeamRepository;
        this.projectTeamTradeRepository = projectTeamTradeRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.authorization = authorization;
    }

    @Transactional
    public Bid acceptBid(Bid bid) {
        authorization.requireActive(bid.getTask().getProject().getHomeowner());
        authorization.requireActive(bid.getTradesperson());
        authorization.requireDifferentUsers(bid.getTask().getProject().getHomeowner().getUser(),
                bid.getTradesperson().getUser());
        validateTaskTrade(bid);
        if (bid.getStatus() == BidStatus.ACCEPTED) {
            return bid;
        }
        if (bid.getStatus() != BidStatus.SUBMITTED) {
            throw new IllegalStateException("Only a submitted bid can be accepted");
        }

        validateQualification(bid);
        bidRepository.findByTaskTradeAndStatusAndIdNot(
                bid.getTaskTrade(), BidStatus.ACCEPTED, bid.getId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("TaskTrade already has an accepted bid");
                });

        ProjectTeam projectTeam = projectTeamRepository
                .findByProjectAndTradesperson(bid.getTask().getProject(), bid.getTradesperson())
                .map(this::activateMembership)
                .orElseGet(() -> projectTeamRepository.saveAndFlush(new ProjectTeam(
                        bid.getTask().getProject(), bid.getTradesperson(), ProjectTeamStatus.ACTIVE)));

        TradeRole.ensure(projectTeam, bid.getTaskTrade(), projectTeamTradeRepository);
        taskAssignmentRepository.findByTaskAndTradesperson(bid.getTask(), bid.getTradesperson())
                .orElseGet(() -> taskAssignmentRepository.saveAndFlush(
                        new TaskAssignment(bid.getTask(), bid.getTradesperson())));

        bidRepository.findByTaskTradeAndStatus(bid.getTaskTrade(), BidStatus.SUBMITTED).stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), bid.getId()))
                .forEach(candidate -> {
                    candidate.reject();
                    bidRepository.save(candidate);
                });
        bid.accept();
        return bidRepository.saveAndFlush(bid);
    }

    /**
     * Actor-aware entry point for delivery layers. Keeping this check here prevents
     * callers from accidentally accepting a bid on somebody else's project.
     */
    @Transactional
    public Bid acceptBid(Bid bid, Homeowner actor) {
        authorization.requireActive(actor);
        Homeowner owner = bid.getTask().getProject().getHomeowner();
        if (actor != owner && (actor.getId() == null || !Objects.equals(actor.getId(), owner.getId()))) {
            throw new SecurityException("Only the project homeowner may accept a bid");
        }
        return acceptBid(bid);
    }

    private ProjectTeam activateMembership(ProjectTeam projectTeam) {
        if (projectTeam.getStatus() == ProjectTeamStatus.SUSPENDED) {
            throw new IllegalStateException("Suspended ProjectTeam membership cannot accept a bid");
        }
        if (projectTeam.getStatus() != ProjectTeamStatus.ACTIVE) {
            projectTeam.setStatus(ProjectTeamStatus.ACTIVE);
            return projectTeamRepository.saveAndFlush(projectTeam);
        }
        return projectTeam;
    }

    private void validateTaskTrade(Bid bid) {
        if (!Objects.equals(bid.getTask().getId(), bid.getTaskTrade().getTask().getId())) {
            throw new IllegalArgumentException("TaskTrade must belong to the bid task");
        }
    }

    private void validateQualification(Bid bid) {
        Tradesperson tradesperson = bid.getTradesperson();
        boolean qualified = personTradeRepository.findByTradesperson(tradesperson).stream()
                .anyMatch(personTrade -> Objects.equals(
                        personTrade.getTrade().getId(), bid.getTaskTrade().getTrade().getId()));
        if (!qualified) {
            throw new IllegalArgumentException("Tradesperson is not qualified for the bid TaskTrade");
        }
    }

    private static final class TradeRole {

        private TradeRole() {
        }

        private static void ensure(ProjectTeam projectTeam, TaskTrade taskTrade,
                ProjectTeamTradeRepository repository) {
            repository.findByProjectTeamAndTrade(projectTeam, taskTrade.getTrade())
                    .orElseGet(() -> repository.saveAndFlush(
                            new ProjectTeamTrade(projectTeam, taskTrade.getTrade())));
        }
    }
}
