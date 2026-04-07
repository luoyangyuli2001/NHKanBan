package com.lyyl.nhkanban.client.ui.widget;

import java.util.Collections;
import java.util.List;

import com.lyyl.nhkanban.common.network.TaskSummary;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Column;

/**
 * 看板的一列。结构:列标题(固定高度) + 滚动卡片列表(占满剩余空间)。
 *
 * <p>滚动用 ListWidget 完成。注意必须用批量 children(iterable, factory) API
 * 一次性添加所有卡片,不能用 child(widget) 在循环里逐个添加——后者会触发
 * ListWidget 的尺寸计算在子项 widthRel 解析之前发生,导致卡片宽度退化成
 * 内容自适应(中文字符竖排)。批量 API 走的是另一条 init 路径,可以正确
 * 传播宽度。这是 Step 5b 翻车的真正原因,绕了一大圈才搞清。
 *
 * <p>setter 顺序也有讲究:widthRel → expanded → children,跟 MUI2 test code
 * 里的工作示例完全一致。颠倒顺序可能再次触发 sizing 计算时机问题。
 *
 * <p>cards 允许为 null 或空——空列只显示标题,内部 list 仍然存在但没子项。
 */
public final class KanbanColumnWidget {

    private static final int HEADER_HEIGHT = 12;

    /**
     * 卡片列表区的固定高度。本应是"父容器高度 - header 高度"动态算出,
     * 但 ListWidget.canCoverByDefaultSize(Y) 返回 false,且在我们当前的
     * 嵌套层级里 expanded() 拿不到正确的剩余空间(KanbanColumn 同时有
     * expanded 和 heightRel(1f) 互相打架),最终退化成单卡片高。
     *
     * 硬编码值的来历:面板高 220 - top row 14 - root padding 8 - col padding 4
     * - header 12 ≈ 182,取 180 留 2px 余量。
     *
     * 如果以后改 TaskBoardPanel 的 PANEL_HEIGHT,这里要同步改。
     */
    private static final int LIST_HEIGHT = 180;

    private KanbanColumnWidget() {}

    public static Column build(String title, List<TaskSummary> cards) {
        Column col = new Column();
        col.padding(2);

        TextWidget<?> header = new TextWidget<>(IKey.str(title)).height(HEADER_HEIGHT);
        col.child(header);

        List<TaskSummary> safe = cards == null ? Collections.<TaskSummary>emptyList() : cards;

        ListWidget<IWidget, ?> list = new ListWidget<>();
        list.widthRel(1f)
            .height(LIST_HEIGHT)
            .children(safe, t -> TaskCardWidget.build(t));
        col.child(list);

        return col;
    }
}
