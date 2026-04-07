package com.lyyl.nhkanban.common.gui;

import io.netty.buffer.ByteBuf;

/**
 * 描述一次 GUI 打开的来源。看板可以从方块或便携终端道具打开,
 * 两条路径走的 MUI2 工厂不同(TileEntityGuiFactory vs PlayerInventoryGuiFactory),
 * 但 panel 内容、Tab 切换、创建任务等流程完全相同——我们只在 server 端
 * 决定走哪条 open 路径时分支一次。
 *
 * <p>
 * 所有面板构建参数和 C → S 包都用这个对象传 source,而不是散装的 x/y/z。
 * 替换"加 boolean fromItem"的方案,避免每个 packet 多塞一个 flag 还带着
 * 无意义的坐标。
 *
 * <p>
 * 不可变,可在多线程间安全共享。
 */
public final class GuiContext {

    private final boolean fromItem;
    private final int x;
    private final int y;
    private final int z;

    private GuiContext(boolean fromItem, int x, int y, int z) {
        this.fromItem = fromItem;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static GuiContext forTile(int x, int y, int z) {
        return new GuiContext(false, x, y, z);
    }

    public static GuiContext forItem() {
        return new GuiContext(true, 0, 0, 0);
    }

    public boolean isFromItem() {
        return fromItem;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public void writeTo(ByteBuf buf) {
        buf.writeBoolean(fromItem);
        if (!fromItem) {
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
        }
    }

    public static GuiContext readFrom(ByteBuf buf) {
        boolean fromItem = buf.readBoolean();
        if (fromItem) return forItem();
        return forTile(buf.readInt(), buf.readInt(), buf.readInt());
    }
}
