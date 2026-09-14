package com.rockandhardplaces.planning;

import com.rockandhardplaces.account.*;
import com.rockandhardplaces.catalog.TradeRepository;
import com.rockandhardplaces.project.ProjectWorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ApprovedProjectCreationService {
    private final ProjectWorkflowService workflow;
    private final AccountAuthorizationService authorization;
    private final TradeRepository trades;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ObjectMapper mapper;
    private final Validator validator;

    public ApprovedProjectCreationService(ProjectWorkflowService workflow, AccountAuthorizationService authorization,
            TradeRepository trades, JdbcTemplate jdbc, PlatformTransactionManager manager,
            ObjectMapper mapper, Validator validator) {
        this.workflow = workflow; this.authorization = authorization; this.trades = trades;
        this.jdbc = jdbc; this.transaction = new TransactionTemplate(manager);
        this.mapper = mapper; this.validator = validator;
    }

    // The small SQLite application serializes this workflow through commit. The durable
    // owner/key constraint also rolls back competing processes rather than duplicating work.
    public synchronized long create(Homeowner actor, String submissionKey, ApprovedProjectDraft draft) {
        authorization.requireActive(actor);
        if (submissionKey == null || !submissionKey.equals(UUID.fromString(submissionKey).toString()))
            throw new IllegalArgumentException("A valid submission key is required");
        if (draft == null || !validator.validate(draft).isEmpty())
            throw new IllegalArgumentException("Please complete the approved project details");
        final String fingerprint;
        try {
            fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(mapper.writeValueAsBytes(draft)));
        } catch (Exception exception) { throw new IllegalStateException("Could not prepare project creation"); }
        return transaction.execute(status -> {
            var receipts = jdbc.queryForList("SELECT project_id, fingerprint FROM project_creation_receipts WHERE homeowner_id = ? AND submission_key = ?",
                    actor.getId(), submissionKey);
            if (!receipts.isEmpty()) {
                var receipt = receipts.get(0);
                if (!fingerprint.equals(receipt.get("fingerprint")))
                    throw new IllegalStateException("This submission already created a different draft. Open My Projects to review it.");
                return ((Number) receipt.get("project_id")).longValue();
            }
            // Validate the entire catalog relationship set before starting writes.
            draft.tasks().stream().flatMap(task -> task.requiredTradeIds().stream()).distinct().forEach(id -> {
                if (!trades.existsById(id)) throw new IllegalArgumentException("Choose an existing trade for every approved task");
            });
            var project = workflow.create(actor, draft.title(), draft.description(), draft.jobZip());
            for (var task : draft.tasks())
                workflow.createTask(actor, project, task.title(), task.description(), null, task.requiredTradeIds());
            jdbc.update("INSERT INTO project_creation_receipts(homeowner_id, submission_key, fingerprint, project_id) VALUES (?, ?, ?, ?)",
                    actor.getId(), submissionKey, fingerprint, project.getId());
            return project.getId();
        });
    }
}
