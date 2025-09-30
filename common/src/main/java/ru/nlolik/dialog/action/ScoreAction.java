package ru.nlolik.dialog.action;

import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class ScoreAction implements DialogAction {
    public enum Mode {
        SET,
        ADD,
        REMOVE
    }

    private final String objective;
    private final String value;
    private final Mode mode;

    public ScoreAction(String objective, String value, Mode mode) {
        this.objective = objective;
        this.value = value;
        this.mode = mode;
    }

    @Override
    public void execute(DialogContext context) {
        Scoreboard scoreboard = context.player().getScoreboard();
        Objective obj = scoreboard.getObjective(objective);
        if (obj == null) {
            obj = scoreboard.addObjective(objective, net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY, net.minecraft.network.chat.Component.literal(objective), net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER);
        }
        Score score = scoreboard.getOrCreatePlayerScore(context.player().getScoreboardName(), obj);
        int amount = parseValue(context);
        switch (mode) {
            case SET -> score.setScore(amount);
            case ADD -> score.add(amount);
            case REMOVE -> score.add(-amount);
        }
    }

    private int parseValue(DialogContext context) {
        try {
            return Integer.parseInt(PlaceholderEngine.resolve(value, context));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
