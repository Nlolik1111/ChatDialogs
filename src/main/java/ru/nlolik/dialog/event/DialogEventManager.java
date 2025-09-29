package ru.nlolik.dialog.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;
import ru.nlolik.Main;
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
    private boolean registered;

    public void attach() {
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(this);
            registered = true;
        }
    }

    public void detach() {
        if (registered) {
            MinecraftForge.EVENT_BUS.unregister(this);
            registered = false;
        }
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

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("event_name", "on_block_break");
        data.put("block", ForgeRegistries.BLOCKS.getKey(event.getState().getBlock()).toString());
        data.put("x", event.getPos().getX());
        data.put("y", event.getPos().getY());
        data.put("z", event.getPos().getZ());
        fire(DialogEventType.ON_BLOCK_BREAK, player, data);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onEntityDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("event_name", "on_entity_death");
        data.put("entity", event.getEntity().getType().builtInRegistryHolder().key().location().toString());
        fire(DialogEventType.ON_ENTITY_DEATH, player, data);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("event_name", "on_player_interact");
        data.put("hand", event.getHand().name());
        data.put("x", event.getPos().getX());
        data.put("y", event.getPos().getY());
        data.put("z", event.getPos().getZ());
        fire(DialogEventType.ON_PLAYER_INTERACT, player, data);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("event_name", "on_item_use");
        ItemStack stack = event.getItemStack();
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
                    Main.LOGGER.error("Failed to execute trigger action", e);
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
