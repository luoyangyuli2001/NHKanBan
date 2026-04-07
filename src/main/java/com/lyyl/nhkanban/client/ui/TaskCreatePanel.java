package com.lyyl.nhkanban.client.ui;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IPositioned;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Column;
import com.cleanroommc.modularui.widgets.layout.Row;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.lyyl.nhkanban.common.gui.GuiContext;
import com.lyyl.nhkanban.common.network.NHKanbanNet;
import com.lyyl.nhkanban.common.network.packet.CreateTaskPacket;
import com.lyyl.nhkanban.common.task.Task;
import com.lyyl.nhkanban.common.task.TaskPriority;
import com.lyyl.nhkanban.common.task.TaskScope;

/**
 * 创建任务的子面板。表单字段:标题 + 描述 + 优先级 + 作用域 + 人数 + 目标玩家。
 *
 * <p>
 * 不用 DropdownWidget——它强依赖 IValue<T> 即 sync handler,引入会破坏
 * "全 mod 不用 sync handler" 的统一心智模型。改用 ButtonWidget 自管状态:
 * 闭包持有枚举数组,点击递增 ordinal,显示文本通过 IKey.dynamic 每帧重算。
 *
 * <p>
 * 所有 TextFieldWidget 都要显式 acceptsExpressions(false),否则 MUI2 默认
 * 当数学表达式解析,普通文字会触发解析报错。
 *
 * <p>
 * 目标玩家字段始终显示,但只有 scope=DIRECT 时才会被服务端使用并校验。
 * 客户端不做"切换 scope 时显示/隐藏字段"的动态布局——MUI2 的动态可见性
 * 在 panel 已构建后改起来很别扭,而且总显示对用户更直观(看见有这个字段
 * 才知道有 DIRECT 这种用法)。
 */
public final class TaskCreatePanel {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 195;
    private static final int ROW_HEIGHT = 14;
    private static final int LABEL_WIDTH = 50;

    private TaskCreatePanel() {}

    public static ModularPanel build(final IPanelHandler self, final GuiContext ctx) {
        ModularPanel panel = ModularPanel.defaultPanel("nhkanban_create", PANEL_WIDTH, PANEL_HEIGHT);

        Column root = new Column();
        root.sizeRel(1f, 1f)
            .padding(6);

        root.child(new TextWidget<>(IKey.str("新建任务")).height(12));

        // ---- 标题 ----
        final TextFieldWidget titleField = new TextFieldWidget();
        titleField.acceptsExpressions(false);
        titleField.setMaxLength(Task.MAX_TITLE_LENGTH);
        root.child(buildFieldRow("标题", titleField));

        // ---- 描述 ----
        final TextFieldWidget descField = new TextFieldWidget();
        descField.acceptsExpressions(false);
        descField.setMaxLength(Task.MAX_DESCRIPTION_LENGTH);
        root.child(buildFieldRow("描述", descField));

        // ---- 优先级(循环按钮) ----
        final TaskPriority[] prioState = { TaskPriority.NORMAL };
        ButtonWidget<?> prioBtn = new ButtonWidget<>();
        prioBtn.overlay(IKey.dynamic(() -> prioState[0].name()))
            .onMousePressed(mouseButton -> {
                if (mouseButton == 0) {
                    TaskPriority[] all = TaskPriority.values();
                    prioState[0] = all[(prioState[0].ordinal() + 1) % all.length];
                    return true;
                }
                return false;
            });
        root.child(buildFieldRow("优先级", prioBtn));

        // ---- 作用域(循环按钮) ----
        final TaskScope[] scopeState = { TaskScope.TEAM };
        ButtonWidget<?> scopeBtn = new ButtonWidget<>();
        scopeBtn.overlay(IKey.dynamic(() -> scopeState[0].name()))
            .onMousePressed(mouseButton -> {
                if (mouseButton == 0) {
                    TaskScope[] all = TaskScope.values();
                    scopeState[0] = all[(scopeState[0].ordinal() + 1) % all.length];
                    return true;
                }
                return false;
            });
        root.child(buildFieldRow("作用域", scopeBtn));

        // ---- 领取人数 ----
        final TextFieldWidget claimersField = new TextFieldWidget();
        claimersField.acceptsExpressions(false);
        claimersField.setNumbers(1, 64);
        claimersField.setText("3");
        root.child(buildFieldRow("人数", claimersField));

        // ---- 目标玩家(仅 DIRECT 用) ----
        final TextFieldWidget targetField = new TextFieldWidget();
        targetField.acceptsExpressions(false);
        targetField.setMaxLength(32);
        root.child(buildFieldRow("目标", targetField));

        // ---- 底部按钮行 ----
        Row buttons = new Row();
        buttons.widthRel(1f)
            .height(16);

        ButtonWidget<?> createBtn = new ButtonWidget<>();
        createBtn.expanded()
            .heightRel(1f)
            .overlay(IKey.str("创建"))
            .onMousePressed(mouseButton -> {
                if (mouseButton == 0) {
                    String title = titleField.getText();
                    if (title == null || title.trim()
                        .isEmpty()) {
                        return true;
                    }
                    String description = descField.getText();
                    if (description == null) description = "";
                    int claimers;
                    try {
                        claimers = Integer.parseInt(
                            claimersField.getText()
                                .trim());
                    } catch (NumberFormatException e) {
                        claimers = 3;
                    }
                    String target = targetField.getText();
                    if (target == null) target = "";
                    target = target.trim();
                    // DIRECT 必须填目标,客户端先拦一道,服务端还会再校验一次
                    if (scopeState[0] == TaskScope.DIRECT && target.isEmpty()) {
                        return true;
                    }
                    NHKanbanNet.CHANNEL.sendToServer(
                        new CreateTaskPacket(
                            title.trim(),
                            description,
                            scopeState[0],
                            prioState[0],
                            claimers,
                            target,
                            ctx));
                    self.closePanel();
                    return true;
                }
                return false;
            });
        buttons.child(createBtn);

        ButtonWidget<?> cancelBtn = new ButtonWidget<>();
        cancelBtn.expanded()
            .heightRel(1f)
            .overlay(IKey.str("取消"))
            .onMousePressed(mouseButton -> {
                if (mouseButton == 0) {
                    self.closePanel();
                    return true;
                }
                return false;
            });
        buttons.child(cancelBtn);

        root.child(buttons);
        panel.child(root);
        return panel;
    }

    /**
     * 把 label 和 input 横排成一行。label 固定宽,input 用 expanded 撑满剩余。
     * input 的 expanded()/heightRel(1f) 在这里强制设置,调用方不需要在外面
     * 重复写,避免漏掉一个就出现宽度退化的 bug。
     */
    private static Row buildFieldRow(String label, IWidget input) {
        Row row = new Row();
        row.widthRel(1f)
            .height(ROW_HEIGHT);
        row.child(new TextWidget<>(IKey.str(label)).width(LABEL_WIDTH));
        if (input instanceof IPositioned<?>) {
            ((IPositioned<?>) input).expanded()
                .heightRel(1f);
        }
        row.child(input);
        return row;
    }
}
