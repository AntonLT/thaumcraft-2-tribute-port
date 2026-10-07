package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.mixin.ItemEntityAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

final class CrucibleMotionChecks {
    private static boolean checked;
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static boolean verify() {
        if(checked)return true;
        var level=Minecraft.getInstance().level;
        var pos=Minecraft.getInstance().player.blockPosition().above(3);
        if(level.getChunkSource().getChunk(pos.getX()>>4,pos.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)==null)return false;
        checked=true;
        var previous=level.getBlockState(pos);
        var renderer=(net.minecraft.client.renderer.entity.ItemEntityRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(new ItemEntity(level,0,0,0,new ItemStack(Items.COBBLESTONE)));
        for(String id:new String[]{"crucible","crucible_of_eyes","thaumium_crucible","crucible_of_souls"}) {
            var state=Content.block(id).defaultBlockState();
            check(level.setBlock(pos,state,0),"Fixture crucible is placed in a loaded client chunk");
            var item=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.COBBLESTONE,32));
            Vec3 initial=new Vec3(.1,-.1,.2);
            item.setDeltaMovement(initial);
            item.tickCount=200;
            ((ItemEntityAccess)item).thaumcraft$setAge(100);
            item.setPickUpDelay(0);
            state.entityInside(level,pos,item,InsideBlockEffectApplier.NOOP,false);
            var change=item.getDeltaMovement().subtract(initial);
            var rendered=renderer.createRenderState();
            renderer.extractRenderState(item,rendered,.25f);
            if(id.equals("crucible_of_souls")) {
                check(change.equals(Vec3.ZERO)&&item.getAge()==100&&!item.hasPickUpDelay(),"Soul crucible must not boil items");
                check(rendered.ageInTicks==200.25f,"Soul crucible retains normal item animation");
            } else {
                check(change.y>0&&change.y<.1&&Math.abs(change.x)<.05&&Math.abs(change.z)<.05,id+" applies original boiling impulses on the client");
                check(item.getAge()==0&&item.hasPickUpDelay(),id+" resets the item's animation age and pickup delay");
                check(rendered.ageInTicks==.25f,id+" renders bobbing and rotation using the reset age");
                item.setPos(pos.getX()+.5,pos.getY()+.71,pos.getZ()+.5);
                item.setDeltaMovement(initial);
                state.entityInside(level,pos,item,InsideBlockEffectApplier.NOOP,false);
                check(item.getDeltaMovement().equals(initial),id+" only boils items below the original height threshold");
                item.setPos(pos.getX()+2,pos.getY()+1,pos.getZ()+2);
                ((ItemEntityAccess)item).thaumcraft$setAge(10);
                renderer.extractRenderState(item,rendered,.25f);
                check(rendered.ageInTicks==10.25f,"Ejected item keeps its animation clock without a jump");
            }
            check(item.getItem().getCount()==32,"Client boiling must not consume items");
        }
        level.setBlock(pos,previous,0);
        Thaumcraft.LOG.info("THAUMCRAFT_CRUCIBLE_MOTION_PASS checks=25");
        return true;
    }
}
