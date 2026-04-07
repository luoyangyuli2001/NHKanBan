package com.lyyl.nhkanban;

import com.lyyl.nhkanban.common.network.NHKanbanNet;
import com.lyyl.nhkanban.common.registry.NHKanbanBlocks;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        NHKanBan.LOG.info(Config.greeting);
        NHKanBan.LOG.info("I am MyMod at version " + Tags.VERSION);

        NHKanbanBlocks.register();
        NHKanbanNet.registerCommonPackets();
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}
}
