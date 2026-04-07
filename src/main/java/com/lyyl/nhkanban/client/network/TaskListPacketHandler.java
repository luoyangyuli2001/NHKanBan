package com.lyyl.nhkanban.client.network;

import com.lyyl.nhkanban.NHKanBan;
import com.lyyl.nhkanban.client.cache.ClientTaskCache;
import com.lyyl.nhkanban.common.network.packet.TaskListPacket;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 把 TaskListPacket 写入 ClientTaskCache。
 *
 * <p>
 * 1.7.10 SimpleNetworkWrapper 的 handler 默认在网络线程执行,不是主线程。
 * 当前实现只写一个 synchronized cache,无 GL/世界访问,所以不需要调度回主线程。
 * 后续如果 handler 要触发 GUI 重建之类的主线程操作,记得用
 * Minecraft.getMinecraft().addScheduledTask(...) 包一层。
 */
@SideOnly(Side.CLIENT)
public class TaskListPacketHandler implements IMessageHandler<TaskListPacket, IMessage> {

    @Override
    public IMessage onMessage(TaskListPacket message, MessageContext ctx) {
        ClientTaskCache.get()
            .update(message.getTab(), message.getSummaries());
        if (NHKanBan.LOG.isDebugEnabled()) {
            NHKanBan.LOG.debug(
                "TaskListPacket received: tab={} count={}",
                message.getTab(),
                message.getSummaries()
                    .size());
        }
        return null;
    }
}
