package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

final class HeldToolFallChecks {
    private static double step(ItemStack main,ItemStack off,double velocity) {
        var player=Minecraft.getInstance().player;
        player.setPos(0,250,0);
        player.setOnGround(false);
        player.setNoGravity(true);
        player.noPhysics=true;
        player.getAbilities().flying=false;
        player.setItemInHand(InteractionHand.MAIN_HAND,main);
        player.setItemInHand(InteractionHand.OFF_HAND,off);
        player.setDeltaMovement(0,velocity,0);
        player.aiStep();
        return player.getDeltaMovement().y;
    }
    private static void equal(double actual,double expected,String message) {
        if(Math.abs(actual-expected)>1e-6)throw new AssertionError(message+": expected "+expected+", got "+actual);
    }
    static void verify() {
        double falling=step(ItemStack.EMPTY,ItemStack.EMPTY,-1);
        double rising=step(ItemStack.EMPTY,ItemStack.EMPTY,1);
        if(falling>=-.5||rising<=.5)throw new AssertionError("Fixture must simulate both falling and rising movement");
        for(String id:new String[]{"sword_of_the_zephyr","elemental_cutter"}) {
            var tool=new ItemStack(Content.item(id));
            equal(step(tool,ItemStack.EMPTY,-1),falling*.9,id+" slows local falling exactly once");
            equal(step(tool,ItemStack.EMPTY,1),rising,id+" does not slow ascent");
            equal(step(ItemStack.EMPTY,tool,-1),falling,id+" in offhand does not slow falling");
        }
        Thaumcraft.LOG.info("THAUMCRAFT_HELD_TOOL_FALL_PASS checks=6");
    }
}
