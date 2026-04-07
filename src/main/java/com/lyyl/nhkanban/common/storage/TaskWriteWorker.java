package com.lyyl.nhkanban.common.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.lyyl.nhkanban.NHKanBan;
import com.lyyl.nhkanban.common.task.Task;

/**
 * 单线程写 worker。
 *
 * 职责:
 * 1. 异步执行所有磁盘 IO,不阻塞主线程
 * 2. 串行化所有写操作(并发控制第一层)
 * 3. 100ms 内同一 uuid 的多次写合并为一次落盘
 *
 * 写入分两类:
 * - 高频普通写(scheduleWrite):走 pending map + 定期 flush 实现合并
 * - 低频终态操作(scheduleArchive / scheduleDelete):立即提交到 worker
 */
class TaskWriteWorker {

    private static final long FLUSH_INTERVAL_MS = 100L;
    private static final long SHUTDOWN_AWAIT_SECONDS = 10L;

    private final TaskStorage storage;
    private final ScheduledExecutorService scheduler;

    /** 待落盘的任务,uuid -> 最新版本。后写覆盖前写,实现合并。 */
    private final Map<UUID, Task> pendingWrites = new HashMap<UUID, Task>();
    private final Object pendingLock = new Object();

    private volatile boolean shutdown = false;

    TaskWriteWorker(TaskStorage storage) {
        this.storage = storage;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "NHKanBan-Storage");
                t.setDaemon(true);
                return t;
            }
        });
        this.scheduler.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                try {
                    flushWrites();
                } catch (Throwable t) {
                    NHKanBan.LOG.error("NHKanBan storage flush error", t);
                }
            }
        }, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /** 提交一个普通写,会被合并 */
    void scheduleWrite(Task task) {
        if (shutdown) return;
        synchronized (pendingLock) {
            pendingWrites.put(task.getUuid(), task);
        }
    }

    /** 立即归档(不合并,因为是低频终态操作) */
    void scheduleArchive(final Task task) {
        if (shutdown) return;
        // 取消该 uuid 任何 pending 的普通写
        synchronized (pendingLock) {
            pendingWrites.remove(task.getUuid());
        }
        scheduler.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    storage.writeArchiveFile(task);
                } catch (Throwable t) {
                    NHKanBan.LOG.error("NHKanBan archive failed for " + task.getUuid(), t);
                }
            }
        });
    }

    /** 立即硬删除 */
    void scheduleDelete(final UUID uuid) {
        if (shutdown) return;
        synchronized (pendingLock) {
            pendingWrites.remove(uuid);
        }
        scheduler.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    storage.deleteTaskFile(uuid);
                } catch (Throwable t) {
                    NHKanBan.LOG.error("NHKanBan delete failed for " + uuid, t);
                }
            }
        });
    }

    /** 关服时调用,等待所有 pending 写完成 */
    void shutdown() {
        shutdown = true;
        // 最后一次 flush(同步执行,确保没遗漏)
        try {
            scheduler.submit(new Runnable() {

                @Override
                public void run() {
                    flushWrites();
                }
            })
                .get(SHUTDOWN_AWAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Throwable t) {
            NHKanBan.LOG.error("NHKanBan final flush failed", t);
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(SHUTDOWN_AWAIT_SECONDS, TimeUnit.SECONDS)) {
                NHKanBan.LOG.warn("NHKanBan storage worker did not terminate in time");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
            scheduler.shutdownNow();
        }
    }

    private void flushWrites() {
        List<Task> drained;
        synchronized (pendingLock) {
            if (pendingWrites.isEmpty()) {
                return;
            }
            drained = new ArrayList<Task>(pendingWrites.values());
            pendingWrites.clear();
        }
        for (Task t : drained) {
            try {
                storage.writeTaskFile(t);
            } catch (Throwable e) {
                NHKanBan.LOG.error("NHKanBan write failed for " + t.getUuid(), e);
            }
        }
    }
}
