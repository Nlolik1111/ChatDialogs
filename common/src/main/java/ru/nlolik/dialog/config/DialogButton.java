package ru.nlolik.dialog.config;

import ru.nlolik.dialog.action.DialogAction;
import ru.nlolik.dialog.condition.DialogCondition;

import java.util.List;

public class DialogButton {
    private final String id;
    private final String text;
    private final DialogTextStyle style;
    private final List<DialogAction> actions;
    private final String nextNode;
    private final List<DialogCondition> conditions;
    private final boolean closesDialog;
    private final int delayTicks;

    public DialogButton(String id,
                        String text,
                        DialogTextStyle style,
                        List<DialogAction> actions,
                        String nextNode,
                        List<DialogCondition> conditions,
                        boolean closesDialog,
                        int delayTicks) {
        this.id = id;
        this.text = text;
        this.style = style;
        this.actions = List.copyOf(actions);
        this.nextNode = nextNode;
        this.conditions = List.copyOf(conditions);
        this.closesDialog = closesDialog;
        this.delayTicks = Math.max(0, delayTicks);
    }

    public String id() {
        return id;
    }

    public String text() {
        return text;
    }

    public DialogTextStyle style() {
        return style;
    }

    public List<DialogAction> actions() {
        return actions;
    }

    public String nextNode() {
        return nextNode;
    }

    public List<DialogCondition> conditions() {
        return conditions;
    }

    public boolean closesDialog() {
        return closesDialog;
    }

    public int delayTicks() {
        return delayTicks;
    }
}
