package com.lyyl.nhkanban.common.registry;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.lyyl.nhkanban.common.item.ItemTaskTerminal;

import cpw.mods.fml.common.registry.GameRegistry;

public final class NHKanbanItems {

    public static ItemTaskTerminal taskTerminal;

    private NHKanbanItems() {}

    public static void register() {
        taskTerminal = new ItemTaskTerminal();
        GameRegistry.registerItem(taskTerminal, "task_terminal");
    }

    public static void registerRecipes() {
        GameRegistry
            .addShapelessRecipe(new ItemStack(taskTerminal), new ItemStack(Items.book), new ItemStack(Items.redstone));
    }
}
