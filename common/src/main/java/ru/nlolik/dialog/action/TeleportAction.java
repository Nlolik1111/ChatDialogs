package ru.nlolik.dialog.action;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class TeleportAction implements DialogAction {
    private final String dimension;
    private final String x;
    private final String y;
    private final String z;

    public TeleportAction(String dimension, String x, String y, String z) {
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
        double px = parseCoordinate(x, player.getX(), context);
        double py = parseCoordinate(y, player.getY(), context);
        double pz = parseCoordinate(z, player.getZ(), context);
        if (player.level() != level) {
            player.teleportTo(level, px, py, pz, player.getYRot(), player.getXRot());
        } else {
            player.teleportTo(px, py, pz);
        }
    }

    private double parseCoordinate(String value, double current, DialogContext context) {
        if (value == null || value.isBlank()) {
            return current;
        }
        String resolved = PlaceholderEngine.resolve(value, context);
        if (resolved.startsWith("~")) {
            double offset = resolved.length() > 1 ? parseNumber(resolved.substring(1), 0.0) : 0.0;
            return current + offset;
        }
        return parseNumber(resolved, current);
    }

    private double parseNumber(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
