package com.lyyl.nhkanban.common.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lyyl.nhkanban.NHKanBan;
import com.lyyl.nhkanban.common.task.Task;

/**
 * 持久化总入口。负责:
 * - 启动时加载所有任务到 TaskRepository
 * - 提供写/归档/删除的调度入口(转发给 worker)
 * - 关服时刷新 worker 队列
 *
 * 文件布局:
 * <world>/nhkanban/
 * ├── tasks/<uuid>.json
 * └── archive/yyyy-MM/<uuid>.json
 */
public class TaskStorage {

    private static final TaskStorage INSTANCE = new TaskStorage();

    public static TaskStorage get() {
        return INSTANCE;
    }

    private File baseDir;
    private File tasksDir;
    private File archiveDir;
    private Gson gson;
    private TaskWriteWorker worker;
    private boolean initialized = false;

    /** 由 ServerStarting 事件调用,worldDir 通常是 DimensionManager.getCurrentSaveRootDirectory() */
    public synchronized void init(File worldDir) {
        if (initialized) {
            NHKanBan.LOG.warn("NHKanBan TaskStorage already initialized");
            return;
        }
        baseDir = new File(worldDir, "nhkanban");
        tasksDir = new File(baseDir, "tasks");
        archiveDir = new File(baseDir, "archive");
        ensureDir(baseDir);
        ensureDir(tasksDir);
        ensureDir(archiveDir);

        gson = new GsonBuilder().setPrettyPrinting()
            .create();

        worker = new TaskWriteWorker(this);

        loadAllTasks();
        initialized = true;
        NHKanBan.LOG.info("NHKanBan TaskStorage initialized at " + baseDir.getAbsolutePath());
    }

    /** 由 ServerStopping 事件调用 */
    public synchronized void shutdown() {
        if (!initialized) return;
        if (worker != null) {
            worker.shutdown();
            worker = null;
        }
        TaskRepository.get()
            .clear();
        initialized = false;
        NHKanBan.LOG.info("NHKanBan TaskStorage shut down");
    }

    public boolean isInitialized() {
        return initialized;
    }

    // --- 调度接口,供 TaskRepository 调用 ---

    void scheduleWrite(Task task) {
        if (!initialized) return;
        worker.scheduleWrite(task);
    }

    void scheduleArchive(Task task) {
        if (!initialized) return;
        worker.scheduleArchive(task);
    }

    void scheduleDelete(UUID uuid) {
        if (!initialized) return;
        worker.scheduleDelete(uuid);
    }

    // --- 实际文件 IO,在 worker 线程上执行 ---

    void writeTaskFile(Task task) throws IOException {
        File target = new File(
            tasksDir,
            task.getUuid()
                .toString() + ".json");
        File tmp = new File(
            tasksDir,
            task.getUuid()
                .toString() + ".json.tmp");
        String json = gson.toJson(task);
        Writer w = new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8");
        try {
            w.write(json);
        } finally {
            w.close();
        }
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    void writeArchiveFile(Task task) throws IOException {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM");
        String month = fmt.format(new Date());
        File monthDir = new File(archiveDir, month);
        ensureDir(monthDir);

        File target = new File(
            monthDir,
            task.getUuid()
                .toString() + ".json");
        File tmp = new File(
            monthDir,
            task.getUuid()
                .toString() + ".json.tmp");
        String json = gson.toJson(task);
        Writer w = new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8");
        try {
            w.write(json);
        } finally {
            w.close();
        }
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // 删除活跃目录中的副本
        File activeFile = new File(
            tasksDir,
            task.getUuid()
                .toString() + ".json");
        if (activeFile.exists() && !activeFile.delete()) {
            NHKanBan.LOG.warn("Failed to delete active file after archive: " + activeFile);
        }
    }

    void deleteTaskFile(UUID uuid) {
        File f = new File(tasksDir, uuid.toString() + ".json");
        if (f.exists() && !f.delete()) {
            NHKanBan.LOG.warn("Failed to delete task file: " + f);
        }
    }

    // --- 启动加载 ---

    private void loadAllTasks() {
        File[] files = tasksDir.listFiles();
        if (files == null || files.length == 0) {
            TaskRepository.get()
                .loadAll(Collections.<Task>emptyList());
            NHKanBan.LOG.info("NHKanBan: loaded 0 tasks (empty directory)");
            return;
        }
        List<Task> loaded = new ArrayList<Task>();
        int failed = 0;
        for (File f : files) {
            if (!f.isFile() || !f.getName()
                .endsWith(".json")) {
                continue;
            }
            try {
                Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
                try {
                    Task t = gson.fromJson(r, Task.class);
                    if (t != null && t.getUuid() != null) {
                        loaded.add(t);
                    } else {
                        failed++;
                        NHKanBan.LOG.warn("NHKanBan: skipped invalid task file " + f.getName());
                    }
                } finally {
                    r.close();
                }
            } catch (Exception e) {
                failed++;
                NHKanBan.LOG.error("NHKanBan: failed to load task file " + f.getName(), e);
            }
        }
        TaskRepository.get()
            .loadAll(loaded);
        NHKanBan.LOG.info("NHKanBan: loaded " + loaded.size() + " tasks (" + failed + " failed)");
    }

    private void ensureDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            NHKanBan.LOG.error("NHKanBan: failed to create directory " + dir.getAbsolutePath());
        }
    }
}
