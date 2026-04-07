package com.lyyl.nhkanban.client.ui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.lyyl.nhkanban.client.cache.ClientTaskCache;

/**
 * 任务板主面板。当前是 M2 最小骨架:一行动态文字显示 cache 中任务数,
 * 用来验证"推数据 → open → cache 命中 → widget 渲染"整条管线打通。
 *
 * <p>
 * build() 会被 TileTaskBoard.buildUI 在双端调用。直接引用 ClientTaskCache
 * (它是 client-only)看似危险,但 IKey.dynamic 的 supplier 只在 widget 实际
 * 渲染时被调用,而渲染只发生在客户端,所以 cache 类的解析也只发生在客户端。
 * 服务端走完 build() 不会触碰 supplier,因此不会触发 ClientTaskCache 类加载。
 *
 * <p>
 * 不要把读取 cache 的逻辑提到 build() 主体里——那样服务端调用 build() 时
 * 就会立即触发 ClientTaskCache 解析,在专用服 JVM 上 NoClassDefFoundError。
 */
public final class TaskBoardPanel {

    private TaskBoardPanel() {}

    public static ModularPanel build() {
        ModularPanel panel = ModularPanel.defaultPanel("nhkanban:board", 200, 80);
        TextWidget<?> label = new TextWidget<>(IKey.dynamic(() -> {
            int n = ClientTaskCache.get()
                .getTasks()
                .size();
            return "NHKanban — " + n + " tasks";
        })).pos(10, 10);
        panel.child(label);
        return panel;
    }
}
