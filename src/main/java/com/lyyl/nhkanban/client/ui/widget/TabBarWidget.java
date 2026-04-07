package com.lyyl.nhkanban.client.ui.widget;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.lyyl.nhkanban.client.cache.ClientTaskCache;
import com.lyyl.nhkanban.common.gui.GuiContext;
import com.lyyl.nhkanban.common.network.NHKanbanNet;
import com.lyyl.nhkanban.common.network.ViewTab;
import com.lyyl.nhkanban.common.network.packet.RequestTaskListPacket;

/**
 * 看板顶部 Tab 切换栏。横向 4 个按钮,点击发送 C→S 请求,服务端响应后会
 * 用新 Tab 数据重开 GUI(走 NHKanbanGui.openMainBoard 同一个入口)。
 *
 * <p>
 * 当前活动 Tab 通过对比 ClientTaskCache.currentTab 决定,文字加黄色高亮。
 * 这是构建期判定,不是运行时绑定——切换 Tab 后整个 panel 会被重建,所以
 * 高亮自然刷新,不需要 widget 状态同步。
 *
 * <p>
 * GuiContext 通过构建时参数透传下来,用于 RequestTaskListPacket 告诉
 * 服务端该用方块路径还是道具路径重开 GUI。
 */
public final class TabBarWidget {

    private static final int BAR_HEIGHT = 14;

    private TabBarWidget() {}

    public static Row build(GuiContext ctx) {
        ViewTab active = ClientTaskCache.get()
            .getCurrentTab();

        Row bar = new Row();
        bar.widthRel(1f)
            .height(BAR_HEIGHT);

        bar.child(buildButton("公共", ViewTab.PUBLIC, active, ctx));
        bar.child(buildButton("我的", ViewTab.MINE, active, ctx));
        bar.child(buildButton("收件箱", ViewTab.INBOX, active, ctx));
        bar.child(buildButton("归档", ViewTab.ARCHIVE, active, ctx));

        return bar;
    }

    private static ButtonWidget<?> buildButton(String label, final ViewTab tab, ViewTab active, final GuiContext ctx) {
        String colored = (tab == active ? EnumChatFormatting.YELLOW.toString() : EnumChatFormatting.WHITE.toString())
            + label;
        ButtonWidget<?> btn = new ButtonWidget<>();
        btn.expanded()
            .heightRel(1f)
            .overlay(IKey.str(colored))
            .onMousePressed(mouseButton -> {
                if (mouseButton == 0) {
                    NHKanbanNet.CHANNEL.sendToServer(new RequestTaskListPacket(tab, ctx));
                    return true;
                }
                return false;
            });
        return btn;
    }
}
