package com.lyyl.nhkanban.common.network.packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lyyl.nhkanban.common.network.TaskSummary;
import com.lyyl.nhkanban.common.network.ViewTab;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/**
 * S → C:把某个 Tab 当前的任务列表完整推给单个玩家。
 *
 * <p>
 * 不带分页字段,服务端按固定上限裁剪。真要分页时再加 page/total 并升包版本。
 */
public class TaskListPacket implements IMessage {

    private ViewTab tab;
    private List<TaskSummary> summaries;

    /** Forge 反射构造,不要删。 */
    public TaskListPacket() {
        this.tab = ViewTab.PUBLIC;
        this.summaries = Collections.emptyList();
    }

    public TaskListPacket(ViewTab tab, List<TaskSummary> summaries) {
        this.tab = tab;
        this.summaries = summaries;
    }

    public ViewTab getTab() {
        return tab;
    }

    public List<TaskSummary> getSummaries() {
        return summaries;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.tab = ViewTab.fromOrdinal(buf.readByte());
        int n = buf.readShort();
        List<TaskSummary> list = new ArrayList<TaskSummary>(n);
        for (int i = 0; i < n; i++) {
            list.add(TaskSummary.readFrom(buf));
        }
        this.summaries = list;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(tab.ordinal());
        buf.writeShort(summaries.size());
        for (TaskSummary s : summaries) {
            s.writeTo(buf);
        }
    }
}
