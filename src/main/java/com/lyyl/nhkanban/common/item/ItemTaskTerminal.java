package com.lyyl.nhkanban.common.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.lyyl.nhkanban.NHKanBan;
import com.lyyl.nhkanban.client.ui.TaskBoardPanel;
import com.lyyl.nhkanban.common.gui.GuiContext;
import com.lyyl.nhkanban.common.gui.NHKanbanGui;
import com.lyyl.nhkanban.common.network.ViewTab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 便携任务终端。右键空气打开看板,内容跟方块版完全一致——
 * 通过 GuiContext.forItem() 告诉服务端走 PlayerInventoryGuiFactory 而不是
 * TileEntityGuiFactory。
 *
 * <p>
 * 实现 IGuiHolder<PlayerInventoryGuiData> 而不是 IGuiHolder<GuiData>,
 * 因为 ItemGuiFactory 已废弃,推荐路径是 PlayerInventoryGuiFactory。
 * data 参数我们不读,只用来满足泛型签名。
 */
public class ItemTaskTerminal extends Item implements IGuiHolder<PlayerInventoryGuiData> {

    public ItemTaskTerminal() {
        setUnlocalizedName("nhkanban.task_terminal");
        setTextureName("book_normal");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            NHKanbanGui.openMainBoard((EntityPlayerMP) player, GuiContext.forItem(), ViewTab.MINE);
        }
        return stack;
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return TaskBoardPanel.build(GuiContext.forItem());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(NHKanBan.MODID, mainPanel);
    }
}
