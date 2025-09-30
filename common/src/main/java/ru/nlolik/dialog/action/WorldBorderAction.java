package ru.nlolik.dialog.action;

import net.minecraft.world.level.border.WorldBorder;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class WorldBorderAction implements DialogAction {
    private final String size;
    private final String warningTime;

    public WorldBorderAction(String size, String warningTime) {
        this.size = size;
        this.warningTime = warningTime;
    }

    @Override
    public void execute(DialogContext context) {
        WorldBorder border = context.player().serverLevel().getWorldBorder();
        if (size != null) {
            try {
                double newSize = Double.parseDouble(PlaceholderEngine.resolve(size, context));
                border.setSize(newSize);
            } catch (NumberFormatException ignored) {
            }
        }
        if (warningTime != null) {
            try {
                int time = Integer.parseInt(PlaceholderEngine.resolve(warningTime, context));
                border.setWarningTime(time);
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
