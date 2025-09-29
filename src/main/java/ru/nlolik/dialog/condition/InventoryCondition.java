package ru.nlolik.dialog.condition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import ru.nlolik.dialog.runtime.DialogContext;

public class InventoryCondition implements DialogCondition {
    private final ResourceLocation itemId;
    private final int count;

    public InventoryCondition(ResourceLocation itemId, int count) {
        this.itemId = itemId;
        this.count = count;
    }

    @Override
    public boolean test(DialogContext context) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return false;
        }
        ServerPlayer player = context.player();
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                found += stack.getCount();
                if (found >= count) {
                    return true;
                }
            }
        }
        return count <= 0;
    }
}
