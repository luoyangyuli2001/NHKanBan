package com.lyyl.nhkanban.common.network;

import java.util.UUID;

import com.lyyl.nhkanban.common.task.Task;
import com.lyyl.nhkanban.common.task.TaskPriority;
import com.lyyl.nhkanban.common.task.TaskStatus;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

/**
 * 列表视图用的任务投影,只含卡片渲染必需字段,避免列表请求拉全量 Task
 * (描述、评论、claimer 列表)浪费带宽。
 *
 * <p>
 * 详情视图走另外的完整 Task 包,与本类无关。扩字段时记得同步升 packet 版本。
 */
public final class TaskSummary {

    public final UUID uuid;
    public final String title;
    public final TaskStatus status;
    public final TaskPriority priority;
    /** null 表示无截止 */
    public final Long deadline;
    public final int claimerCount;
    public final int maxClaimers;
    public final int version;

    public TaskSummary(UUID uuid, String title, TaskStatus status, TaskPriority priority, Long deadline,
        int claimerCount, int maxClaimers, int version) {
        this.uuid = uuid;
        this.title = title;
        this.status = status;
        this.priority = priority;
        this.deadline = deadline;
        this.claimerCount = claimerCount;
        this.maxClaimers = maxClaimers;
        this.version = version;
    }

    public static TaskSummary from(Task t) {
        return new TaskSummary(
            t.getUuid(),
            t.getTitle(),
            t.getStatus(),
            t.getPriority(),
            t.getDeadline(),
            t.getClaimers()
                .size(),
            t.getMaxClaimers(),
            t.getVersion());
    }

    public void writeTo(ByteBuf buf) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        ByteBufUtils.writeUTF8String(buf, title == null ? "" : title);
        buf.writeByte(status.ordinal());
        buf.writeByte(priority.ordinal());
        buf.writeBoolean(deadline != null);
        if (deadline != null) {
            buf.writeLong(deadline);
        }
        buf.writeShort(claimerCount);
        buf.writeShort(maxClaimers);
        buf.writeInt(version);
    }

    public static TaskSummary readFrom(ByteBuf buf) {
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        String title = ByteBufUtils.readUTF8String(buf);
        TaskStatus status = TaskStatus.values()[buf.readByte()];
        TaskPriority priority = TaskPriority.values()[buf.readByte()];
        Long deadline = buf.readBoolean() ? buf.readLong() : null;
        int claimerCount = buf.readShort();
        int maxClaimers = buf.readShort();
        int version = buf.readInt();
        return new TaskSummary(uuid, title, status, priority, deadline, claimerCount, maxClaimers, version);
    }
}
