package com.rockandhardplaces.planning;

import java.util.List;

public record ValidatedProjectPlan(ProjectPlanningAiResponse plan,
        List<RecognizedTrade> recognizedTrades, List<String> unresolvedTrades) {
    public record RecognizedTrade(String suggestion, Long tradeId, String tradeName) {}
}
