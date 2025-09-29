package ru.nlolik.dialog.action;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import ru.nlolik.Main;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class PlaceBlockAction implements DialogAction {
    private final String blockId;
    private final String x;
    private final String y;
    private final String z;

    public PlaceBlockAction(String blockId, String x, String y, String z) {
        this.blockId = blockId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void execute(DialogContext context) {
        String resolved = PlaceholderEngine.resolve(blockId, context);
        ResourceLocation location;
        try {
            location = ResourceLocation.parse(resolved);
        } catch (IllegalArgumentException e) {
            Main.LOGGER.warn("Invalid block id '{}' in place_block action", resolved);
            return;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(location);
        if (block == null) {
            return;
        }
        BlockPos pos = new BlockPos(parseCoordinate(x, context), parseCoordinate(y, context), parseCoordinate(z, context));
        BlockState state = block.defaultBlockState();
        context.player().level().setBlockAndUpdate(pos, state);
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
