package ru.nlolik;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import ru.nlolik.command.CommandManager;
import ru.nlolik.dialog.DialogManager;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "nwutils";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Main() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(DialogManager::reload);
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
