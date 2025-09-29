package ru.nlolik.dialog.condition;

import ru.nlolik.dialog.runtime.DialogContext;

@FunctionalInterface
public interface DialogCondition {
    boolean test(DialogContext context);

    DialogCondition TRUE = context -> true;
}
