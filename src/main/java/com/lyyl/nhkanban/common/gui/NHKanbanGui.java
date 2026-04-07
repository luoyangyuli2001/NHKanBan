package com.lyyl.nhkanban.common.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

import com.cleanroommc.modularui.factory.GuiFactories;
import com.lyyl.nhkanban.common.block.TileTaskBoard;
import com.lyyl.nhkanban.common.network.NHKanbanNet;
import com.lyyl.nhkanban.common.network.TaskSummary;
import com.lyyl.nhkanban.common.network.ViewTab;
import com.lyyl.nhkanban.common.network.packet.TaskListPacket;
import com.lyyl.nhkanban.common.storage.TaskRepository;
import com.lyyl.nhkanban.common.task.Task;
import com.lyyl.nhkanban.common.task.TaskScope;

/**
 * 所有 GUI 入口的服务端封装。先把数据包推到客户端,再调 MUI2 open。
 *
 * <p>
 * 顺序很关键:先 sendTo,后 open。SimpleNetworkWrapper 入队比 MUI2 的 open
 * 命令快得多,实测客户端 buildUI 执行前数据包已经到位,所以打开 GUI 时
 * ClientTaskCache 就有内容。颠倒顺序会让首次渲染看到空 cache,出现
 * "先空帧再刷新"的视觉跳变。
 *
 * <p>
 * 来源(方块还是道具)由 GuiContext 区分,内部分支选不同的 MUI2 工厂。
 * 所有方块/道具/包 handler 都走这一个入口,不要在调用方各自拼接,以免
 * 日后两边漂移。
 */
public final class NHKanbanGui {

    private static final int MAX_LIST_SIZE = 20;

    /** 列表排序:URGENT 在前,同优先级新的在前。 */
    private static final Comparator<Task> TASK_ORDER = new Comparator<Task>() {

        @Override
        public int compare(Task a, Task b) {
            int p = b.getPriority()
                .ordinal()
                - a.getPriority()
                    .ordinal();
            if (p != 0) return p;
            return Long.compare(b.getCreatedAt(), a.getCreatedAt());
        }
    };

    private NHKanbanGui() {}

    public static void openMainBoard(EntityPlayerMP player, GuiContext ctx, ViewTab tab) {
        if (player == null || ctx == null) return;

        // 数据包总是先发,无论来源
        List<TaskSummary> summaries = querySummariesForTab(player, tab);
        NHKanbanNet.CHANNEL.sendTo(new TaskListPacket(tab, summaries), player);

        if (ctx.isFromItem()) {
            GuiFactories.playerInventory()
                .openFromMainHand(player);
            return;
        }

        // 方块路径:从 ctx 重新查 tile,顺便兜底"tile 已经被破坏"的情况
        if (player.worldObj == null) return;
        TileEntity te = player.worldObj.getTileEntity(ctx.getX(), ctx.getY(), ctx.getZ());
        if (!(te instanceof TileTaskBoard)) return;
        GuiFactories.tileEntity()
            .open(player, (TileTaskBoard) te);
    }

    private static List<TaskSummary> querySummariesForTab(EntityPlayerMP player, ViewTab tab) {
        UUID id = player.getUniqueID();
        List<Task> raw;
        switch (tab) {
            case MINE: {
                Set<UUID> seen = new HashSet<UUID>();
                List<Task> mine = new ArrayList<Task>();
                for (Task t : TaskRepository.get()
                    .findByAuthor(id)) {
                    if (seen.add(t.getUuid())) mine.add(t);
                }
                for (Task t : TaskRepository.get()
                    .findByClaimer(id)) {
                    if (seen.add(t.getUuid())) mine.add(t);
                }
                raw = mine;
                break;
            }
            case PUBLIC: {
                raw = new ArrayList<Task>();
                for (Task t : TaskRepository.get()
                    .all()) {
                    if (t.getScope() == TaskScope.GLOBAL) {
                        raw.add(t);
                    }
                }
                break;
            }
            case INBOX: {
                raw = TaskRepository.get()
                    .findByTarget(id);
                break;
            }
            case ARCHIVE:
            default:
                raw = Collections.emptyList();
        }

        if (!raw.isEmpty()) {
            Collections.sort(raw, TASK_ORDER);
        }
        if (raw.size() > MAX_LIST_SIZE) {
            raw = raw.subList(0, MAX_LIST_SIZE);
        }
        List<TaskSummary> result = new ArrayList<TaskSummary>(raw.size());
        for (Task t : raw) {
            result.add(TaskSummary.from(t));
        }
        return result;
    }
}
