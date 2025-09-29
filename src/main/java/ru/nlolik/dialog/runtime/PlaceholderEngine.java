package ru.nlolik.dialog.runtime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import ru.nlolik.dialog.DialogManager;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderEngine {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

    private PlaceholderEngine() {
    }

    public static String resolve(String input, DialogContext context) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = resolvePlaceholder(matcher.group(1), context);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String resolvePlaceholder(String key, DialogContext context) {
        ServerPlayer player = context.player();
        ServerLevel level = player.serverLevel();
        Map<String, Object> event = context.eventData();
        switch (key.toLowerCase(Locale.ROOT)) {
            case "player_name":
                return player.getGameProfile().getName();
            case "player_uuid":
                return player.getUUID().toString();
            case "player_health":
                return String.valueOf(Mth.floor(player.getHealth()));
            case "player_coords.x":
                return String.valueOf(Mth.floor(player.getX()));
            case "player_coords.y":
                return String.valueOf(Mth.floor(player.getY()));
            case "player_coords.z":
                return String.valueOf(Mth.floor(player.getZ()));
            case "world_name":
                return level.dimension().location().toString();
            case "time":
                long dayTime = level.getDayTime() % 24000L;
                int hours = (int) ((dayTime / 1000 + 6) % 24);
                int minutes = (int) ((dayTime % 1000) * 60 / 1000.0);
                return String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
            case "date":
                return LocalDate.now().toString();
            case "event_name":
                Object eventName = event.get("event_name");
                return eventName == null ? "" : eventName.toString();
            case "player_score":
                return String.valueOf(getScore(player, "player_score"));
            default:
                break;
        }

        if (key.startsWith("random:")) {
            String[] parts = key.split(":");
            if (parts.length == 3) {
                try {
                    int min = Integer.parseInt(parts[1]);
                    int max = Integer.parseInt(parts[2]);
                    if (max < min) {
                        int tmp = min;
                        min = max;
                        max = tmp;
                    }
                    return String.valueOf(DialogManager.random().nextInt(max - min + 1) + min);
                } catch (NumberFormatException ignored) {
                }
            }
        } else if (key.startsWith("score:")) {
            String objective = key.substring("score:".length());
            return String.valueOf(getScore(player, objective));
        } else if (key.startsWith("mob_count")) {
            int radius = 8;
            if (key.contains(":")) {
                String[] parts = key.split(":");
                if (parts.length >= 2) {
                    try {
                        radius = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return String.valueOf(countMobs(player, radius));
        } else if (key.startsWith("event.")) {
            String eventKey = key.substring("event.".length());
            Object value = event.get(eventKey);
            return value == null ? "" : value.toString();
        }
        return "{" + key + "}";
    }

    private static int getScore(ServerPlayer player, String objectiveName) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return 0;
        }
        Score score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
        return score.getScore();
    }

    private static int countMobs(ServerPlayer player, int radius) {
        return (int) player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), e -> e != player).size();
    }
}
