package ru.nlolik.dialog.action;

import net.minecraft.util.Mth;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class SetHealthAction implements DialogAction {
    private final String value;

    public SetHealthAction(String value) {
        this.value = value;
    }

    @Override
    public void execute(DialogContext context) {
        try {
            float health = Float.parseFloat(PlaceholderEngine.resolve(value, context));
            context.player().setHealth(Mth.clamp(health, 0.0F, context.player().getMaxHealth()));
        } catch (NumberFormatException ignored) {
        }
    }
}
