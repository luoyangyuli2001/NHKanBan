package com.lyyl.nhkanban.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.lyyl.nhkanban.common.gui.NHKanbanGui;
import com.lyyl.nhkanban.common.network.ViewTab;

public class BlockTaskBoard extends Block {

    public BlockTaskBoard() {
        super(Material.wood);
        setBlockName("nhkanban.task_board");
        setBlockTextureName("planks_oak");
        setHardness(1.5F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setCreativeTab(CreativeTabs.tabDecorations);
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileTaskBoard();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy,
        float hz) {
        if (world.isRemote) {
            return true;
        }
        if (!(player instanceof EntityPlayerMP)) {
            return true;
        }
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileTaskBoard) {
            NHKanbanGui.openMainBoard((EntityPlayerMP) player, (TileTaskBoard) te, ViewTab.MINE);
        }
        return true;
    }
}
