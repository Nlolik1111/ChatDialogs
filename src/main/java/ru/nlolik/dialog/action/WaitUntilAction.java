package ru.nlolik.dialog.action;

import ru.nlolik.dialog.condition.DialogCondition;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.DialogScheduler;

public class WaitUntilAction implements DialogAction {
    private final DialogCondition condition;
    private final int checkInterval;
    private final int timeout;

    public WaitUntilAction(DialogCondition condition, int checkInterval, int timeout) {
        this.condition = condition;
        this.checkInterval = Math.max(1, checkInterval);
        this.timeout = timeout;
    }

    @Override
    public void execute(DialogContext context) {
        DialogScheduler scheduler = context.runtime() != null ? context.runtime().scheduler() : null;
        if (scheduler == null) {
            return;
        }
        scheduler.scheduleRepeating(checkInterval, timeout, () -> condition.test(context), () -> {
        }, null);
    }
}
