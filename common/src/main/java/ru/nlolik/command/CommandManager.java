package ru.nlolik.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ru.nlolik.dialog.DialogManager;
import ru.nlolik.dialog.config.DialogDefinition;
import ru.nlolik.dialog.runtime.DialogRuntime;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CommandManager {

    private CommandManager() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("dialog");

        root.then(Commands.literal("start")
                .then(Commands.argument("dialog", StringArgumentType.string())
                        .suggests(CommandManager::suggestDialogs)
                        .executes(ctx -> startFor(ctx, StringArgumentType.getString(ctx, "dialog"), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> startFor(ctx, StringArgumentType.getString(ctx, "dialog"), EntityArgument.getPlayer(ctx, "target"))))));

        root.then(Commands.literal("choose")
                .then(Commands.argument("token", StringArgumentType.string())
                        .executes(ctx -> choose(ctx, StringArgumentType.getString(ctx, "token")))));

        root.then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> reload(ctx.getSource())));

        root.then(Commands.literal("stop")
                .then(Commands.argument("target", EntityArgument.player())
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> stop(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))))
                .executes(ctx -> stop(ctx.getSource(), ctx.getSource().getPlayerOrException())));

        root.executes(ctx -> {
            ctx.getSource().sendFailure(Component.translatable("command.chatdialogs.dialog.syntax"));
            return 0;
        });

        dispatcher.register(root);
    }

    private static int choose(CommandContext<CommandSourceStack> ctx, String token) {
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(Component.translatable("command.chatdialogs.dialog.choose_player"));
            return 0;
        }
        DialogRuntime runtime = DialogManager.getRuntime(player);
        if (runtime == null) {
            ctx.getSource().sendFailure(Component.translatable("command.chatdialogs.dialog.choose_missing"));
            return 0;
        }
        runtime.choose(token);
        return 1;
    }

    private static int startFor(CommandContext<CommandSourceStack> ctx, String dialogId, ServerPlayer player) {
        DialogDefinition definition = DialogManager.findDefinition(dialogId);
        if (definition == null) {
            ctx.getSource().sendFailure(Component.translatable("command.chatdialogs.dialog.not_found", dialogId));
            return 0;
        }
        boolean started = DialogManager.startDialog(dialogId, player, UUID.randomUUID().toString());
        if (started) {
            String display = definition.displayName();
            ctx.getSource().sendSuccess(() -> Component.translatable("command.chatdialogs.dialog.started", display, player.getName().getString()), false);
            return 1;
        }
        ctx.getSource().sendFailure(Component.translatable("command.chatdialogs.dialog.start_failed", dialogId));
        return 0;
    }

    private static int stop(CommandSourceStack source, ServerPlayer player) {
        boolean stopped = DialogManager.stopDialog(player.getUUID());
        if (stopped) {
            source.sendSuccess(() -> Component.translatable("command.chatdialogs.dialog.stopped", player.getName().getString()), false);
            return 1;
        }
        source.sendFailure(Component.translatable("command.chatdialogs.dialog.stop_missing", player.getName().getString()));
        return 0;
    }

    private static int reload(CommandSourceStack source) {
        DialogManager.reload();
        int count = DialogManager.definitions().size();
        source.sendSuccess(() -> Component.translatable("command.chatdialogs.dialog.reloaded", count), false);
        if (count > 0) {
            source.sendSuccess(() -> Component.translatable("command.chatdialogs.dialog.available", String.join(", ", DialogManager.dialogSuggestions())), false);
        }
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestDialogs(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(DialogManager.dialogSuggestions(), builder);
    }
}
