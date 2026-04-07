package com.lyyl.nhkanban;

import java.io.File;

import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lyyl.nhkanban.common.command.KanbanCommand;
import com.lyyl.nhkanban.common.storage.TaskStorage;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;

@Mod(modid = NHKanBan.MODID, version = Tags.VERSION, name = "NHKanBan", acceptedMinecraftVersions = "[1.7.10]")
public class NHKanBan {

    public static final String MODID = "nhkanban";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "com.lyyl.nhkanban.ClientProxy", serverSide = "com.lyyl.nhkanban.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        if (worldDir == null) {
            World overworld = event.getServer()
                .worldServerForDimension(0);
            if (overworld != null) {
                worldDir = overworld.getSaveHandler()
                    .getWorldDirectory();
            }
        }
        if (worldDir != null) {
            TaskStorage.get()
                .init(worldDir);
        } else {
            LOG.error("NHKanBan: cannot resolve world directory, storage not initialized");
        }

        // 注册指令
        event.registerServerCommand(new KanbanCommand());

        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        TaskStorage.get()
            .shutdown();
    }
}
