package com.lyyl.nhkanban.common.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.lyyl.nhkanban.common.gui.GuiContext;
import com.lyyl.nhkanban.common.gui.NHKanbanGui;
import com.lyyl.nhkanban.common.network.packet.RequestTaskListPacket;
import com.lyyl.nhkanban.common.util.ServerScheduler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * 服务端 handler。包到达时在网络线程,这里把实际处理调度到主线程,
 * 因为 NHKanbanGui.openMainBoard 内部走 MUI2 的 GuiManager.open,
 * 后者会修改 player.openContainer 等共享状态,只能在主线程操作。
 *
 * <p>
 * 对方块来源做距离校验,防止恶意客户端伪造 packet 远程操作任意坐标。
 * 道具来源不需要——道具 GUI 是绑定到当前手持物的,客户端不可能让服务端
 * "替别人开 GUI"。
 */
public class RequestTaskListPacketHandler implements IMessageHandler<RequestTaskListPacket, IMessage> {

    private static final double MAX_INTERACT_SQ = 64.0D;

    @Override
    public IMessage onMessage(final RequestTaskListPacket msg, final MessageContext ctx) {
        final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        ServerScheduler.schedule(new Runnable() {

            @Override
            public void run() {
                handleOnMainThread(player, msg);
            }
        });
        return null;
    }

    private void handleOnMainThread(EntityPlayerMP player, RequestTaskListPacket msg) {
        if (player == null) return;
        GuiContext gctx = msg.getCtx();
        if (gctx == null) return;

        if (!gctx.isFromItem()) {
            if (player.worldObj == null) return;
            double dx = gctx.getX() + 0.5 - player.posX;
            double dy = gctx.getY() + 0.5 - player.posY;
            double dz = gctx.getZ() + 0.5 - player.posZ;
            if (dx * dx + dy * dy + dz * dz > MAX_INTERACT_SQ) return;
        }

        NHKanbanGui.openMainBoard(player, gctx, msg.getTab());
    }
}
