package com.lyyl.nhkanban.common.registry;

import com.lyyl.nhkanban.NHKanBan;
import com.lyyl.nhkanban.common.block.BlockTaskBoard;
import com.lyyl.nhkanban.common.block.TileTaskBoard;

import cpw.mods.fml.common.registry.GameRegistry;

public final class NHKanbanBlocks {

    public static BlockTaskBoard taskBoard;

    private NHKanbanBlocks() {}

    public static void register() {
        taskBoard = new BlockTaskBoard();
        GameRegistry.registerBlock(taskBoard, "task_board");
        GameRegistry.registerTileEntity(TileTaskBoard.class, NHKanBan.MODID + ":task_board");
    }
}
