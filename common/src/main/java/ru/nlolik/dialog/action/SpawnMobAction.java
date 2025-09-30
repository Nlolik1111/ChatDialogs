package ru.nlolik.dialog.action;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class SpawnMobAction implements DialogAction {
    private final String entityId;
    private final String x;
    private final String y;
    private final String z;

    public SpawnMobAction(String entityId, String x, String y, String z) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void execute(DialogContext context) {
        ServerLevel level = context.player().serverLevel();
        EntityType<?> type = EntityType.byString(PlaceholderEngine.resolve(entityId, context)).orElse(null);
        if (type == null) {
            return;
        }
        BlockPos pos = new BlockPos(parse(x, context, (int) context.player().getX()), parse(y, context, (int) context.player().getY()), parse(z, context, (int) context.player().getZ()));
        type.spawn(level, null, context.player(), pos, MobSpawnType.COMMAND, true, false);
    }

    private int parse(String value, DialogContext context, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(PlaceholderEngine.resolve(value, context));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
