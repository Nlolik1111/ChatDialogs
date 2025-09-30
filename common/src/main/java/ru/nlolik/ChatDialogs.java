package ru.nlolik;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import ru.nlolik.command.CommandManager;
import ru.nlolik.dialog.DialogManager;

public final class ChatDialogs {
    public static final String MOD_ID = "chatdialogs";
    public static final Logger LOGGER = LogUtils.getLogger();

    private ChatDialogs() {
    }

    public static void init() {
        DialogManager.reload();
    }

    public static void attachServer(MinecraftServer server) {
        DialogManager.attachServer(server);
    }

    public static void detachServer() {
        DialogManager.detachServer();
    }

    public static void tickServer() {
        DialogManager.tick();
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandManager.register(dispatcher);
    }
}
