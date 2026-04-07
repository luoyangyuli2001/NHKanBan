package com.lyyl.nhkanban.common.network.packet;

import com.lyyl.nhkanban.common.gui.GuiContext;
import com.lyyl.nhkanban.common.task.TaskPriority;
import com.lyyl.nhkanban.common.task.TaskScope;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * C → S:玩家在创建表单点击"创建",请求服务端建一个新任务。
 *
 * <p>
 * GuiContext 用于服务端在创建后用 NHKanbanGui.openMainBoard 重新打开 GUI,
 * 让玩家立刻看到自己刚建的任务出现在列表里。来源(方块/道具)决定 reopen
 * 走哪条 MUI2 工厂路径。
 *
 * <p>
 * targetUsername 仅在 scope=DIRECT 时有意义,其他 scope 应为空字符串。
 * 传 username 而不是 UUID 是因为客户端不知道目标玩家的 UUID,服务端解析。
 */
public class CreateTaskPacket implements IMessage {

    private String title;
    private String description;
    private TaskScope scope;
    private TaskPriority priority;
    private int maxClaimers;
    private String targetUsername;
    private GuiContext ctx;

    public CreateTaskPacket() {
        this.title = "";
        this.description = "";
        this.scope = TaskScope.TEAM;
        this.priority = TaskPriority.NORMAL;
        this.maxClaimers = 3;
        this.targetUsername = "";
        this.ctx = GuiContext.forItem();
    }

    public CreateTaskPacket(String title, String description, TaskScope scope, TaskPriority priority, int maxClaimers,
        String targetUsername, GuiContext ctx) {
        this.title = title;
        this.description = description;
        this.scope = scope;
        this.priority = priority;
        this.maxClaimers = maxClaimers;
        this.targetUsername = targetUsername;
        this.ctx = ctx;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskScope getScope() {
        return scope;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public int getMaxClaimers() {
        return maxClaimers;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public GuiContext getCtx() {
        return ctx;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.title = ByteBufUtils.readUTF8String(buf);
        this.description = ByteBufUtils.readUTF8String(buf);
        this.scope = TaskScope.values()[buf.readByte()];
        this.priority = TaskPriority.values()[buf.readByte()];
        this.maxClaimers = buf.readShort();
        this.targetUsername = ByteBufUtils.readUTF8String(buf);
        this.ctx = GuiContext.readFrom(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, title == null ? "" : title);
        ByteBufUtils.writeUTF8String(buf, description == null ? "" : description);
        buf.writeByte(scope.ordinal());
        buf.writeByte(priority.ordinal());
        buf.writeShort(maxClaimers);
        ByteBufUtils.writeUTF8String(buf, targetUsername == null ? "" : targetUsername);
        ctx.writeTo(buf);
    }
}
