package ru.nlolik.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import ru.nlolik.ChatDialogs;
import ru.nlolik.dialog.DialogManager;

public final class ChatDialogsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ChatDialogs.init();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> ChatDialogs.attachServer(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ChatDialogs.detachServer());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ChatDialogs.registerCommands(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(server -> ChatDialogs.tickServer());

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                DialogManager.events().handleBlockBreak(serverPlayer, state, pos);
            }
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed) -> {
            if (entity instanceof ServerPlayer serverPlayer) {
                DialogManager.events().handleEntityDeath(serverPlayer, killed);
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                DialogManager.events().handleBlockInteract(serverPlayer, hitResult.getBlockPos(), hand);
            }
            return InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                DialogManager.events().handleItemUse(serverPlayer, stack);
            }
            return InteractionResultHolder.pass(stack);
        });
    }
}
