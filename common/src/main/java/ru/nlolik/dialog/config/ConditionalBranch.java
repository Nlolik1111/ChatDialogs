package ru.nlolik.dialog.config;

import ru.nlolik.dialog.action.DialogAction;
import ru.nlolik.dialog.condition.DialogCondition;

import java.util.List;

public class ConditionalBranch {
    public enum Type {
        IF,
        ELIF,
        ELSE
    }

    private final Type type;
    private final DialogCondition condition;
    private final List<DialogAction> actions;
    private final String nextNode;

    public ConditionalBranch(Type type, DialogCondition condition, List<DialogAction> actions, String nextNode) {
        this.type = type;
        this.condition = condition;
        this.actions = List.copyOf(actions);
        this.nextNode = nextNode;
    }

    public Type type() {
        return type;
    }

    public DialogCondition condition() {
        return condition;
    }

    public List<DialogAction> actions() {
        return actions;
    }

    public String nextNode() {
        return nextNode;
    }
}
