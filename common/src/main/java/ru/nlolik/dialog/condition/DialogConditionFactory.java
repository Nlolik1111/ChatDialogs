package ru.nlolik.dialog.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public final class DialogConditionFactory {
    private DialogConditionFactory() {
    }

    public static DialogCondition parse(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return DialogCondition.TRUE;
        }
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            return parseExpression(value);
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("if")) {
                return parseExpression(object.get("if").getAsString());
            }
            if (object.has("check")) {
                return parseExpression(object.get("check").getAsString());
            }
            if (object.has("random") || object.has("random_check")) {
                int chance = object.has("random") ? object.get("random").getAsInt() : object.get("random_check").getAsInt();
                return new RandomCondition(chance);
            }
            if (object.has("type")) {
                String type = object.get("type").getAsString();
                return switch (type) {
                    case "random" -> new RandomCondition(object.get("chance").getAsInt());
                    case "inventory" -> {
                        ResourceLocation id = ResourceLocation.tryParse(object.get("item").getAsString());
                        if (id == null) {
                            yield DialogCondition.TRUE;
                        }
                        yield new InventoryCondition(id, object.has("count") ? object.get("count").getAsInt() : 1);
                    }
                    case "score" -> new ScoreCondition(object.get("objective").getAsString(), object.get("operator").getAsString(), object.get("value").getAsString());
                    case "comparison" -> new ComparisonCondition(object.get("left").getAsString(), object.get("operator").getAsString(), object.get("right").getAsString());
                    default -> DialogCondition.TRUE;
                };
            }
        }
        return DialogCondition.TRUE;
    }

    private static DialogCondition parseExpression(String expression) {
        String trimmed = expression.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        String[] operators = {"<=", ">=", "==", "!=", "<", ">"};
        for (String op : operators) {
            int index = trimmed.indexOf(op);
            if (index > 0) {
                String left = trimmed.substring(0, index).trim();
                String right = trimmed.substring(index + op.length()).trim();
                return new ComparisonCondition(left, op, right);
            }
        }
        return DialogCondition.TRUE;
    }
}
