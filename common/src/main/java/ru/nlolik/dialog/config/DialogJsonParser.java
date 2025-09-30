package ru.nlolik.dialog.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import ru.nlolik.dialog.action.DialogAction;
import ru.nlolik.dialog.action.DialogActionParser;
import ru.nlolik.dialog.condition.DialogCondition;
import ru.nlolik.dialog.condition.DialogConditionFactory;
import ru.nlolik.dialog.event.DialogEventTrigger;
import ru.nlolik.dialog.event.DialogEventType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class DialogJsonParser {
    private DialogJsonParser() {
    }

    public static DialogDefinition parseDefinition(JsonObject object, String source) {
        String sourceName = source;
        if (sourceName == null || sourceName.isBlank()) {
            sourceName = "dialog";
        }
        int extensionIndex = sourceName.lastIndexOf('.');
        if (extensionIndex > 0) {
            sourceName = sourceName.substring(0, extensionIndex);
        }
        String id = getString(object, "id", sourceName.replace('.', '_'));
        String displayName = getString(object, "name", id);
        String start = getString(object, "start", "start");
        int initialDelay = getTimeTicks(object, "time_to_start", "delay");

        Map<String, DialogNode> nodes = new LinkedHashMap<>();
        if (object.has("nodes") && object.get("nodes").isJsonObject()) {
            JsonObject nodesObj = object.getAsJsonObject("nodes");
            for (Map.Entry<String, JsonElement> entry : nodesObj.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                nodes.put(entry.getKey(), parseNode(entry.getKey(), entry.getValue().getAsJsonObject()));
            }
        } else if (object.has("steps") && object.get("steps").isJsonArray()) {
            JsonArray array = object.getAsJsonArray("steps");
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject step = element.getAsJsonObject();
                String nodeId = getString(step, "id", null);
                if (nodeId == null) {
                    continue;
                }
                nodes.put(nodeId, parseNode(nodeId, step));
            }
        }

        List<DialogEventTrigger> triggers = parseTriggers(object.get("events"), id);
        return new DialogDefinition(id, displayName, sourceName, start, initialDelay, nodes, triggers);
    }

    private static DialogNode parseNode(String nodeId, JsonObject nodeObj) {
        List<DialogLine> lines = parseLines(nodeObj.get("lines"));
        if (lines.isEmpty()) {
            lines.addAll(parseLines(nodeObj.get("messages")));
        }
        List<DialogAction> actions = DialogActionParser.parse(nodeObj.get("actions"));
        List<DialogButton> buttons = parseButtons(nodeObj.get("buttons"));
        List<ConditionalBranch> branches = parseBranches(nodeObj);
        int startDelay = getTimeTicks(nodeObj, "time_to_start", "start_delay");
        String autoNext = getString(nodeObj, "auto_next", getString(nodeObj, "default_next", null));
        int autoNextDelay = getInt(nodeObj, "auto_next_delay", getInt(nodeObj, "auto_delay", 0));
        boolean closeOnFinish = nodeObj.has("close") && nodeObj.get("close").getAsBoolean();
        int stopDelay = getTimeTicks(nodeObj, "stop_time", "stop_delay");
        if (stopDelay == 0 && nodeObj.has("stop_ticks") && nodeObj.get("stop_ticks").isJsonPrimitive()) {
            stopDelay = Math.max(0, nodeObj.get("stop_ticks").getAsInt());
        }
        return new DialogNode(nodeId, lines, actions, buttons, branches, startDelay, autoNext, autoNextDelay, closeOnFinish, stopDelay);
    }

    private static List<DialogLine> parseLines(JsonElement element) {
        List<DialogLine> lines = new ArrayList<>();
        if (element == null) {
            return lines;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement jsonElement : array) {
                parseLine(jsonElement).ifPresent(lines::add);
            }
        } else {
            parseLine(element).ifPresent(lines::add);
        }
        return lines;
    }

    private static java.util.Optional<DialogLine> parseLine(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return java.util.Optional.empty();
        }
        if (element.isJsonPrimitive()) {
            return java.util.Optional.of(new DialogLine(element.getAsString(), new DialogTextStyle(null, List.of(), null), 0, null));
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            String text = getString(obj, "text", "");
            DialogTextStyle style = parseStyle(obj, "text_color", "text_format", "text_modifier");
            int delay = getInt(obj, "ticks", getInt(obj, "delay", 0));
            LoopSettings loop = null;
            if (obj.has("loop") && obj.get("loop").isJsonObject()) {
                JsonObject loopObj = obj.getAsJsonObject("loop");
                int times = getInt(loopObj, "times", loopObj.has("forever") && loopObj.get("forever").getAsBoolean() ? -1 : 1);
                int interval = getInt(loopObj, "ticks", 20);
                if (loopObj.has("text")) {
                    text = loopObj.get("text").getAsString();
                }
                loop = new LoopSettings(times, interval);
            }
            return java.util.Optional.of(new DialogLine(text, style, delay, loop));
        }
        return java.util.Optional.empty();
    }

    private static List<DialogButton> parseButtons(JsonElement element) {
        List<DialogButton> buttons = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return buttons;
        }
        JsonArray array = element.getAsJsonArray();
        int index = 0;
        for (JsonElement entry : array) {
            index++;
            if (entry.isJsonPrimitive()) {
                String text = entry.getAsString();
                buttons.add(new DialogButton("button_" + index, text, new DialogTextStyle(null, List.of(), null), List.of(), null, List.of(), false, 0));
                continue;
            }
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject obj = entry.getAsJsonObject();
            String id = getString(obj, "id", getString(obj, "name", "button_" + index));
            String text = getString(obj, "text", getString(obj, "button_text", "Button"));
            DialogTextStyle style = parseStyle(obj, "button_color", "button_format", "button_modifier");
            if (style.isEmpty()) {
                style = parseStyle(obj, "text_color", "text_format", "text_modifier");
            }
            List<DialogAction> actions = new ArrayList<>(DialogActionParser.parse(obj.get("actions")));
            if (obj.has("button_action")) {
                actions.addAll(DialogActionParser.parse(obj.get("button_action")));
            }
            if (obj.has("button_execute")) {
                actions.addAll(DialogActionParser.parse(obj.get("button_execute")));
            }
            String next = getString(obj, "next", getString(obj, "goto", null));
            List<DialogCondition> conditions = parseConditions(obj.get("conditions"));
            if (obj.has("if")) {
                conditions.add(DialogConditionFactory.parse(obj.get("if")));
            }
            boolean close = obj.has("close") && obj.get("close").getAsBoolean();
            close = close || (obj.has("button_close") && obj.get("button_close").getAsBoolean());
            int delay = getInt(obj, "delay", getInt(obj, "ticks", 0));
            buttons.add(new DialogButton(id, text, style, actions, next, conditions, close, delay));
        }
        return buttons;
    }

    private static List<DialogCondition> parseConditions(JsonElement element) {
        List<DialogCondition> conditions = new ArrayList<>();
        if (element == null) {
            return conditions;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement entry : array) {
                conditions.add(DialogConditionFactory.parse(entry));
            }
        } else {
            conditions.add(DialogConditionFactory.parse(element));
        }
        return conditions;
    }

    private static List<ConditionalBranch> parseBranches(JsonObject nodeObj) {
        List<ConditionalBranch> branches = new ArrayList<>();
        if (nodeObj.has("branches") && nodeObj.get("branches").isJsonArray()) {
            JsonArray array = nodeObj.getAsJsonArray("branches");
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                ConditionalBranch.Type type = ConditionalBranch.Type.IF;
                if (obj.has("type")) {
                    String typeValue = obj.get("type").getAsString().toLowerCase(Locale.ROOT);
                    if ("elif".equals(typeValue) || "else_if".equals(typeValue)) {
                        type = ConditionalBranch.Type.ELIF;
                    } else if ("else".equals(typeValue)) {
                        type = ConditionalBranch.Type.ELSE;
                    }
                }
                DialogCondition condition = type == ConditionalBranch.Type.ELSE ? DialogCondition.TRUE : DialogConditionFactory.parse(obj.get("condition"));
                List<DialogAction> actions = DialogActionParser.parse(obj.get("actions"));
                String next = getString(obj, "next", null);
                branches.add(new ConditionalBranch(type, condition, actions, next));
            }
            return branches;
        }
        if (nodeObj.has("if")) {
            DialogCondition condition = DialogConditionFactory.parse(nodeObj.get("if"));
            List<DialogAction> actions = DialogActionParser.parse(nodeObj.get("then"));
            String next = getString(nodeObj, "then_next", null);
            branches.add(new ConditionalBranch(ConditionalBranch.Type.IF, condition, actions, next));
        }
        if (nodeObj.has("elif")) {
            JsonElement elifElement = nodeObj.get("elif");
            if (elifElement.isJsonArray()) {
                for (JsonElement entry : elifElement.getAsJsonArray()) {
                    if (!entry.isJsonObject()) {
                        continue;
                    }
                    JsonObject obj = entry.getAsJsonObject();
                    DialogCondition condition = DialogConditionFactory.parse(obj.get("condition"));
                    List<DialogAction> actions = DialogActionParser.parse(obj.get("actions"));
                    String next = getString(obj, "next", null);
                    branches.add(new ConditionalBranch(ConditionalBranch.Type.ELIF, condition, actions, next));
                }
            } else {
                DialogCondition condition = DialogConditionFactory.parse(elifElement);
                branches.add(new ConditionalBranch(ConditionalBranch.Type.ELIF, condition, DialogActionParser.parse(nodeObj.get("elif_actions")), getString(nodeObj, "elif_next", null)));
            }
        }
        if (nodeObj.has("else")) {
            JsonElement elseElement = nodeObj.get("else");
            List<DialogAction> actions = DialogActionParser.parse(elseElement);
            String next = getString(nodeObj, "else_next", null);
            branches.add(new ConditionalBranch(ConditionalBranch.Type.ELSE, DialogCondition.TRUE, actions, next));
        }
        return branches;
    }

    private static List<DialogEventTrigger> parseTriggers(JsonElement element, String dialogId) {
        List<DialogEventTrigger> triggers = new ArrayList<>();
        if (element == null) {
            return triggers;
        }
        if (!element.isJsonArray()) {
            return triggers;
        }
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject obj = entry.getAsJsonObject();
            DialogEventType type = DialogEventType.from(getString(obj, "event", null));
            String name = getString(obj, "name", getString(obj, "event_name", null));
            String targetDialog = getString(obj, "dialog", dialogId);
            String node = getString(obj, "node", null);
            List<DialogCondition> conditions = parseConditions(obj.get("conditions"));
            List<DialogAction> actions = DialogActionParser.parse(obj.get("actions"));
            Map<String, String> filters = parseFilters(obj.get("filters"));
            triggers.add(new DialogEventTrigger(type, name, targetDialog, node, conditions, actions, filters));
        }
        return triggers;
    }

    private static Map<String, String> parseFilters(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return Map.of();
        }
        JsonObject obj = element.getAsJsonObject();
        return obj.entrySet().stream()
                .filter(entry -> entry.getValue().isJsonPrimitive())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getAsString()));
    }

    private static DialogTextStyle parseStyle(JsonObject object, String colorKey, String formatKey, String modifierKey) {
        String color = object.has(colorKey) ? object.get(colorKey).getAsString() : null;
        List<String> formats = new ArrayList<>();
        if (object.has(formatKey)) {
            JsonElement element = object.get(formatKey);
            if (element.isJsonArray()) {
                for (JsonElement format : element.getAsJsonArray()) {
                    formats.add(format.getAsString());
                }
            } else if (element.isJsonPrimitive()) {
                formats.add(element.getAsString());
            }
        }
        String modifier = object.has(modifierKey) ? object.get(modifierKey).getAsString() : null;
        return new DialogTextStyle(color, formats, modifier);
    }

    private static String getString(JsonObject obj, String key, String fallback) {
        if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
            return obj.get(key).getAsString();
        }
        return fallback;
    }

    private static int getInt(JsonObject obj, String key, int fallback) {
        if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
            JsonPrimitive primitive = obj.getAsJsonPrimitive(key);
            if (primitive.isNumber()) {
                try {
                    return primitive.getAsInt();
                } catch (NumberFormatException ignored) {
                }
            }
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? 1 : 0;
            }
            if (primitive.isString()) {
                try {
                    return Integer.parseInt(primitive.getAsString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return fallback;
    }

    private static int getTimeTicks(JsonObject obj, String secondsKey, String fallbackKey) {
        if (secondsKey != null && obj.has(secondsKey) && obj.get(secondsKey).isJsonPrimitive()) {
            JsonPrimitive primitive = obj.getAsJsonPrimitive(secondsKey);
            if (primitive.isNumber()) {
                double seconds = primitive.getAsDouble();
                if (Double.isFinite(seconds)) {
                    return (int) Math.max(0, Math.round(seconds * 20.0));
                }
            } else if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? 20 : 0;
            } else if (primitive.isString()) {
                String raw = primitive.getAsString();
                if (raw.equalsIgnoreCase("false")) {
                    return 0;
                }
                try {
                    double seconds = Double.parseDouble(raw);
                    return (int) Math.max(0, Math.round(seconds * 20.0));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (fallbackKey == null) {
            return 0;
        }
        return getInt(obj, fallbackKey, 0);
    }
}
