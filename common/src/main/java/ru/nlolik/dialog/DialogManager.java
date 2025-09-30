package ru.nlolik.dialog;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import ru.nlolik.ChatDialogs;
import ru.nlolik.dialog.config.DialogDefinition;
import ru.nlolik.dialog.config.DialogFile;
import ru.nlolik.dialog.event.DialogEventManager;
import ru.nlolik.dialog.runtime.DialogRuntime;
import ru.nlolik.dialog.runtime.DialogScheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class DialogManager {
    private static final Path CONFIG_ROOT = Paths.get("config", "chatdialogs", "dialogs");
    private static final String SAMPLE_FILE_NAME = "example.json";
    private static final String SAMPLE_FILE_CONTENT = """
            {
              \"id\": \"example\",
              \"name\": \"Sample Dialog\",
              \"nodes\": {
                \"start\": {
                  \"lines\": [
                    \"&6Hello, {player_name}!\"
                  ],
                  \"buttons\": [
                    {
                      \"id\": \"continue\",
                      \"text\": \"&aContinue\",
                      \"close\": true
                    }
                  ]
                }
              }
            }
            """;
    private static final Map<String, DialogDefinition> DEFINITIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, DialogRuntime> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<String, String> LOOKUP = new ConcurrentHashMap<>();
    private static final DialogScheduler SCHEDULER = new DialogScheduler();
    private static final DialogEventManager EVENT_MANAGER = new DialogEventManager();
    private static volatile List<String> SUGGESTIONS = List.of();
    private static MinecraftServer server;

    private DialogManager() {
    }

    public static synchronized void attachServer(MinecraftServer minecraftServer) {
        server = minecraftServer;
        SCHEDULER.attach(server);
    }

    public static synchronized void detachServer() {
        ACTIVE.values().forEach(DialogRuntime::stop);
        ACTIVE.clear();
        SCHEDULER.detach();
        EVENT_MANAGER.clear();
        server = null;
    }

    public static DialogScheduler scheduler() {
        return SCHEDULER;
    }

    public static void tick() {
        SCHEDULER.tick();
    }

    public static DialogEventManager events() {
        return EVENT_MANAGER;
    }

    public static MinecraftServer server() {
        return server;
    }

    public static RandomSource random() {
        if (server != null) {
            ServerLevel level = server.overworld();
            if (level != null) {
                return level.getRandom();
            }
        }
        return RandomSource.create();
    }

    public static void reload() {
        ensureFolders();
        List<DialogDefinition> loaded = new ArrayList<>();
        if (Files.exists(CONFIG_ROOT)) {
            try {
                Files.walk(CONFIG_ROOT)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> loaded.addAll(readFile(path)));
            } catch (IOException e) {
                ChatDialogs.LOGGER.error("Failed to enumerate dialog files", e);
            }
        }
        Map<String, DialogDefinition> byId = new HashMap<>();
        loaded.forEach(def -> byId.put(def.id(), def));
        Map<String, Integer> aliasCounts = new HashMap<>();
        for (DialogDefinition definition : loaded) {
            aliasCounts.merge(definition.sourceName(), 1, Integer::sum);
        }
        Map<String, String> aliasMap = new LinkedHashMap<>();
        for (DialogDefinition definition : loaded) {
            String alias = definition.sourceName();
            int count = aliasCounts.getOrDefault(alias, 1);
            String suggestion = alias;
            if (count > 1) {
                suggestion = alias + ":" + definition.id();
            }
            String unique = suggestion;
            int index = 2;
            while (aliasMap.containsKey(unique)) {
                unique = suggestion + "_" + index++;
            }
            aliasMap.put(unique, definition.id());
        }
        DEFINITIONS.clear();
        DEFINITIONS.putAll(byId);
        LOOKUP.clear();
        LOOKUP.putAll(aliasMap);
        for (DialogDefinition definition : loaded) {
            LOOKUP.put(definition.id(), definition.id());
        }
        SUGGESTIONS = aliasMap.keySet().stream()
                .sorted()
                .toList();
        EVENT_MANAGER.updateTriggers(new ArrayList<>(DEFINITIONS.values()));
        ChatDialogs.LOGGER.info("Loaded {} dialog definitions", DEFINITIONS.size());
    }

    private static List<DialogDefinition> readFile(Path path) {
        List<DialogDefinition> list = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            DialogFile file = DialogFile.parse(element, path.getFileName().toString());
            list.addAll(file.dialogs());
        } catch (IOException | JsonIOException | JsonSyntaxException e) {
            ChatDialogs.LOGGER.error("Failed to read dialog file {}", path, e);
        }
        return list;
    }

    private static void ensureFolders() {
        try {
            if (Files.notExists(CONFIG_ROOT)) {
                Files.createDirectories(CONFIG_ROOT);
            }
        } catch (IOException e) {
            ChatDialogs.LOGGER.error("Failed to create dialog directory", e);
            return;
        }
        normaliseDialogFiles();
        createSampleDialog();
    }

    private static void normaliseDialogFiles() {
        if (!Files.isDirectory(CONFIG_ROOT)) {
            return;
        }
        try (Stream<Path> stream = Files.list(CONFIG_ROOT)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        int dot = name.lastIndexOf('.');
                        return dot >= 0 && name.substring(dot).equals(".Json");
                    })
                    .forEach(DialogManager::renameToLowercaseJson);
        } catch (IOException e) {
            ChatDialogs.LOGGER.warn("Failed to normalise dialog file extensions", e);
        }
    }

    private static void renameToLowercaseJson(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return;
        }
        String lowerName = name.substring(0, dot) + ".json";
        Path target = path.resolveSibling(lowerName);
        if (Files.exists(target)) {
            ChatDialogs.LOGGER.warn("Skipping rename of {} because {} already exists", path, target);
            return;
        }
        try {
            Files.move(path, target);
        } catch (IOException e) {
            ChatDialogs.LOGGER.warn("Failed to rename {} to lowercase extension", path, e);
        }
    }

    private static void createSampleDialog() {
        Path sample = CONFIG_ROOT.resolve(SAMPLE_FILE_NAME);
        if (Files.exists(sample)) {
            return;
        }
        try {
            Files.writeString(sample, SAMPLE_FILE_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            ChatDialogs.LOGGER.warn("Failed to create sample dialog file", e);
        }
    }

    public static boolean startDialog(String dialogId, ServerPlayer player, String sessionId) {
        return startDialog(dialogId, player, sessionId, null);
    }

    public static boolean startDialog(String dialogId, ServerPlayer player, String sessionId, String startNode) {
        DialogDefinition definition = findDefinition(dialogId);
        if (definition == null) {
            ChatDialogs.LOGGER.warn("Dialog '{}' not found", dialogId);
            return false;
        }
        stopDialog(player.getUUID());
        DialogRuntime runtime = new DialogRuntime(sessionId, definition, player, SCHEDULER, startNode);
        ACTIVE.put(player.getUUID(), runtime);
        runtime.start();
        ChatDialogs.LOGGER.info("Starting dialog '{}' for {}", definition.id(), player.getGameProfile().getName());
        return true;
    }

    public static boolean stopDialog(UUID playerId) {
        DialogRuntime runtime = ACTIVE.get(playerId);
        if (runtime != null) {
            runtime.stop();
            ChatDialogs.LOGGER.info("Stopped dialog '{}' for {}", runtime.definition().id(), runtime.player().getGameProfile().getName());
            return true;
        }
        return false;
    }

    public static DialogRuntime getRuntime(ServerPlayer player) {
        return ACTIVE.get(player.getUUID());
    }

    public static void removeRuntime(DialogRuntime runtime) {
        ACTIVE.remove(runtime.player().getUUID(), runtime);
    }

    public static Map<String, DialogDefinition> definitions() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }

    public static List<String> dialogSuggestions() {
        return SUGGESTIONS;
    }

    public static DialogDefinition findDefinition(String key) {
        DialogDefinition direct = DEFINITIONS.get(key);
        if (direct != null) {
            return direct;
        }
        String mappedId = LOOKUP.get(key);
        if (mappedId != null) {
            return DEFINITIONS.get(mappedId);
        }
        return null;
    }

    public static void triggerCustomEvent(String name, ServerPlayer player, Map<String, Object> data) {
        EVENT_MANAGER.triggerCustom(name, player, data);
    }
}
