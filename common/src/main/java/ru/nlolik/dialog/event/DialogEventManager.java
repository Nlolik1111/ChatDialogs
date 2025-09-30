package ru.nlolik.dialog.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ru.nlolik.ChatDialogs;
import ru.nlolik.dialog.DialogManager;
import ru.nlolik.dialog.action.DialogAction;
import ru.nlolik.dialog.config.DialogDefinition;
import ru.nlolik.dialog.runtime.DialogContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class DialogEventManager {
    private final List<DialogEventTrigger> triggers = new CopyOnWriteArrayList<>();
    public void clear() {
        triggers.clear();
    }

    public void updateTriggers(List<DialogDefinition> definitions) {
        List<DialogEventTrigger> collected = new ArrayList<>();
        for (DialogDefinition definition : definitions) {
            collected.addAll(definition.triggers());
        }
        triggers.clear();
        triggers.addAll(collected);
    }

    public void handleBlockBreak(ServerPlayer player, BlockState state, BlockPos pos) {
        Map<String, Object> data = new HashMap<>();
        data.put("event_name", "on_block_break");
        Block block = state.getBlock();
        data.put("block", BuiltInRegistries.BLOCK.getKey(block).toString());
        data.put("x", pos.getX());
        data.put("y", pos.getY());
        data.put("z", pos.getZ());
        fire(DialogEventType.ON_BLOCK_BREAK, player, data);
    }

    public void handleEntityDeath(ServerPlayer player, Entity entity) {
        Map<String, Object> data = new HashMap<>();
        data.put("event_name", "on_entity_death");
        data.put("entity", entity.getType().builtInRegistryHolder().key().location().toString());
        fire(DialogEventType.ON_ENTITY_DEATH, player, data);
    }

    public void handleBlockInteract(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        Map<String, Object> data = new HashMap<>();
        data.put("event_name", "on_player_interact");
        data.put("hand", hand.name());
        data.put("x", pos.getX());
        data.put("y", pos.getY());
        data.put("z", pos.getZ());
        fire(DialogEventType.ON_PLAYER_INTERACT, player, data);
    }

    public void handleItemUse(ServerPlayer player, ItemStack stack) {
        Map<String, Object> data = new HashMap<>();
        data.put("event_name", "on_item_use");
        data.put("item", stack.getItem().builtInRegistryHolder().key().location().toString());
        fire(DialogEventType.ON_ITEM_USE, player, data);
    }

    public void triggerCustom(String name, ServerPlayer player, Map<String, Object> data) {
        data = data == null ? new HashMap<>() : new HashMap<>(data);
        data.putIfAbsent("event_name", name);
        fire(DialogEventType.CUSTOM, player, data, name);
    }

    private void fire(DialogEventType type, ServerPlayer player, Map<String, Object> data) {
        fire(type, player, data, null);
    }

    private void fire(DialogEventType type, ServerPlayer player, Map<String, Object> data, String customName) {
        for (DialogEventTrigger trigger : triggers) {
            if (trigger.type() != type) {
                continue;
            }
            if (customName != null && trigger.type() == DialogEventType.CUSTOM && !customName.equalsIgnoreCase(trigger.name())) {
                continue;
            }
            if (!matchesFilters(trigger.filters(), data)) {
                continue;
            }
            DialogDefinition definition = trigger.dialogId() == null ? null : DialogManager.definitions().get(trigger.dialogId());
            DialogContext context = new DialogContext(null, definition, player, player.getServer(), data);
            if (trigger.conditions().stream().anyMatch(condition -> !condition.test(context))) {
                continue;
            }
            for (DialogAction action : trigger.actions()) {
                try {
                    action.execute(context);
                } catch (Exception e) {
                    ChatDialogs.LOGGER.error("Failed to execute trigger action", e);
                }
            }
            if (trigger.dialogId() != null && !trigger.dialogId().isBlank()) {
                DialogManager.startDialog(trigger.dialogId(), player, UUID.randomUUID().toString(), trigger.nodeId());
            }
        }
    }

    private boolean matchesFilters(Map<String, String> filters, Map<String, Object> data) {
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            Object value = data.get(entry.getKey());
            if (value == null || !value.toString().equalsIgnoreCase(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}
