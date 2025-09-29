package ru.nlolik.dialog.config;

import ru.nlolik.dialog.action.DialogAction;

import java.util.List;

public class DialogNode {
    private final String id;
    private final List<DialogLine> lines;
    private final List<DialogAction> entryActions;
    private final List<DialogButton> buttons;
    private final List<ConditionalBranch> branches;
    private final int startDelay;
    private final String autoNext;
    private final int autoNextDelay;
    private final boolean closeOnFinish;
    private final int stopDelayTicks;

    public DialogNode(String id,
                      List<DialogLine> lines,
                      List<DialogAction> entryActions,
                      List<DialogButton> buttons,
                      List<ConditionalBranch> branches,
                      int startDelay,
                      String autoNext,
                      int autoNextDelay,
                      boolean closeOnFinish,
                      int stopDelayTicks) {
        this.id = id;
        this.lines = List.copyOf(lines);
        this.entryActions = List.copyOf(entryActions);
        this.buttons = List.copyOf(buttons);
        this.branches = List.copyOf(branches);
        this.startDelay = Math.max(0, startDelay);
        this.autoNext = autoNext;
        this.autoNextDelay = Math.max(0, autoNextDelay);
        this.closeOnFinish = closeOnFinish;
        this.stopDelayTicks = Math.max(0, stopDelayTicks);
    }

    public String id() {
        return id;
    }

    public List<DialogLine> lines() {
        return lines;
    }

    public List<DialogAction> entryActions() {
        return entryActions;
    }

    public List<DialogButton> buttons() {
        return buttons;
    }

    public List<ConditionalBranch> branches() {
        return branches;
    }

    public int startDelay() {
        return startDelay;
    }

    public String autoNext() {
        return autoNext;
    }

    public int autoNextDelay() {
        return autoNextDelay;
    }

    public boolean closeOnFinish() {
        return closeOnFinish;
    }

    public int stopDelayTicks() {
        return stopDelayTicks;
    }
}
