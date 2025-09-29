package ru.nlolik.dialog.action;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import ru.nlolik.Main;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class GiveItemAction implements DialogAction {
    private final String itemId;
    private final int count;

    public GiveItemAction(String itemId, int count) {
        this.itemId = itemId;
        this.count = Math.max(1, count);
    }

    @Override
    public void execute(DialogContext context) {
        String resolved = PlaceholderEngine.resolve(itemId, context);
        ResourceLocation location;
        try {
            location = ResourceLocation.parse(resolved);
        } catch (IllegalArgumentException e) {
            Main.LOGGER.warn("Invalid item id '{}' in give_item action", resolved);
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(location);
        if (item == null) {
            return;
        }
        ServerPlayer player = context.player();
        ItemStack stack = new ItemStack(item, count);
        boolean added = player.getInventory().add(stack);
        if (!added) {
            player.drop(stack, false);
        }
        player.containerMenu.broadcastChanges();
    }
}
