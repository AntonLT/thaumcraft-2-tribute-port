package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.item.ItemState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Resolve actual baked bracelet models, including old saved stacks with only seal_rune. */
final class BraceletVisualChecks {
    private static boolean checked;
    static void verify(){
        if(checked)return;checked=true;
        var mc=Minecraft.getInstance();var resolver=new ItemModelResolver(mc.getModelManager());
        for(int rune=-2;rune<=5;rune++){
            var stack=new ItemStack(Content.item("void_bracelet"));if(rune>=-1)ItemState.setInt(stack,"seal_rune",rune);
            ItemState.setString(stack,"unrelated","preserved");
            var state=new ItemStackRenderState();resolver.updateForTopItem(state,stack,ItemDisplayContext.GUI,mc.level,mc.player,0);
            var material=state.pickParticleMaterial(RandomSource.create(0));
            var expected=Thaumcraft.id("item/atlas_"+(rune<0?48:49+rune));
            if(state.isEmpty()||material==null||!material.sprite().contents().name().equals(expected))throw new AssertionError("Bracelet icon for rune "+rune+": expected "+expected+", got "+material);
        }
        Thaumcraft.LOG.info("THAUMCRAFT_BRACELET_VISUAL_PASS checks=8");
    }
}
