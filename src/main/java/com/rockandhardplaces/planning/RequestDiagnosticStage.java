package com.rockandhardplaces.planning;

import com.fasterxml.jackson.databind.JsonNode;

/** Each transition changes a real difference; L is an exact normal-path control. */
enum RequestDiagnosticStage {
    H, I, J, K, L;

    JsonNode schema(JsonNode normalSchema) {
        JsonNode result = normalSchema.deepCopy();
        if (ordinal() < K.ordinal()) removeDescriptions(result, 0);
        return result;
    }

    private void removeDescriptions(JsonNode node, int objectDepth) {
        // H/I omit annotations. J restores only summary and root string-array annotations.
        if (this != J || objectDepth > 1)
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("description");
        node.path("properties").forEach(child -> removeDescriptions(child, objectDepth + 1));
        if (node.has("items")) removeDescriptions(node.get("items"), objectDepth);
    }
}
