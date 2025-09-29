package ru.nlolik.dialog.action;

import ru.nlolik.dialog.runtime.DialogContext;

public class GiveCurrencyAction extends ScoreAction {
    public GiveCurrencyAction(String value) {
        super("currency", value, Mode.ADD);
    }
}
