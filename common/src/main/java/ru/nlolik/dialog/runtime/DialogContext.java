package ru.nlolik.dialog.runtime;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.nlolik.dialog.config.DialogDefinition;

import java.util.Collections;
import java.util.Map;

public class DialogContext {
    private final DialogRuntime runtime;
    private final DialogDefinition definition;
    private final ServerPlayer player;
    private final MinecraftServer server;
    private final Map<String, Object> eventData;

    public DialogContext(DialogRuntime runtime,
                         DialogDefinition definition,
                         ServerPlayer player,
                         MinecraftServer server,
                         Map<String, Object> eventData) {
        this.runtime = runtime;
        this.definition = definition;
        this.player = player;
        this.server = server;
        this.eventData = eventData == null ? Map.of() : Map.copyOf(eventData);
    }

    public DialogRuntime runtime() {
        return runtime;
    }

    public DialogDefinition definition() {
        return definition;
    }

    public ServerPlayer player() {
        return player;
    }

    public MinecraftServer server() {
        return server;
    }

    public Map<String, Object> eventData() {
        return Collections.unmodifiableMap(eventData);
    }
}
