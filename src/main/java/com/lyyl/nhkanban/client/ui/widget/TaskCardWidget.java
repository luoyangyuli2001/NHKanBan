package com.lyyl.nhkanban.client.ui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.lyyl.nhkanban.common.network.TaskSummary;
import com.lyyl.nhkanban.common.task.TaskPriority;

/**
 * 单张任务卡片。直接用 ButtonWidget 作为卡片根,widthRel(1f) 撑满父列。
 *
 * <p>
 * 不要把 Minecraft 类的引用从 lambda 里提到方法体顶层——lambda 体的类解析
 * 是惰性的,只在 widget 真正被点击(必然在客户端)时才发生;但提到顶层后,
 * 服务端调用 buildUI 时就会触发 Minecraft 类加载,在专用服上立刻挂掉。
 */
public final class TaskCardWidget {

    private static final int CARD_HEIGHT = 14;

    private TaskCardWidget() {}

    public static IWidget build(TaskSummary task) {
        String label = formatLabel(task);
        String uuidStr = task.uuid.toString();
        String shortId = uuidStr.substring(0, 8);

        ButtonWidget<?> btn = new ButtonWidget<>();
        btn.widthRel(1f)
            .height(CARD_HEIGHT)
            .overlay(IKey.str(label))
            .onMousePressed(mouseButton -> {
                if (mouseButton == 0) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(
                            EnumChatFormatting.GRAY + "[NHKanban] "
                                + EnumChatFormatting.WHITE
                                + task.title
                                + EnumChatFormatting.GRAY
                                + " ("
                                + shortId
                                + ")"));
                    return true;
                }
                return false;
            });
        return btn;
    }

    private static String formatLabel(TaskSummary task) {
        String prioColor = priorityColorCode(task.priority);
        return prioColor + "\u258C"
            + EnumChatFormatting.RESET
            + " "
            + EnumChatFormatting.WHITE
            + truncate(task.title, 12)
            + "  "
            + EnumChatFormatting.GRAY
            + task.claimerCount
            + "/"
            + task.maxClaimers
            + EnumChatFormatting.RESET;
    }

    private static String priorityColorCode(TaskPriority p) {
        if (p == null) return EnumChatFormatting.WHITE.toString();
        switch (p) {
            case LOW:
                return EnumChatFormatting.GRAY.toString();
            case HIGH:
                return EnumChatFormatting.YELLOW.toString();
            case URGENT:
                return EnumChatFormatting.RED.toString();
            case NORMAL:
            default:
                return EnumChatFormatting.WHITE.toString();
        }
    }

    private static String truncate(String s, int maxChars) {
        if (s == null) return "";
        if (s.length() <= maxChars) return s;
        return s.substring(0, maxChars - 1) + "…";
    }
}
