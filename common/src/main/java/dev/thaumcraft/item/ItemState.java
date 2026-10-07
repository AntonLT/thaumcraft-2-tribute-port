package dev.thaumcraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** All per-stack state uses synchronized, saved Minecraft data components. */
public final class ItemState {
    private ItemState() {}
    public static CompoundTag tag(ItemStack stack) {return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static int getInt(ItemStack stack,String key,int fallback) {return tag(stack).getIntOr("thaumcraft_"+key,fallback);}
    public static void setInt(ItemStack stack,String key,int value) {
        CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.putInt("thaumcraft_"+key,value));
    }
    public static String getString(ItemStack stack,String key,String fallback) {return tag(stack).getStringOr("thaumcraft_"+key,fallback);}
    public static void setString(ItemStack stack,String key,String value) {
        CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.putString("thaumcraft_"+key,value));
    }
}
