package com.lyyl.nhkanban.client.cache;

import java.util.Collections;
import java.util.List;

import com.lyyl.nhkanban.common.network.TaskSummary;
import com.lyyl.nhkanban.common.network.ViewTab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 客户端唯一的任务数据来源。Widget 只能从这里读,严禁绕过去访问 TaskRepository
 * ——后者是服务端 singleton,客户端 JVM 上永远是空的,在专用服上会渲染出空看板。
 *
 * <p>
 * 只由网络包 handler 写入,Widget 只读。不持久化,关闭客户端即丢失;每次开 GUI
 * 服务端都会主动推一份新的,不需要 TTL。
 *
 * <p>
 * handler 在网络线程写、GUI 在主线程读,所以读写都加 synchronized。getTasks
 * 返回不可变拷贝,避免读迭代时被并发替换。
 */
@SideOnly(Side.CLIENT)
public final class ClientTaskCache {

    private static final ClientTaskCache INSTANCE = new ClientTaskCache();

    public static ClientTaskCache get() {
        return INSTANCE;
    }

    private ViewTab currentTab = ViewTab.PUBLIC;
    private List<TaskSummary> tasks = Collections.emptyList();
    private long lastUpdatedAt = 0L;

    private ClientTaskCache() {}

    public synchronized void update(ViewTab tab, List<TaskSummary> summaries) {
        this.currentTab = tab;
        this.tasks = Collections.unmodifiableList(summaries);
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public synchronized ViewTab getCurrentTab() {
        return currentTab;
    }

    public synchronized List<TaskSummary> getTasks() {
        return tasks;
    }

    public synchronized long getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    /** 退出世界时清空,避免下次进新世界看到旧数据。 */
    public synchronized void clear() {
        this.tasks = Collections.emptyList();
        this.currentTab = ViewTab.PUBLIC;
        this.lastUpdatedAt = 0L;
    }
}
