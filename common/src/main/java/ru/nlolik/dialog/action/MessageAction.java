package ru.nlolik.dialog.action;

import net.minecraft.network.chat.Component;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;
import ru.nlolik.dialog.runtime.TextRenderer;

public class MessageAction implements DialogAction {
    private final String message;
    private final boolean broadcast;

    public MessageAction(String message, boolean broadcast) {
        this.message = message;
        this.broadcast = broadcast;
    }

    @Override
    public void execute(DialogContext context) {
        String text = PlaceholderEngine.resolve(message, context);
        Component component = TextRenderer.render(text);
        if (broadcast) {
            context.server().getPlayerList().broadcastSystemMessage(component, false);
        } else {
            context.player().sendSystemMessage(component);
        }
    }
}
