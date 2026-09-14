package com.rockandhardplaces.planning;

import java.util.*;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.catalog.TradeRepository;

@Service
public class ProjectPlanningService {
    private final ProjectPlanningAiClient ai;
    private final TradeRepository trades;
    private final AccountAuthorizationService authorization;
    private final PlanningResponseValidator validator;

    public ProjectPlanningService(ProjectPlanningAiClient ai, TradeRepository trades,
            AccountAuthorizationService authorization, PlanningResponseValidator validator) {
        this.ai = ai; this.trades = trades;
        this.authorization = authorization; this.validator = validator;
    }

    public ValidatedProjectPlan plan(Homeowner actor, String idea) {
        authorization.requireActive(actor);
        if (idea == null || idea.isBlank() || idea.length() > 8000)
            throw new IllegalArgumentException("Idea must contain between 1 and 8000 characters");
        // Snapshot scalar catalog values before the network call; no write repositories or transaction.
        var catalog = trades.findAll().stream()
                .map(t -> new ValidatedProjectPlan.RecognizedTrade(t.getName(), t.getId(), t.getName()))
                .toList();
        var response = validator.validate(ai.plan(new ProjectPlanningPrompt(idea.strip(),
                catalog.stream().map(ValidatedProjectPlan.RecognizedTrade::tradeName).toList())));
        var recognized = new ArrayList<ValidatedProjectPlan.RecognizedTrade>();
        var unresolved = new ArrayList<String>();
        Stream.concat(response.suggestedTrades().stream().map(ProjectPlanningAiResponse.SuggestedTrade::trade),
                response.tasks().stream().map(ProjectPlanningAiResponse.SuggestedTask::trade))
                .distinct().forEach(suggestion -> {
                    var matches = catalog.stream()
                            .filter(t -> normalize(t.tradeName()).equals(normalize(suggestion))).toList();
                    if (matches.size() == 1) {
                        var match = matches.get(0);
                        recognized.add(new ValidatedProjectPlan.RecognizedTrade(
                                suggestion, match.tradeId(), match.tradeName()));
                    } else unresolved.add(suggestion); // Ambiguous normalized names also need review.
                });
        return new ValidatedProjectPlan(response, List.copyOf(recognized), List.copyOf(unresolved));
    }

    private String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
