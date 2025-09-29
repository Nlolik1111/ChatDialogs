package ru.nlolik.dialog.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record DialogFile(String source, List<DialogDefinition> dialogs) {

    public static DialogFile parse(JsonElement root, String source) {
        List<DialogDefinition> definitions = new ArrayList<>();
        if (root == null || root.isJsonNull()) {
            return new DialogFile(source, definitions);
        }

        if (root.isJsonArray()) {
            JsonArray array = root.getAsJsonArray();
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    definitions.add(DialogJsonParser.parseDefinition(element.getAsJsonObject(), source));
                }
            }
        } else if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            if (object.has("dialogs") && object.get("dialogs").isJsonArray()) {
                JsonArray array = object.getAsJsonArray("dialogs");
                for (JsonElement element : array) {
                    if (element.isJsonObject()) {
                        definitions.add(DialogJsonParser.parseDefinition(element.getAsJsonObject(), source));
                    }
                }
            } else {
                definitions.add(DialogJsonParser.parseDefinition(object, source));
            }
        }

        return new DialogFile(source, Collections.unmodifiableList(definitions));
    }
}
