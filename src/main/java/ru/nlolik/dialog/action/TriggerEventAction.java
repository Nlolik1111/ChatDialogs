package ru.nlolik.dialog.action;

import ru.nlolik.dialog.DialogManager;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

import java.util.HashMap;
import java.util.Map;

public class TriggerEventAction implements DialogAction {
    private final String name;

    public TriggerEventAction(String name) {
        this.name = name;
    }

    @Override
    public void execute(DialogContext context) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event_name", PlaceholderEngine.resolve(name, context));
        DialogManager.triggerCustomEvent(payload.get("event_name").toString(), context.player(), payload);
    }
}
