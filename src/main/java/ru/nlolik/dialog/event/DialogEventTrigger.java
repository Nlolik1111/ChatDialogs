package ru.nlolik.dialog.event;

import ru.nlolik.dialog.action.DialogAction;
import ru.nlolik.dialog.condition.DialogCondition;

import java.util.List;
import java.util.Map;

public class DialogEventTrigger {
    private final DialogEventType type;
    private final String name;
    private final String dialogId;
    private final String nodeId;
    private final List<DialogCondition> conditions;
    private final List<DialogAction> actions;
    private final Map<String, String> filters;

    public DialogEventTrigger(DialogEventType type,
                              String name,
                              String dialogId,
                              String nodeId,
                              List<DialogCondition> conditions,
                              List<DialogAction> actions,
                              Map<String, String> filters) {
        this.type = type;
        this.name = name;
        this.dialogId = dialogId;
        this.nodeId = nodeId;
        this.conditions = List.copyOf(conditions);
        this.actions = List.copyOf(actions);
        this.filters = Map.copyOf(filters);
    }

    public DialogEventType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public String dialogId() {
        return dialogId;
    }

    public String nodeId() {
        return nodeId;
    }

    public List<DialogCondition> conditions() {
        return conditions;
    }

    public List<DialogAction> actions() {
        return actions;
    }

    public Map<String, String> filters() {
        return filters;
    }
}
