package ru.nlolik.dialog.runtime;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import ru.nlolik.dialog.config.DialogTextStyle;

import java.util.List;
import java.util.Locale;

public final class TextRenderer {
    private TextRenderer() {
    }

    public static Component render(String text) {
        return render(text, null);
    }

    public static Component render(String text, DialogTextStyle style) {
        String modified = applyModifier(text, style);
        MutableComponent result = Component.empty();
        Style currentStyle = Style.EMPTY;
        if (style != null && style.color() != null && !style.color().isBlank()) {
            TextColor color = parseColor(style.color());
            if (color != null) {
                currentStyle = currentStyle.withColor(color);
            }
        }
        if (style != null) {
            for (String format : style.formats()) {
                currentStyle = applyFormat(currentStyle, format);
            }
        }

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < modified.length(); i++) {
            char c = modified.charAt(i);
            if (c == '&' && i + 1 < modified.length()) {
                ChatFormatting formatting = ChatFormatting.getByCode(modified.charAt(i + 1));
                if (formatting != null) {
                    if (buffer.length() > 0) {
                        result.append(Component.literal(buffer.toString()).withStyle(currentStyle));
                        buffer.setLength(0);
                    }
                    currentStyle = applyFormattingCode(currentStyle, formatting);
                    i++;
                    continue;
                }
            }
            buffer.append(c);
        }
        if (buffer.length() > 0) {
            result.append(Component.literal(buffer.toString()).withStyle(currentStyle));
        }
        return result;
    }

    private static String applyModifier(String text, DialogTextStyle style) {
        if (style == null || style.modifier().isBlank()) {
            return text;
        }
        return switch (style.modifier()) {
            case "uppercase" -> text.toUpperCase(Locale.ROOT);
            case "lowercase" -> text.toLowerCase(Locale.ROOT);
            case "capitalize" -> capitalize(text);
            default -> text;
        };
    }

    private static String capitalize(String text) {
        if (text.isEmpty()) {
            return text;
        }
        String[] parts = text.split(" ");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                builder.append(parts[i].substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    private static Style applyFormat(Style style, String format) {
        String key = format.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "bold" -> style.withBold(true);
            case "italic" -> style.withItalic(true);
            case "underline", "underlined" -> style.withUnderlined(true);
            case "strikethrough" -> style.withStrikethrough(true);
            case "obfuscated", "magic" -> style.withObfuscated(true);
            default -> style;
        };
    }

    private static Style applyFormattingCode(Style base, ChatFormatting formatting) {
        if (formatting.isColor()) {
            return base.withColor(TextColor.fromLegacyFormat(formatting));
        }
        return switch (formatting) {
            case BOLD -> base.withBold(true);
            case ITALIC -> base.withItalic(true);
            case UNDERLINE -> base.withUnderlined(true);
            case STRIKETHROUGH -> base.withStrikethrough(true);
            case OBFUSCATED -> base.withObfuscated(true);
            case RESET -> Style.EMPTY;
            default -> base;
        };
    }

    private static TextColor parseColor(String color) {
        String normalized = color.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("#") && normalized.length() == 7) {
            try {
                int value = Integer.parseInt(normalized.substring(1), 16);
                return TextColor.fromRgb(value);
            } catch (NumberFormatException ignored) {
            }
        }
        ChatFormatting formatting = ChatFormatting.getByName(normalized);
        if (formatting != null && formatting.isColor()) {
            return TextColor.fromLegacyFormat(formatting);
        }
        if (normalized.startsWith("&") && normalized.length() == 2) {
            ChatFormatting byCode = ChatFormatting.getByCode(normalized.charAt(1));
            if (byCode != null && byCode.isColor()) {
                return TextColor.fromLegacyFormat(byCode);
            }
        }
        return null;
    }
}
