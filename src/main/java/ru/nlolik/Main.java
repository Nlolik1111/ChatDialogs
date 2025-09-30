package ru.nlolik;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import ru.nlolik.command.CommandManager;
import ru.nlolik.dialog.DialogManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "chatdialogs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Main() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Создаем папку для диалогов если её нет
            Path dialogsDir = FMLPaths.CONFIGDIR.get().resolve(MOD_ID).resolve("dialogs");
            try {
                if (!Files.exists(dialogsDir)) {
                    Files.createDirectories(dialogsDir);
                }

                // Создаем файл Welcome.txt если его нет
                Path welcomeFile = dialogsDir.resolve("Welcome.json");
                if (!Files.exists(welcomeFile)) {
                    String welcomeContent = "{\n" +
                            "  \"id\": \"welcome\",\n" +
                            "  \"name\": \"Welcome Sequence\",\n" +
                            "  \"start\": \"intro\",\n" +
                            "  \"time_to_start\": 5,\n" +
                            "  \"nodes\": {\n" +
                            "    \"intro\": {\n" +
                            "      \"lines\": [\n" +
                            "        \"&6Hello, {player_name}!\",\n" +
                            "        { \"text\": \"&7Current time: {time}\", \"ticks\": 40 }\n" +
                            "      ],\n" +
                            "      \"buttons\": [\n" +
                            "        {\n" +
                            "          \"id\": \"gift\",\n" +
                            "          \"text\": \"&aClaim Gift\",\n" +
                            "          \"actions\": [\n" +
                            "            { \"type\": \"give_item\", \"item\": \"minecraft:diamond\", \"count\": 3 },\n" +
                            "            { \"type\": \"send_message\", \"message\": \"&aEnjoy your reward!\" }\n" +
                            "          ],\n" +
                            "          \"next\": \"quest\",\n" +
                            "          \"delay\": 10\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"id\": \"later\",\n" +
                            "          \"text\": \"&cMaybe Later\",\n" +
                            "          \"actions\": [ { \"type\": \"send_message\", \"message\": \"&7Come back when you are ready.\" } ],\n" +
                            "          \"close\": true\n" +
                            "        }\n" +
                            "      ]\n" +
                            "    },\n" +
                            "    \"reward\": {\n" +
                            "      \"actions\": [\n" +
                            "        { \"type\": \"give_currency\", \"value\": \"50\" },\n" +
                            "        { \"type\": \"complete_quest\", \"complete_quest\": \"starter\" }\n" +
                            "      ],\n" +
                            "      \"close\": true,\n" +
                            "      \"stop_time\": 3\n" +
                            "    }\n" +
                            "  }\n" +
                            "}";

                    Files.writeString(welcomeFile, welcomeContent);
                    LOGGER.info("Created default Welcome.txt dialog file");
                }
            } catch (IOException e) {
                LOGGER.error("Failed to create dialogs directory or Welcome.txt file", e);
            }

            // Перезагружаем диалоги после создания файлов
            DialogManager.reload();
        });
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        DialogManager.attachServer(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        DialogManager.detachServer();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandManager.register(event.getDispatcher());
    }
}