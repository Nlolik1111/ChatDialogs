package ru.nlolik.dialog.action;

import net.minecraft.core.BlockPos;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class RemoveBlockAction implements DialogAction {
    private final String x;
    private final String y;
    private final String z;

    public RemoveBlockAction(String x, String y, String z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void execute(DialogContext context) {
        BlockPos pos = new BlockPos(parseCoordinate(x, context), parseCoordinate(y, context), parseCoordinate(z, context));
        context.player().level().removeBlock(pos, false);
    }

    private int parseCoordinate(String value, DialogContext context) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(PlaceholderEngine.resolve(value, context));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
