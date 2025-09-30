package ru.nlolik.dialog.action;

public class ResetQuestAction extends ScoreAction {
    public ResetQuestAction(String questId) {
        super("quest_" + questId, "0", Mode.SET);
    }
}
