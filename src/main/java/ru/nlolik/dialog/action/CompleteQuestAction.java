package ru.nlolik.dialog.action;

public class CompleteQuestAction extends ScoreAction {
    public CompleteQuestAction(String questId) {
        super("quest_" + questId, "1", Mode.SET);
    }
}
