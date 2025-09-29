package ru.nlolik.dialog.action;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class CommandAction implements DialogAction {
    private final String command;
    private final boolean runAsPlayer;
    private final boolean silent;

    public CommandAction(String command, boolean runAsPlayer, boolean silent) {
        this.command = command;
        this.runAsPlayer = runAsPlayer;
        this.silent = silent;
    }

    @Override
    public void execute(DialogContext context) throws CommandSyntaxException {
        String resolved = PlaceholderEngine.resolve(command, context);
        CommandSourceStack source;
        if (runAsPlayer) {
            source = context.player().createCommandSourceStack();
        } else {
            source = context.server().createCommandSourceStack().withSuppressedOutput();
            source = source.withEntity(context.player());
        }
        if (silent) {
            source = source.withSuppressedOutput();
        }
        context.server().getCommands().performPrefixedCommand(source, resolved.startsWith("/") ? resolved.substring(1) : resolved);
    }
}
