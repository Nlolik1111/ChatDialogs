package ru.nlolik.dialog.condition;

import ru.nlolik.dialog.DialogManager;
import ru.nlolik.dialog.runtime.DialogContext;

public class RandomCondition implements DialogCondition {
    private final int chance;

    public RandomCondition(int chance) {
        this.chance = Math.max(0, Math.min(100, chance));
    }

    @Override
    public boolean test(DialogContext context) {
        if (chance >= 100) {
            return true;
        }
        if (chance <= 0) {
            return false;
        }
        return DialogManager.random().nextInt(100) < chance;
    }
}
