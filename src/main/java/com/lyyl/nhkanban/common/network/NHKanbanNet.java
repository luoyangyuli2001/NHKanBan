package com.lyyl.nhkanban.common.network;

import com.lyyl.nhkanban.NHKanBan;
import com.lyyl.nhkanban.client.network.TaskListPacketHandler;
import com.lyyl.nhkanban.common.network.packet.CreateTaskPacket;
import com.lyyl.nhkanban.common.network.packet.RequestTaskListPacket;
import com.lyyl.nhkanban.common.network.packet.TaskListPacket;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/**
 * 全 mod 唯一的 SimpleNetworkWrapper 通道,所有业务包从这里收发。
 *
 * <p>
 * 注册分两个入口,因为 S → C 的 handler 通常会引用 client-only 类
 * (Minecraft、客户端缓存等),在专用服 JVM 上加载会 NoClassDefFoundError:
 * <ul>
 * <li>{@link #registerCommonPackets()} —— 双端都安全,在 CommonProxy 调</li>
 * <li>{@link #registerClientPackets()} —— 仅客户端调,在 ClientProxy 调</li>
 * </ul>
 *
 * <p>
 * discriminator 走单调计数器,跨方法共享同一个 nextId,避免两侧手写 ID 冲突。
 */
public final class NHKanbanNet {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(NHKanBan.MODID);

    private static int nextId = 0;

    private NHKanbanNet() {}

    public static int nextId() {
        return nextId++;
    }

    public static void registerCommonPackets() {
        CHANNEL.registerMessage(RequestTaskListPacketHandler.class, RequestTaskListPacket.class, nextId(), Side.SERVER);
        CHANNEL.registerMessage(CreateTaskPacketHandler.class, CreateTaskPacket.class, nextId(), Side.SERVER);
    }

    public static void registerClientPackets() {
        CHANNEL.registerMessage(TaskListPacketHandler.class, TaskListPacket.class, nextId(), Side.CLIENT);
    }
}
