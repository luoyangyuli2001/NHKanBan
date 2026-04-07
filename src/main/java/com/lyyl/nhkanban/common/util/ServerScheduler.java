package com.lyyl.nhkanban.common.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.lyyl.nhkanban.NHKanBan;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * 把任意线程上的任务延迟到主服务端线程执行。
 *
 * <p>
 * 1.7.10 的 vanilla MinecraftServer 没有 addScheduledTask(那是 1.8+ 的 API),
 * 所以走这套自建队列:任意线程往队列里塞 Runnable,ServerTickEvent.START 阶段
 * 排干执行。每帧最多吃光当前队列,不留隔夜任务。
 *
 * <p>
 * 典型用法:网络包 handler 在网络线程接收数据,把"实际处理"包成 Runnable 推
 * 进队列,确保所有 player 状态修改、容器打开、世界访问都跑在主线程上。
 *
 * <p>
 * 不做异常隔离会拖垮整个 tick 队列,所以每个 task 单独 try-catch 并打日志。
 */
public final class ServerScheduler {

    private static final Queue<Runnable> QUEUE = new ConcurrentLinkedQueue<Runnable>();
    private static boolean registered = false;

    private ServerScheduler() {}

    public static void register() {
        if (registered) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new ServerScheduler());
        registered = true;
    }

    public static void schedule(Runnable task) {
        if (task != null) {
            QUEUE.add(task);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Runnable r;
        while ((r = QUEUE.poll()) != null) {
            try {
                r.run();
            } catch (Throwable t) {
                NHKanBan.LOG.error("Scheduled task threw", t);
            }
        }
    }
}
