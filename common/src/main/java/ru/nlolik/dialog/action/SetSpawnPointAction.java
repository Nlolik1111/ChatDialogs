package ru.nlolik.dialog.action;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class SetSpawnPointAction implements DialogAction {
    private final String dimension;
    private final String x;
    private final String y;
    private final String z;

    public SetSpawnPointAction(String dimension, String x, String y, String z) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void execute(DialogContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        if (dimension != null && !dimension.isBlank()) {
            ResourceLocation id = ResourceLocation.tryParse(PlaceholderEngine.resolve(dimension, context));
            if (id != null) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
                ServerLevel target = context.server().getLevel(key);
                if (target != null) {
                    level = target;
                }
            }
        }
        int px = parse(x, (int) player.getX(), context);
        int py = parse(y, (int) player.getY(), context);
        int pz = parse(z, (int) player.getZ(), context);
        player.setRespawnPosition(level.dimension(), new net.minecraft.core.BlockPos(px, py, pz), player.getYRot(), true, false);
    }

    private int parse(String value, int fallback, DialogContext context) {
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
