package com.lyyl.nhkanban;

import com.lyyl.nhkanban.common.network.NHKanbanNet;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        NHKanbanNet.registerClientPackets();
    }
}
