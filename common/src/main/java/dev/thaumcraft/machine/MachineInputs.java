package dev.thaumcraft.machine;

import dev.thaumcraft.content.Content;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared client hints and authoritative insertion rules, including automation. */
public final class MachineInputs {
    private MachineInputs() {}
    public static int fuelTicks(ItemStack stack,Level level){
        if(stack.is(Content.item("alumentum")))return 16000;
        if(stack.is(Content.item("silverwood_log")))return 600;
        if(stack.is(Content.item("greatwood_log")))return 400;
        return level==null?0:level.fuelValues().burnDuration(stack);
    }
    public static boolean accepts(String id,int slot,ItemStack stack,Level level){
        if(slot==MachineBlockEntity.FUEL_SLOT)return id.equals("arcane_furnace")&&fuelTicks(stack,level)>0;
        if(slot>=9)return false;
        if(slot>=MachineLayout.get(id).inputs())return false;
        var entry=Content.entry(stack);
        return switch(id){
            case "arcane_bore" -> slot==0?entry!=null&&entry.source_class().equals("ItemFocus"):stack.is(Content.item("arcane_singularity"));
            case "vis_condenser" -> entry!=null&&entry.source_class().equals("ItemCrystals")&&entry.meta()!=6;
            case "thaumic_crystalizer" -> entry!=null&&entry.source_class().equals("ItemCrystals");
            case "brazier_of_souls" -> stack.is(Content.item("soul_fragment"));
            case "thaumic_enchanter","occultic_enchanter" -> stack.isEnchantable()||stack.is(net.minecraft.world.item.Items.BOOK);
            default -> true;
        };
    }
}
