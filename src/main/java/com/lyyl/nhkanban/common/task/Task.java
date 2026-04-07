package com.lyyl.nhkanban.common.task;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Task {

    /** 描述长度上限,与网络包大小约束一致 */
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    /** 标题长度上限 */
    public static final int MAX_TITLE_LENGTH = 100;

    private UUID uuid;
    private String title;
    private String description;

    private UUID author;
    private long createdAt;
    /** 可空,null 表示无截止 */
    private Long deadline;

    /** 可空,null 表示根任务 */
    private UUID parentUuid;

    private TaskScope scope;
    private TaskType type;
    private TaskPriority priority;

    private int maxClaimers;
    private TaskStatus status;

    private List<String> tags = new ArrayList<String>();
    private List<UUID> claimers = new ArrayList<UUID>();
    /** 仅 DIRECT 任务有意义,其他 scope 为 null */
    private UUID targetPlayer;
    private List<Comment> comments = new ArrayList<Comment>();

    /** 乐观锁,每次写 +1 */
    private int version;

    /** Gson 反序列化用 */
    public Task() {}

    /** 创建新任务的便捷工厂 */
    public static Task create(UUID author, String title, String description, TaskScope scope, TaskType type,
        TaskPriority priority, int maxClaimers) {
        Task t = new Task();
        t.uuid = UUID.randomUUID();
        t.title = title;
        t.description = description;
        t.author = author;
        t.createdAt = System.currentTimeMillis();
        t.scope = scope;
        t.type = type;
        t.priority = priority;
        t.maxClaimers = maxClaimers;
        t.status = TaskStatus.OPEN;
        t.version = 0;
        return t;
    }

    // --- getters / setters ---

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getAuthor() {
        return author;
    }

    public void setAuthor(UUID author) {
        this.author = author;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public UUID getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(UUID parentUuid) {
        this.parentUuid = parentUuid;
    }

    public TaskScope getScope() {
        return scope;
    }

    public void setScope(TaskScope scope) {
        this.scope = scope;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public int getMaxClaimers() {
        return maxClaimers;
    }

    public void setMaxClaimers(int maxClaimers) {
        this.maxClaimers = maxClaimers;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<UUID> getClaimers() {
        return claimers;
    }

    public void setClaimers(List<UUID> claimers) {
        this.claimers = claimers;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public UUID getTargetPlayer() {
        return targetPlayer;
    }

    public void setTargetPlayer(UUID targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /** 写操作完成后调用,版本号自增 */
    public void bumpVersion() {
        this.version++;
    }
}
