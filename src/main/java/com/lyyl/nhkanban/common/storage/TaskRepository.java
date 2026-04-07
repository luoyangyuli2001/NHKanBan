package com.lyyl.nhkanban.common.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.lyyl.nhkanban.common.task.Task;
import com.lyyl.nhkanban.common.task.TaskStatus;

/**
 * 内存中的任务索引,所有查询的真相来源。
 *
 * 写操作通过 {@link #put(Task)} 进入,会自动:
 * 1. 更新内存索引
 * 2. 提交到 {@link TaskStorage} 异步落盘
 *
 * 这是单例,服务端全局唯一。
 */
public class TaskRepository {

    private static final TaskRepository INSTANCE = new TaskRepository();

    public static TaskRepository get() {
        return INSTANCE;
    }

    /** uuid -> task */
    private final ConcurrentHashMap<UUID, Task> tasks = new ConcurrentHashMap<UUID, Task>();

    /** 由 TaskStorage 在启动时调用,批量加载已有任务 */
    void loadAll(Collection<Task> loaded) {
        tasks.clear();
        for (Task t : loaded) {
            tasks.put(t.getUuid(), t);
        }
    }

    /** 由 TaskStorage 在关服时调用,清空内存 */
    void clear() {
        tasks.clear();
    }

    // --- 查询 API ---

    /** 按 uuid 查找,不存在返回 null */
    public Task find(UUID uuid) {
        return tasks.get(uuid);
    }

    /** 是否存在 */
    public boolean exists(UUID uuid) {
        return tasks.containsKey(uuid);
    }

    /** 当前活跃任务总数 */
    public int size() {
        return tasks.size();
    }

    /** 获取所有任务的不可变快照(用于遍历筛选) */
    public List<Task> all() {
        return new ArrayList<Task>(tasks.values());
    }

    /** 按状态筛选 */
    public List<Task> findByStatus(TaskStatus status) {
        List<Task> result = new ArrayList<Task>();
        for (Task t : tasks.values()) {
            if (t.getStatus() == status) {
                result.add(t);
            }
        }
        return result;
    }

    /** 按发布者筛选 */
    public List<Task> findByAuthor(UUID author) {
        List<Task> result = new ArrayList<Task>();
        for (Task t : tasks.values()) {
            if (author.equals(t.getAuthor())) {
                result.add(t);
            }
        }
        return result;
    }

    /** 按领取者筛选 */
    public List<Task> findByClaimer(UUID claimer) {
        List<Task> result = new ArrayList<Task>();
        for (Task t : tasks.values()) {
            if (t.getClaimers()
                .contains(claimer)) {
                result.add(t);
            }
        }
        return result;
    }

    /** 按 DIRECT 任务的 target 玩家筛选,只返回 scope=DIRECT 的任务 */
    public List<Task> findByTarget(UUID target) {
        List<Task> result = new ArrayList<Task>();
        for (Task t : tasks.values()) {
            if (target.equals(t.getTargetPlayer())) {
                result.add(t);
            }
        }
        return result;
    }

    /** 某玩家发布的活跃(非终态)任务数,用于配额校验 */
    public int countActiveByAuthor(UUID author) {
        int count = 0;
        for (Task t : tasks.values()) {
            if (author.equals(t.getAuthor()) && !t.getStatus()
                .isTerminal()) {
                count++;
            }
        }
        return count;
    }

    // --- 写操作 ---

    /**
     * 新增或更新任务。
     * 调用方应已经修改完字段并 bumpVersion()。
     * 此方法会触发异步落盘。
     */
    public void put(Task task) {
        if (task == null || task.getUuid() == null) {
            throw new IllegalArgumentException("Task or uuid is null");
        }
        tasks.put(task.getUuid(), task);
        TaskStorage.get()
            .scheduleWrite(task);
    }

    /**
     * 从活跃集中删除并归档。
     * 实际归档移动由 TaskStorage 处理。
     */
    public void archive(UUID uuid) {
        Task removed = tasks.remove(uuid);
        if (removed != null) {
            TaskStorage.get()
                .scheduleArchive(removed);
        }
    }

    /** 仅供测试或重置:不归档,直接从内存和磁盘删除 */
    public void deleteHard(UUID uuid) {
        Task removed = tasks.remove(uuid);
        if (removed != null) {
            TaskStorage.get()
                .scheduleDelete(uuid);
        }
    }
}
