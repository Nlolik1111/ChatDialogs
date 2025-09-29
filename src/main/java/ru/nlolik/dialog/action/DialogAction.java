package ru.nlolik.dialog.action;

import ru.nlolik.dialog.runtime.DialogContext;

@FunctionalInterface
public interface DialogAction {
    void execute(DialogContext context) throws Exception;
}
