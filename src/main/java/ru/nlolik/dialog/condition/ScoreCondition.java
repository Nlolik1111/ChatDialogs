package ru.nlolik.dialog.condition;

import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import ru.nlolik.dialog.runtime.DialogContext;

public class ScoreCondition implements DialogCondition {
    private final String objectiveName;
    private final ComparisonCondition comparison;

    public ScoreCondition(String objectiveName, String operator, String value) {
        this.objectiveName = objectiveName;
        this.comparison = new ComparisonCondition("score:" + objectiveName, operator, value);
    }

    @Override
    public boolean test(DialogContext context) {
        Scoreboard scoreboard = context.player().getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return false;
        }
        return comparison.test(context);
    }
}
