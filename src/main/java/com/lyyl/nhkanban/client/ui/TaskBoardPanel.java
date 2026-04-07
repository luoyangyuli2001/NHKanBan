package com.lyyl.nhkanban.client.ui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.lyyl.nhkanban.client.cache.ClientTaskCache;
import com.lyyl.nhkanban.client.ui.widget.KanbanColumnWidget;
import com.lyyl.nhkanban.client.ui.widget.TabBarWidget;
import com.lyyl.nhkanban.common.gui.GuiContext;
import com.lyyl.nhkanban.common.network.TaskSummary;
import com.lyyl.nhkanban.common.task.TaskStatus;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

/**
 * 任务板主面板。结构:顶部 TabBar + "新建"按钮 + 下方 4 列看板。
 *
 * <p>
 * "新建"按钮通过 IPanelHandler.simple 注册一个子面板,只在客户端创建——
 * simple() 在服务端调会 crash。服务端构建 panel 时 formHandler 保持 null,
 * 反正 onMousePressed lambda 永远不会在服务端被触发,所以服务端持有 null
 * 是安全的。
 *
 * <p>
 * build() 在 MUI2 的双端机制下两侧都会跑。服务端跑时 ClientTaskCache 是空的,
 * 客户端跑时 cache 已经被服务端在 open 之前推过来的 TaskListPacket 写满。
 */
public final class TaskBoardPanel {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 220;

    private TaskBoardPanel() {}

    public static ModularPanel build(final GuiContext ctx) {
        final ModularPanel panel = ModularPanel.defaultPanel("nhkanban_board", PANEL_WIDTH, PANEL_HEIGHT);

        final IPanelHandler[] formHandler = { null };
        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.CLIENT) {
            formHandler[0] = IPanelHandler
                .simple(panel, (parent, player) -> TaskCreatePanel.build(formHandler[0], ctx), true);
        }

        Map<TaskStatus, List<TaskSummary>> bucketed = bucketByStatus(
            ClientTaskCache.get()
                .getTasks());

        Column root = new Column();
        root.sizeRel(1f, 1f)
            .padding(4);

        Row top = new Row();
        top.widthRel(1f)
            .height(14);

        Row tabBar = TabBarWidget.build(ctx);
        tabBar.expanded()
            .heightRel(1f);
        top.child(tabBar);

        ButtonWidget<?> createOpenBtn = new ButtonWidget<>();
        createOpenBtn.width(40)
            .heightRel(1f)
            .overlay(IKey.str("+ 新建"))
            .onMousePressed(mouseButton -> {
                if (mouseButton == 0 && formHandler[0] != null) {
                    formHandler[0].openPanel();
                    return true;
                }
                return false;
            });
        top.child(createOpenBtn);

        root.child(top);

        Row board = new Row();
        board.widthRel(1f)
            .expanded();

        Column open = KanbanColumnWidget.build("待领取", bucketed.get(TaskStatus.OPEN));
        open.expanded()
            .heightRel(1f);
        board.child(open);

        Column claimed = KanbanColumnWidget.build("进行中", bucketed.get(TaskStatus.CLAIMED));
        claimed.expanded()
            .heightRel(1f);
        board.child(claimed);

        Column submitted = KanbanColumnWidget.build("待审核", bucketed.get(TaskStatus.SUBMITTED));
        submitted.expanded()
            .heightRel(1f);
        board.child(submitted);

        Column completed = KanbanColumnWidget.build("已完成", bucketed.get(TaskStatus.COMPLETED));
        completed.expanded()
            .heightRel(1f);
        board.child(completed);

        root.child(board);
        panel.child(root);
        return panel;
    }

    private static Map<TaskStatus, List<TaskSummary>> bucketByStatus(List<TaskSummary> tasks) {
        Map<TaskStatus, List<TaskSummary>> map = new EnumMap<TaskStatus, List<TaskSummary>>(TaskStatus.class);
        map.put(TaskStatus.OPEN, new ArrayList<TaskSummary>());
        map.put(TaskStatus.CLAIMED, new ArrayList<TaskSummary>());
        map.put(TaskStatus.SUBMITTED, new ArrayList<TaskSummary>());
        map.put(TaskStatus.COMPLETED, new ArrayList<TaskSummary>());
        for (TaskSummary t : tasks) {
            List<TaskSummary> bucket = map.get(t.status);
            if (bucket != null) {
                bucket.add(t);
            }
        }
        return map;
    }
}
