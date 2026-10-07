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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

/** Screenshots plus real select/start packets and accepted-quote menu synchronization. */
final class OcculticGuiChecks {
    private static final List<String> STATES=List.of("empty","selected","capacity","four","running");
    private static int elapsed,wait;
    private static boolean clicked,started;
    private static void check(boolean value,String message){if(!value)throw new AssertionError("Occultic GUI: "+message);}
    private static ItemStack upgrade(int meta){return new ItemStack(Content.item(Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemUpgrades")&&e.meta()==meta).findFirst().orElseThrow().id()));}
    private static void select(MachineBlockEntity machine,ResourceKey<Enchantment> key,int rank){
        var ids=machine.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();int bound=machine.enchanting.data(14)/10+1;
        for(int page=0;page<bound;page++){
            for(int slot=0;slot<40;slot++){int id=machine.enchanting.data(16+slot*2);if(id>0&&ids.byId(id-1).is(key)&&machine.enchanting.data(17+slot*2)==rank){check(machine.enchanting.click(100+slot),"Select prepared candidate");return;}}
            machine.enchanting.click(1);
        }
        throw new AssertionError("Missing GUI candidate "+key);
    }
    private static void open(String state){
        var mc=Minecraft.getInstance();for(var player:mc.getSingleplayerServer().getPlayerList().getPlayers()){
            player.closeContainer();var level=player.level();var pos=player.blockPosition().below();
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<2;y++)level.setBlockAndUpdate(pos.offset(x,y,z),(Math.abs(x)==2||Math.abs(z)==2)?Content.block("brain_in_a_jar").defaultBlockState():Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos,Content.block("occultic_enchanter").defaultBlockState());var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.setOwner(player.getUUID());machine.setItem(0,new ItemStack(state.equals("four")?Items.BOOK:Items.DIAMOND_PICKAXE));
            if(state.equals("capacity")){machine.setItem(MachineBlockEntity.UPGRADE_START,upgrade(5));select(machine,Enchantments.EFFICIENCY,5);}
            if(state.equals("four")){machine.setItem(MachineBlockEntity.UPGRADE_START,upgrade(6));for(var key:List.of(Enchantments.UNBREAKING,Enchantments.EFFICIENCY,Enchantments.RESPIRATION,Enchantments.PROTECTION))select(machine,key,1);}
            if(state.equals("running")){machine.setItem(MachineBlockEntity.UPGRADE_START,upgrade(1));select(machine,Enchantments.EFFICIENCY,2);machine.insertVis(10,false);}
            player.openMenu(machine);
        }
    }
    private static void click(Minecraft mc,int x,int y){check(mc.screen.mouseClicked(new MouseButtonEvent(x,y,new MouseButtonInfo(0,0)),false),"Visible control consumes click");}
    static boolean tick(){
        var mc=Minecraft.getInstance();int stage=elapsed/40,phase=elapsed%40;
        if(stage==STATES.size()){Thaumcraft.LOG.info("THAUMCRAFT_OCCULTIC_GUI_PASS states=5 select_start_quote_packets");return true;}
        String state=STATES.get(stage);
        if(phase==0){clicked=false;started=false;mc.getSingleplayerServer().execute(()->open(state));}
        if(phase>=20){
            if(!(mc.screen instanceof MachineScreen)||!(mc.player.containerMenu instanceof MachineMenu menu)||!menu.machineId().equals("occultic_enchanter")){check(++wait<200,"Menu reaches client");return false;}
            int x=(mc.screen.width-176)/2,y=(mc.screen.height-198)/2;
            if(state.equals("selected")&&!clicked){check(menu.candidateData(0)>0,"Visible candidate synchronized");click(mc,x+10,y+10);clicked=true;}
            if(state.equals("selected")&&menu.enchantmentData(15)==0){check(++wait<200,"Glyph selection reaches server and returns");return false;}
            if(state.equals("running")&&!started){check(menu.enchantmentData(10)==92,"Discounted quote synchronized");click(mc,x+48,y+100);started=true;}
            if(state.equals("running")&&(menu.enchantmentData(13)==0||menu.progress()!=10)){check(++wait<200,"Start packet and partial payment synchronized");return false;}
            if(phase==26&&state.equals("running"))mc.getSingleplayerServer().execute(()->{for(var player:mc.getSingleplayerServer().getPlayerList().getPlayers())if(player.level().getBlockEntity(player.blockPosition().below()) instanceof MachineBlockEntity machine)machine.setItem(MachineBlockEntity.UPGRADE_START,ItemStack.EMPTY);});
            if(phase==34){
                check(!state.equals("empty")||menu.enchantmentData(15)==0&&menu.enchantmentData(10)==0,"Empty selection and quote synchronized");
                check(!state.equals("capacity")||menu.status(28)==6,"Capacity upgrade synchronized");
                check(!state.equals("four")||menu.status(28)==7&&menu.enchantmentData(15)==4,"Fourth selection synchronized");
                check(!state.equals("running")||menu.status(28)==0&&menu.enchantmentData(10)==92,"Accepted quote unchanged after upgrade removal");
                Screenshot.grab(mc.gameDirectory,"thaumcraft-gui-occultic-"+state+".png",mc.getMainRenderTarget(),1,result->{});
            }
        }
        wait=0;elapsed++;return false;
    }
}
