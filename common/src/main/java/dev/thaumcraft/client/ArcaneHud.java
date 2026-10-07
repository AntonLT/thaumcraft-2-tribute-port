package dev.thaumcraft.client;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.CarpetEntity;
import dev.thaumcraft.item.ItemState;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** Original detector and carpet overlay. Aura values arrive on the inventory item from the server. */
public final class ArcaneHud {
    private ArcaneHud(){}
    private static void blit(GuiGraphicsExtractor g,int x,int y,int u,int v,int w,int h){if(w>0&&h>0)g.blit(RenderPipelines.GUI_TEXTURED,Thaumcraft.id("textures/legacy/guidetector.png"),x,y,u,v,w,h,256,256);}
    public static void extract(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.screen!=null||mc.options.hideGui)return;
        int width=mc.getWindow().getGuiScaledWidth(),height=mc.getWindow().getGuiScaledHeight();
        if(mc.player.getVehicle() instanceof CarpetEntity carpet){int full=Math.clamp(carpet.endurance()*96/600,0,96);blit(g,width/2-48,5,0,104,full,8);blit(g,width/2-48+full,5,full,112,96-full,8);}
        int type=-1;ItemStack detector=ItemStack.EMPTY;
        for(int i=0;i<mc.player.getInventory().getContainerSize();i++){
            var stack=mc.player.getInventory().getItem(i);var entry=Content.entry(stack);if(entry==null||!entry.source_class().equals("ItemVisDetector"))continue;
            int found=entry.meta();if(type==3)continue;
            if(found==2)type=3;else if(type==1&&found==0||type==0&&found==1)type=2;else type=Math.max(type,found);detector=stack;
        }
        var worn=dev.thaumcraft.item.ArcanaItem.auraRevealer(mc.player);boolean goggles=!worn.isEmpty();
        if(goggles){type=3;detector=worn;}if(type<0||detector.isEmpty())return;
        var aura=ItemState.tag(detector);if(!aura.contains("thaumcraft_aura_vis"))return;
        int vis=aura.getIntOr("thaumcraft_aura_vis",0),taint=aura.getIntOr("thaumcraft_aura_taint",0),good=aura.getIntOr("thaumcraft_aura_good",0),bad=aura.getIntOr("thaumcraft_aura_bad",0),shift=type==3?56:0;
        for(int i=0;i<2;i++){
            if(type==0&&i==1||type==1&&i==0)continue;
            int maximum=Math.max(1,aura.getIntOr("thaumcraft_aura_max",dev.thaumcraft.PortConfig.auraMax));
            int value=i==0?vis:taint,previous=aura.getIntOr(i==0?"thaumcraft_aura_previous_vis":"thaumcraft_aura_previous_taint",value),missing=Math.clamp(48*(maximum-value)/maximum,0,48),x=width-30+i*15;
            if((i==0?good:bad)>0)blit(g,x-4,height-17,0,72,16,16);
            blit(g,x,height-67+missing,shift+i*8,missing,8,48-missing);
            if(type>=3&&value!=previous)blit(g,x,height-48,value>previous?72:80,0,8,8);
            blit(g,x-1,height-71,i==0?23:39,0,10,74);
        }
        if(!goggles)return;
        if(good>0)blit(g,46,height-28,0,72,16,16);if(bad>0)blit(g,46,height-18,0,72,16,16);
        blit(g,50,height-25,24,57,8,9);blit(g,50,height-15,40,57,8,9);
        g.text(mc.font,Component.translatableWithFallback("gui.thaumcraft2tp.hud.vis", "%s V", vis),6,height-24,0xffeeceee,true);g.text(mc.font,Component.translatableWithFallback("gui.thaumcraft2tp.hud.taint", "%s T", taint),6,height-14,0xff997799,true);
        g.text(mc.font,good+"%",61,height-24,0xffeeceee,true);g.text(mc.font,bad+"%",61,height-14,0xff997799,true);
        if(mc.hitResult instanceof BlockHitResult hit&&mc.level.getBlockEntity(hit.getBlockPos()) instanceof MachineBlockEntity machine
                &&dev.thaumcraft.machine.MachineConnections.revealsVis(mc.level,hit.getBlockPos(),machine.getBlockState())){
            if(machine.suction()>0)g.text(mc.font,Component.translatableWithFallback("gui.thaumcraft2tp.hud.suction", "%s Vis TCB, %s Taint TCB", machine.visSuction(), machine.taintSuction()),width/2+5,height/2+25,0xff888888,true);
            g.text(mc.font,Component.translatableWithFallback("gui.thaumcraft2tp.hud.vis_capacity", "%s V (%s%%)", Math.round(machine.pureVis()), Math.round(machine.pureVis()/machine.capacity()*100)),width/2+5,height/2+5,0xffeeceee,true);
            g.text(mc.font,Component.translatableWithFallback("gui.thaumcraft2tp.hud.taint_capacity", "%s T (%s%%)", Math.round(machine.taintedVis()), Math.round(machine.taintedVis()/machine.capacity()*100)),width/2+5,height/2+15,0xff997799,true);
        }
    }
}
