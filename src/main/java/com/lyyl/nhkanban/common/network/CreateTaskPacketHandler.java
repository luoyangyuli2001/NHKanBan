package com.lyyl.nhkanban.common.network;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.lyyl.nhkanban.NHKanBan;
import com.lyyl.nhkanban.common.gui.GuiContext;
import com.lyyl.nhkanban.common.gui.NHKanbanGui;
import com.lyyl.nhkanban.common.network.packet.CreateTaskPacket;
import com.lyyl.nhkanban.common.storage.TaskRepository;
import com.lyyl.nhkanban.common.task.Task;
import com.lyyl.nhkanban.common.task.TaskScope;
import com.lyyl.nhkanban.common.task.TaskType;
import com.lyyl.nhkanban.common.util.PlayerLookup;
import com.lyyl.nhkanban.common.util.ServerScheduler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * 服务端 handler。建任务必须在主线程做,因为 TaskRepository.put 内部走的
 * TaskWriteWorker 假定调用方是主线程,且 NHKanbanGui.openMainBoard 必须主线程。
 *
 * <p>
 * 所有字段都从 packet 重新校验:不能信任客户端发来的 title 长度、claimers
 * 范围、target username 是否真实存在。校验失败直接 drop。
 */
public class CreateTaskPacketHandler implements IMessageHandler<CreateTaskPacket, IMessage> {

    private static final double MAX_INTERACT_SQ = 64.0D;

    @Override
    public IMessage onMessage(final CreateTaskPacket msg, final MessageContext ctx) {
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        ServerScheduler.schedule(new Runnable() {

            @Override
            public void run() {
                handleOnMainThread(player, msg);
            }
        });
        return null;
    }

    private void handleOnMainThread(EntityPlayerMP player, CreateTaskPacket msg) {
        if (player == null || player.worldObj == null) return;

        GuiContext gctx = msg.getCtx();
        if (gctx == null) return;

        // 方块来源做距离校验。道具来源没有坐标可比,跳过。
        if (!gctx.isFromItem()) {
            double dx = gctx.getX() + 0.5 - player.posX;
            double dy = gctx.getY() + 0.5 - player.posY;
            double dz = gctx.getZ() + 0.5 - player.posZ;
            if (dx * dx + dy * dy + dz * dz > MAX_INTERACT_SQ) return;
        }

        // 字段校验
        String title = msg.getTitle();
        if (title == null) title = "";
        title = title.trim();
        if (title.isEmpty()) {
            NHKanBan.LOG.debug("CreateTaskPacket rejected: empty title from {}", player.getCommandSenderName());
            return;
        }
        if (title.length() > Task.MAX_TITLE_LENGTH) {
            title = title.substring(0, Task.MAX_TITLE_LENGTH);
        }
        String description = msg.getDescription();
        if (description == null) description = "";
        if (description.length() > Task.MAX_DESCRIPTION_LENGTH) {
            description = description.substring(0, Task.MAX_DESCRIPTION_LENGTH);
        }
        int maxClaimers = msg.getMaxClaimers();
        if (maxClaimers < 1) maxClaimers = 1;
        if (maxClaimers > 64) maxClaimers = 64;

        // DIRECT 必须有目标玩家
        UUID targetPlayer = null;
        if (msg.getScope() == TaskScope.DIRECT) {
            targetPlayer = PlayerLookup.resolveByName(msg.getTargetUsername());
            if (targetPlayer == null) {
                NHKanBan.LOG.debug(
                    "CreateTaskPacket rejected: DIRECT task with unresolvable target '{}' from {}",
                    msg.getTargetUsername(),
                    player.getCommandSenderName());
                return;
            }
        }

        Task task = Task.create(
            player.getUniqueID(),
            title,
            description,
            msg.getScope(),
            TaskType.OPEN,
            msg.getPriority(),
            maxClaimers);
        task.setTargetPlayer(targetPlayer);
        TaskRepository.get()
            .put(task);

        // 写完立刻给玩家重开 GUI,让新任务出现在列表里。
        NHKanbanGui.openMainBoard(player, gctx, ViewTab.MINE);
    }
}
