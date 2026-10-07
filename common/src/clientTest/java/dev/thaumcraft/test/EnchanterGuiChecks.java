package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.client.MachineScreen;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Original empty, runic, hover, selected and invalid-input GUI states with real menu packets. */
final class EnchanterGuiChecks {
    private static int ticks,wait;
    private static void check(boolean value,String message){if(!value)throw new AssertionError("Enchanter GUI: "+message);}
    static boolean tick(){
        var mc=Minecraft.getInstance();
        if(ticks==0){
            mc.options.guiScale().set(2);mc.resizeGui();mc.options.hideGui=false;mc.options.pauseOnLostFocus=false;
            mc.getSingleplayerServer().execute(()->{
                for(var player:mc.getSingleplayerServer().getPlayerList().getPlayers()){
                    player.closeContainer();var level=player.level();var pos=player.blockPosition().below();
                    for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<2;y++)
                        level.setBlockAndUpdate(pos.offset(x,y,z),Math.abs(x)==2||Math.abs(z)==2?Blocks.BOOKSHELF.defaultBlockState():Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(pos,Content.block("thaumic_enchanter").defaultBlockState());
                    var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.setOwner(player.getUUID());player.openMenu(machine);
                }
            });
        }
        if(ticks>=20){
            if(!(mc.screen instanceof MachineScreen)||!(mc.player.containerMenu instanceof MachineMenu menu)||!menu.machineId().equals("thaumic_enchanter")){
                check(++wait<200,"Menu synchronized");return false;
            }
            if(ticks==20){check(menu.slots.size()==37&&menu.status(32)==0,"Empty menu retains all inventory slots");capture("empty");}
            if(ticks==25)setItem(new ItemStack(Items.DIAMOND_SWORD));
            if(ticks==50){check(menu.status(34)>0,"Offers synchronized");capture("offers");}
            if(ticks==55){
                double scale=mc.getWindow().getGuiScale();
                // Wayland ignores cursor warps; deliver the normal callback directly.
                try{
                    var move=net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove",long.class,double.class,double.class);move.setAccessible(true);
                    for(int i=0;i<2;i++)move.invoke(mc.mouseHandler,mc.getWindow().handle(),((mc.screen.width-176)/2+100)*scale,((mc.screen.height-182)/2+66)*scale);
                }catch(ReflectiveOperationException e){throw new AssertionError("Hover input",e);}
            }
            if(ticks==65)capture("hover");
            if(ticks==70){
                int x=(mc.screen.width-176)/2+100,y=(mc.screen.height-182)/2+66;
                check(mc.screen.mouseClicked(new MouseButtonEvent(x,y,new MouseButtonInfo(0,0)),false),"Offer click handled");
            }
            if(ticks==80){
                check(menu.status(35)==3,"Third offer accepted by server");
                mc.getSingleplayerServer().execute(()->{for(var player:mc.getSingleplayerServer().getPlayerList().getPlayers()){
                    var machine=(MachineBlockEntity)player.level().getBlockEntity(player.blockPosition().below());machine.insertVis(machine.workRequired/2,false);
                }});
            }
            if(ticks==120){check(menu.progress()>0&&menu.progress()<menu.required(),"Partial payment remains in progress");capture("running");}
            if(ticks==125)setItem(new ItemStack(Items.DIRT));
            if(ticks==150){check(menu.status(32)==0&&menu.status(35)==0,"Invalid input clears offers and selection");capture("invalid");}
            if(ticks==160){Thaumcraft.LOG.info("THAUMCRAFT_ENCHANTER_GUI_PASS states=5 offer_selection_partial_payment");return true;}
        }
        ticks++;return false;
    }
    private static void setItem(ItemStack item){
        var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->{for(var player:mc.getSingleplayerServer().getPlayerList().getPlayers())
            ((MachineBlockEntity)player.level().getBlockEntity(player.blockPosition().below())).setItem(0,item.copy());});
    }
    private static void capture(String state){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"thaumcraft-enchanter-"+state+".png",mc.getMainRenderTarget(),1,result->{});}
}
