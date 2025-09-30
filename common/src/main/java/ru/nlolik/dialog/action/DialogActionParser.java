package ru.nlolik.dialog.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ru.nlolik.dialog.condition.DialogCondition;
import ru.nlolik.dialog.condition.DialogConditionFactory;

import java.util.ArrayList;
import java.util.List;

public final class DialogActionParser {
    private DialogActionParser() {
    }

    public static List<DialogAction> parse(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (element.isJsonArray()) {
            List<DialogAction> actions = new ArrayList<>();
            JsonArray array = element.getAsJsonArray();
            for (JsonElement entry : array) {
                actions.addAll(parse(entry));
            }
            return actions;
        }
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            return List.of(new CommandAction(value, false, false));
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("actions")) {
                return parse(object.get("actions"));
            }
            String type = detectType(object);
            return switch (type) {
                case "command" -> List.of(new CommandAction(object.get("command").getAsString(), object.has("as") && "player".equalsIgnoreCase(object.get("as").getAsString()), object.has("silent") && object.get("silent").getAsBoolean()));
                case "commands" -> parseCommands(object.getAsJsonArray("commands"));
                case "execute" -> List.of(new CommandAction(object.get("execute").getAsString(), true, object.has("silent") && object.get("silent").getAsBoolean()));
                case "mod_command" -> List.of(new CommandAction(object.get("mod_command").getAsString(), false, true));
                case "send_message" -> List.of(new MessageAction(object.get("message").getAsString(), object.has("broadcast") && object.get("broadcast").getAsBoolean()));
                case "give_item" -> List.of(new GiveItemAction(object.get("item").getAsString(), object.has("count") ? object.get("count").getAsInt() : 1));
                case "take_item" -> List.of(new TakeItemAction(object.get("item").getAsString(), object.has("count") ? object.get("count").getAsInt() : 1));
                case "teleport" -> List.of(new TeleportAction(object.has("dimension") ? object.get("dimension").getAsString() : null,
                        object.has("x") ? object.get("x").getAsString() : null,
                        object.has("y") ? object.get("y").getAsString() : null,
                        object.has("z") ? object.get("z").getAsString() : null));
                case "set_world_time" -> {
                    String value = object.has("set_world_time") ? object.get("set_world_time").getAsString() : object.get("value").getAsString();
                    yield List.of(new SetWorldTimeAction(value));
                }
                case "spawn_mob" -> List.of(new SpawnMobAction(object.get("spawn_mob").getAsString(),
                        object.has("x") ? object.get("x").getAsString() : null,
                        object.has("y") ? object.get("y").getAsString() : null,
                        object.has("z") ? object.get("z").getAsString() : null));
                case "world_border" -> List.of(new WorldBorderAction(object.has("world_border") ? object.get("world_border").getAsString() : (object.has("size") ? object.get("size").getAsString() : null),
                        object.has("warning_time") ? object.get("warning_time").getAsString() : null));
                case "place_block" -> {
                    String block = object.has("place_block") ? object.get("place_block").getAsString() : object.get("block").getAsString();
                    yield List.of(new PlaceBlockAction(block,
                            object.has("x") ? object.get("x").getAsString() : null,
                            object.has("y") ? object.get("y").getAsString() : null,
                            object.has("z") ? object.get("z").getAsString() : null));
                }
                case "remove_block" -> {
                    String x = object.has("x") ? object.get("x").getAsString() : (object.has("remove_block") ? object.get("remove_block").getAsString() : null);
                    String y = object.has("y") ? object.get("y").getAsString() : null;
                    String z = object.has("z") ? object.get("z").getAsString() : null;
                    yield List.of(new RemoveBlockAction(x, y, z));
                }
                case "set_health" -> {
                    String value = object.has("set_health") ? object.get("set_health").getAsString() : object.get("value").getAsString();
                    yield List.of(new SetHealthAction(value));
                }
                case "set_score" -> List.of(new ScoreAction(object.get("objective").getAsString(), object.get("value").getAsString(), ScoreAction.Mode.SET));
                case "add_score" -> List.of(new ScoreAction(object.get("objective").getAsString(), object.get("value").getAsString(), ScoreAction.Mode.ADD));
                case "give_currency" -> {
                    String value = object.has("give_currency") ? object.get("give_currency").getAsString() : object.get("value").getAsString();
                    yield List.of(new GiveCurrencyAction(value));
                }
                case "set_spawn_point" -> List.of(new SetSpawnPointAction(object.has("dimension") ? object.get("dimension").getAsString() : null,
                        object.has("x") ? object.get("x").getAsString() : null,
                        object.has("y") ? object.get("y").getAsString() : null,
                        object.has("z") ? object.get("z").getAsString() : null));
                case "trigger_event" -> List.of(new TriggerEventAction(object.get("trigger_event").getAsString()));
                case "complete_quest" -> List.of(new CompleteQuestAction(object.get("complete_quest").getAsString()));
                case "reset_quest" -> List.of(new ResetQuestAction(object.get("reset_quest").getAsString()));
                case "wait_until" -> {
                    DialogCondition condition = DialogConditionFactory.parse(object.get("condition"));
                    int interval = object.has("interval") ? object.get("interval").getAsInt() : 20;
                    int timeout = object.has("timeout") ? object.get("timeout").getAsInt() : -1;
                    yield List.of(new WaitUntilAction(condition, interval, timeout));
                }
                default -> List.of();
            };
        }
        return List.of();
    }

    private static List<DialogAction> parseCommands(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<DialogAction> actions = new ArrayList<>();
        for (JsonElement element : array) {
            actions.addAll(parse(element));
        }
        return actions;
    }

    private static String detectType(JsonObject object) {
        if (object.has("type")) {
            return object.get("type").getAsString();
        }
        if (object.has("command")) {
            return "command";
        }
        if (object.has("commands")) {
            return "commands";
        }
        if (object.has("execute")) {
            return "execute";
        }
        if (object.has("mod_command")) {
            return "mod_command";
        }
        if (object.has("message") || object.has("send_message")) {
            object.addProperty("message", object.has("message") ? object.get("message").getAsString() : object.get("send_message").getAsString());
            return "send_message";
        }
        if (object.has("give_item")) {
            object.addProperty("item", object.get("give_item").getAsString());
            return "give_item";
        }
        if (object.has("take_item")) {
            object.addProperty("item", object.get("take_item").getAsString());
            return "take_item";
        }
        if (object.has("teleport")) {
            object.addProperty("dimension", object.get("teleport").getAsString());
            return "teleport";
        }
        if (object.has("set_world_time")) {
            return "set_world_time";
        }
        if (object.has("spawn_mob")) {
            return "spawn_mob";
        }
        if (object.has("world_border") || object.has("size")) {
            return "world_border";
        }
        if (object.has("place_block")) {
            return "place_block";
        }
        if (object.has("remove_block")) {
            return "remove_block";
        }
        if (object.has("set_health")) {
            return "set_health";
        }
        if (object.has("set_score")) {
            return "set_score";
        }
        if (object.has("add_score")) {
            return "add_score";
        }
        if (object.has("give_currency")) {
            return "give_currency";
        }
        if (object.has("set_spawn_point")) {
            return "set_spawn_point";
        }
        if (object.has("trigger_event")) {
            return "trigger_event";
        }
        if (object.has("complete_quest")) {
            return "complete_quest";
        }
        if (object.has("reset_quest")) {
            return "reset_quest";
        }
        if (object.has("wait_until")) {
            object.add("condition", object.get("wait_until"));
            return "wait_until";
        }
        return "command";
    }
}
