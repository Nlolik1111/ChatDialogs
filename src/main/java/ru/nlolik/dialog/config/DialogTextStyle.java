package ru.nlolik.dialog.config;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DialogTextStyle {
    private final String color;
    private final List<String> formats;
    private final String modifier;

    public DialogTextStyle(String color, List<String> formats, String modifier) {
        this.color = color;
        this.formats = formats == null ? List.of() : List.copyOf(formats);
        this.modifier = modifier == null ? "" : modifier.toLowerCase(Locale.ROOT);
    }

    public String color() {
        return color;
    }

    public List<String> formats() {
        return Collections.unmodifiableList(formats);
    }

    public String modifier() {
        return modifier;
    }

    public boolean isEmpty() {
        return (color == null || color.isBlank()) && formats.isEmpty() && modifier.isBlank();
    }
}
