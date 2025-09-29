package ru.nlolik.dialog.event;

import java.util.Locale;

public enum DialogEventType {
    ON_CLICK,
    ON_BLOCK_BREAK,
    ON_ENTITY_DEATH,
    ON_PLAYER_INTERACT,
    ON_ITEM_USE,
    CUSTOM;

    public static DialogEventType from(String value) {
        if (value == null) {
            return CUSTOM;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "on_click", "click" -> ON_CLICK;
            case "on_block_break", "block_break" -> ON_BLOCK_BREAK;
            case "on_entity_death", "entity_death" -> ON_ENTITY_DEATH;
            case "on_player_interact", "player_interact" -> ON_PLAYER_INTERACT;
            case "on_item_use", "item_use" -> ON_ITEM_USE;
            default -> CUSTOM;
        };
    }
}
