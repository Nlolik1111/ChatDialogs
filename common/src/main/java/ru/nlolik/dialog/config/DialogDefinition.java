package ru.nlolik.dialog.config;

import ru.nlolik.dialog.event.DialogEventTrigger;

import java.util.List;
import java.util.Map;

public record DialogDefinition(
        String id,
        String displayName,
        String sourceName,
        String startNode,
        int initialDelay,
        Map<String, DialogNode> nodes,
        List<DialogEventTrigger> triggers) {

    public DialogNode node(String nodeId) {
        DialogNode node = nodes.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Dialog node '" + nodeId + "' not found in dialog " + id);
        }
        return node;
    }
}
