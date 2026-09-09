package com.rockandhardplaces.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.project.*;

@RestController
@RequestMapping("/api/bids")
@Transactional
class ProposalController {
    private final ActiveAccountContext account;
    private final AccountAuthorizationService authorization;
    private final BidRepository bids;
    private final FrontendSupportService support;
    private final JdbcTemplate jdbc;
    private final EntityManager em;
    ProposalController(ActiveAccountContext account, AccountAuthorizationService authorization, BidRepository bids,
            FrontendSupportService support, JdbcTemplate jdbc, EntityManager em) {
        this.account=account;this.authorization=authorization;this.bids=bids;this.support=support;this.jdbc=jdbc;this.em=em;
    }
    @GetMapping
    List<FrontendDtos.BidSummary> mine() { return support.myBids(); }
    record EditProposal(@NotNull @Positive @Digits(integer=10,fraction=2) BigDecimal amount, @Size(max=2000) String message) {}
    @PutMapping("/{id}")
    ApiDtos.BidResponse edit(@PathVariable Long id,@Valid @RequestBody EditProposal input) {
        Bid bid=owned(id);
        changed(jdbc.update("UPDATE bids SET amount=?,message=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND tradesperson_id=? AND status='SUBMITTED'",
                input.amount(),input.message(),id,bid.getTradesperson().getId()));
        em.refresh(bid);return ApiDtos.BidResponse.from(bid);
    }
    @PostMapping("/{id}/withdraw")
    ApiDtos.BidResponse withdraw(@PathVariable Long id) {
        Bid bid=owned(id);
        bid.withdraw(); // Reuse the domain transition, then persist with a decision-state guard.
        em.detach(bid); // An ORM flush must not bypass the conditional update.
        changed(jdbc.update("UPDATE bids SET status='WITHDRAWN',updated_at=CURRENT_TIMESTAMP WHERE id=? AND tradesperson_id=? AND status='SUBMITTED'",id,bid.getTradesperson().getId()));
        return ApiDtos.BidResponse.from(bids.findById(id).orElseThrow(ResourceNotFoundException::new));
    }
    private Bid owned(Long id) {
        if(!(account.activeProfile() instanceof Tradesperson actor)) throw new SecurityException("The active tradesperson profile is required");
        authorization.requireActive(actor);
        Bid bid=bids.findById(id).orElseThrow(ResourceNotFoundException::new);
        if(!Objects.equals(actor.getId(),bid.getTradesperson().getId())) throw new SecurityException("Only the bidder may change this proposal");
        authorization.requireDifferentUsers(actor.getUser(),bid.getTask().getProject().getHomeowner().getUser());
        return bid;
    }
    private void changed(int count) { if(count!=1) throw new IllegalStateException("This proposal is no longer submitted and cannot be changed"); }
}
