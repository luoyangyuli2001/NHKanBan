package com.lyyl.nhkanban.common.network.packet;

import com.lyyl.nhkanban.common.gui.GuiContext;
import com.lyyl.nhkanban.common.network.ViewTab;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * C → S:玩家在 GUI 内点击 Tab,请求服务端重新查询并推送对应 Tab 的列表。
 *
 * <p>
 * 携带 GuiContext 是为了让服务端知道用方块路径还是道具路径重开 GUI——
 * 服务端不维护"玩家当前正在看哪个 GUI"的状态,所有上下文从客户端发回。
 */
public class RequestTaskListPacket implements IMessage {

    private ViewTab tab;
    private GuiContext ctx;

    public RequestTaskListPacket() {
        this.tab = ViewTab.PUBLIC;
        this.ctx = GuiContext.forItem();
    }

    public RequestTaskListPacket(ViewTab tab, GuiContext ctx) {
        this.tab = tab;
        this.ctx = ctx;
    }

    public ViewTab getTab() {
        return tab;
    }

    public GuiContext getCtx() {
        return ctx;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.tab = ViewTab.fromOrdinal(buf.readByte());
        this.ctx = GuiContext.readFrom(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(tab.ordinal());
        ctx.writeTo(buf);
    }
}
