package ru.nlolik.dialog.action;

import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class SetWorldTimeAction implements DialogAction {
    private final String time;

    public SetWorldTimeAction(String time) {
        this.time = time;
    }

    @Override
    public void execute(DialogContext context) {
        String value = PlaceholderEngine.resolve(time, context);
        try {
            long ticks = Long.parseLong(value);
            context.player().serverLevel().setDayTime(ticks);
        } catch (NumberFormatException ignored) {
        }
    }
}
