package ru.nlolik.dialog.action;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.nlolik.ChatDialogs;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class TakeItemAction implements DialogAction {
    private final String itemId;
    private final int count;

    public TakeItemAction(String itemId, int count) {
        this.itemId = itemId;
        this.count = Math.max(1, count);
    }

    @Override
    public void execute(DialogContext context) {
        String resolved = PlaceholderEngine.resolve(itemId, context);
        ResourceLocation location = ResourceLocation.tryParse(resolved);
        if (location == null) {
            ChatDialogs.LOGGER.warn("Invalid item id '{}' in take_item action", resolved);
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(location);
        if (item == null) {
            return;
        }
        ServerPlayer player = context.player();
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                int remove = Math.min(remaining, stack.getCount());
                stack.shrink(remove);
                remaining -= remove;
                if (remaining <= 0) {
                    break;
                }
            }
        }
        player.containerMenu.broadcastChanges();
    }
}
