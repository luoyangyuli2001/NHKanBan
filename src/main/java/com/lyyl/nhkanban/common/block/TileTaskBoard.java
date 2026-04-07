package com.lyyl.nhkanban.common.block;

import net.minecraft.tileentity.TileEntity;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.lyyl.nhkanban.NHKanBan;
import com.lyyl.nhkanban.client.ui.TaskBoardPanel;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 任务板的 TileEntity。本身不存任何状态——所有数据来自服务端 TaskRepository,
 * 通过网络包推到客户端 cache。这里只是 GUI 的挂载点。
 *
 * <p>
 * 多玩家共享同一个 TE 实例是安全的:每次 open 由 NHKanbanGui 按调用方
 * player 单独查询和单播,GUI 会话也由 MUI2 按 player 隔离。这里不要为了
 * "缓存某玩家最近的查询" 之类的优化引入字段——一旦 TE 持有 player-specific
 * 状态,多人共用立刻出问题。
 *
 * <p>
 * buildUI 在客户端和服务端各跑一次,所以严禁在这里访问 TaskRepository
 * (在客户端会拿到空 repo)。Panel 内部读取数据必须通过 ClientTaskCache,
 * 且要确保只在 client-only 代码路径上引用 cache 类。
 */

public class TileTaskBoard extends TileEntity implements IGuiHolder<PosGuiData> {

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return TaskBoardPanel.build();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(NHKanBan.MODID, mainPanel);
    }

    @Override
    public boolean canUpdate() {
        return false;
    }
}
