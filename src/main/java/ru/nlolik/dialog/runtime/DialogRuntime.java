package ru.nlolik.dialog.runtime;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import ru.nlolik.Main;
import ru.nlolik.dialog.DialogManager;
import ru.nlolik.dialog.action.DialogAction;
import ru.nlolik.dialog.config.ConditionalBranch;
import ru.nlolik.dialog.config.DialogButton;
import ru.nlolik.dialog.config.DialogDefinition;
import ru.nlolik.dialog.config.DialogLine;
import ru.nlolik.dialog.config.DialogNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DialogRuntime {
    private final String sessionId;
    private final DialogDefinition definition;
    private final ServerPlayer player;
    private final DialogScheduler scheduler;
    private final String initialNode;
    private final Map<String, DialogButton> buttonsById = new HashMap<>();
    private final Map<String, String> tokenToButton = new ConcurrentHashMap<>();
    private final Set<String> usedButtons = new HashSet<>();
    private boolean active = true;
    private String currentNodeId;

    public DialogRuntime(String sessionId, DialogDefinition definition, ServerPlayer player, DialogScheduler scheduler) {
        this(sessionId, definition, player, scheduler, definition.startNode());
    }

    public DialogRuntime(String sessionId, DialogDefinition definition, ServerPlayer player, DialogScheduler scheduler, String initialNode) {
        this.sessionId = sessionId;
        this.definition = definition;
        this.player = player;
        this.scheduler = scheduler;
        this.initialNode = initialNode == null ? definition.startNode() : initialNode;
    }

    public void start() {
        broadcast(dialogMessage("message.nwutils.dialog.started", definition.displayName()));
        if (definition.initialDelay() > 0) {
            scheduler.schedule(definition.initialDelay(), () -> enterNode(initialNode));
        } else {
            enterNode(initialNode);
        }
    }

    public void stop() {
        if (!active) {
            return;
        }
        active = false;
        broadcast(dialogMessage("message.nwutils.dialog.stopped", definition.displayName()));
        DialogManager.removeRuntime(this);
    }

    public void choose(String token) {
        if (!active) {
            return;
        }
        String buttonId = tokenToButton.get(token);
        if (buttonId == null) {
            player.sendSystemMessage(dialogMessage("message.nwutils.dialog.button.inactive"));
            return;
        }
        DialogButton button = buttonsById.get(buttonId);
        if (button == null || usedButtons.contains(button.id())) {
            player.sendSystemMessage(dialogMessage("message.nwutils.dialog.button.used"));
            return;
        }
        DialogContext context = context(Map.of());
        boolean allowed = button.conditions().stream().allMatch(condition -> condition.test(context));
        if (!allowed) {
            player.sendSystemMessage(dialogMessage("message.nwutils.dialog.button.conditions"));
            return;
        }
        tokenToButton.remove(token);
        usedButtons.add(button.id());
        if (button.delayTicks() > 0) {
            scheduler.schedule(button.delayTicks(), () -> executeButton(button));
        } else {
            executeButton(button);
        }
    }

    public ServerPlayer player() {
        return player;
    }

    public DialogScheduler scheduler() {
        return scheduler;
    }

    public DialogDefinition definition() {
        return definition;
    }

    private void executeButton(DialogButton button) {
        if (!active) {
            return;
        }
        DialogContext context = context(Map.of());
        for (DialogAction action : button.actions()) {
            try {
                action.execute(context);
            } catch (Exception e) {
                Main.LOGGER.error("Failed to execute button action", e);
            }
        }
        if (button.closesDialog()) {
            closeWithNodeDelay();
            return;
        }
        if (button.nextNode() != null && !button.nextNode().isBlank()) {
            enterNode(button.nextNode());
        }
    }

    private void enterNode(String nodeId) {
        if (!active) {
            return;
        }
        DialogNode node;
        try {
            node = definition.node(nodeId);
        } catch (IllegalArgumentException e) {
            Main.LOGGER.warn("Missing dialog node {} in dialog {}", nodeId, definition.id());
            stop();
            return;
        }
        currentNodeId = node.id();
        usedButtons.clear();
        buttonsById.clear();
        tokenToButton.clear();
        DialogContext context = context(Map.of());
        for (DialogAction action : node.entryActions()) {
            try {
                action.execute(context);
            } catch (Exception e) {
                Main.LOGGER.error("Failed to execute node action", e);
            }
        }
        if (processBranches(node, context)) {
            return;
        }
        int delay = node.startDelay();
        for (DialogLine line : node.lines()) {
            int scheduledDelay = delay;
            scheduler.schedule(scheduledDelay, () -> sendLine(line));
            delay += Math.max(1, line.delayTicks());
            if (line.loop() != null) {
                scheduleLoop(line, scheduledDelay + line.loop().intervalTicks(), line.loop());
            }
        }
        if (!node.buttons().isEmpty()) {
            DialogNode finalNode = node;
            scheduler.schedule(delay, () -> sendButtons(finalNode));
        }
        if (node.autoNext() != null) {
            scheduler.schedule(delay + node.autoNextDelay(), () -> enterNode(node.autoNext()));
        }
        if (node.stopDelayTicks() > 0) {
            scheduler.schedule(delay + node.stopDelayTicks(), this::stop);
        } else if (node.closeOnFinish()) {
            scheduler.schedule(delay, this::stop);
        }
    }

    private boolean processBranches(DialogNode node, DialogContext context) {
        for (ConditionalBranch branch : node.branches()) {
            boolean result = branch.type() == ConditionalBranch.Type.ELSE || branch.condition().test(context);
            if (result) {
                for (DialogAction action : branch.actions()) {
                    try {
                        action.execute(context);
                    } catch (Exception e) {
                        Main.LOGGER.error("Failed to execute branch action", e);
                    }
                }
                if (branch.nextNode() != null) {
                    scheduler.schedule(1, () -> enterNode(branch.nextNode()));
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private void sendLine(DialogLine line) {
        if (!active) {
            return;
        }
        DialogContext context = context(Map.of());
        String text = PlaceholderEngine.resolve(line.text(), context);
        Component component = TextRenderer.render(text, line.style());
        player.sendSystemMessage(component);
    }

    private void scheduleLoop(DialogLine line, int initialDelay, ru.nlolik.dialog.config.LoopSettings loop) {
        if (loop.times() == 0) {
            return;
        }
        int iterations = loop.infinite() ? Integer.MAX_VALUE : Math.max(0, loop.times() - 1);
        for (int i = 0; i < iterations; i++) {
            int delay = initialDelay + (loop.intervalTicks() * i);
            scheduler.schedule(delay, () -> sendLine(line));
        }
    }

    private void sendButtons(DialogNode node) {
        if (!active || !node.id().equals(currentNodeId)) {
            return;
        }
        DialogContext context = context(Map.of());
        List<Component> renderedButtons = new ArrayList<>();
        for (DialogButton button : node.buttons()) {
            boolean allow = button.conditions().stream().allMatch(condition -> condition.test(context));
            if (!allow) {
                continue;
            }
            buttonsById.put(button.id(), button);
            String token = UUID.randomUUID().toString();
            tokenToButton.put(token, button.id());
            String resolvedText = PlaceholderEngine.resolve(button.text(), context);
            MutableComponent label = TextRenderer.render(resolvedText, button.style()).copy();
            MutableComponent clickable = Component.empty()
                    .append(Component.literal("[ ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(label)
                    .append(Component.literal(" ]").withStyle(ChatFormatting.DARK_GRAY));
            clickable.withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dialog choose " + token))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("message.nwutils.dialog.button.hover")))
                    .withUnderlined(true));
            renderedButtons.add(clickable);
        }
        if (renderedButtons.isEmpty()) {
            return;
        }
        if (renderedButtons.size() <= 3) {
            MutableComponent row = Component.empty();
            for (int i = 0; i < renderedButtons.size(); i++) {
                if (i > 0) {
                    row.append(Component.literal("  "));
                }
                row.append(renderedButtons.get(i));
            }
            player.sendSystemMessage(row);
        } else {
            renderedButtons.forEach(player::sendSystemMessage);
        }
    }

    private DialogContext context(Map<String, Object> data) {
        return new DialogContext(this, definition, player, player.getServer(), data);
    }

    private Component dialogMessage(String translationKey, Object... args) {
        MutableComponent prefix = Component.translatable("message.nwutils.dialog.prefix").withStyle(ChatFormatting.GOLD);
        MutableComponent body = Component.translatable(translationKey, args).withStyle(ChatFormatting.GRAY);
        return Component.empty()
                .append(prefix)
                .append(Component.literal(" "))
                .append(body);
    }

    private void broadcast(Component message) {
        player.sendSystemMessage(message);
        if (DialogManager.server() == null) {
            return;
        }
        for (ServerPlayer other : DialogManager.server().getPlayerList().getPlayers()) {
            if (other == player) {
                continue;
            }
            other.sendSystemMessage(message);
        }
    }

    private void closeWithNodeDelay() {
        DialogNode node;
        try {
            node = definition.node(currentNodeId);
        } catch (IllegalArgumentException ignored) {
            stop();
            return;
        }
        if (node.stopDelayTicks() > 0) {
            scheduler.schedule(node.stopDelayTicks(), this::stop);
        } else {
            stop();
        }
    }
}
