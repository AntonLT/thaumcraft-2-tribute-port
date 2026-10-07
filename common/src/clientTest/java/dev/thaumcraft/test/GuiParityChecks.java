package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.client.*;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.ModEntities;
import dev.thaumcraft.entity.TravelingTrunk;
import dev.thaumcraft.gameplay.*;
import dev.thaumcraft.machine.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

/** Real menu-open, slot-data and button packets for every source GUI on each loader. */
public final class GuiParityChecks {
    private static final List<String> CASES=List.of("quaesitum","arcane_furnace","arcane_bore","vis_condenser","darkness_generator","thaumic_crystalizer","thaumic_duplicator","thaumic_restorer","thaumic_infuser","dark_infuser","thaumic_enchanter","occultic_enchanter","brazier_of_souls","thaumic_generator","trunk","roomy_trunk","crystal_ball","thaumonomicon","discovery","void_chest","hud");
    private static int elapsed,responseWait;
    private static boolean finished;
    private static void check(boolean test,String message){if(!test)throw new AssertionError("GUI client parity: "+message);}
    private static ItemStack item(String id){return new ItemStack(Content.item(id));}
    private static ItemStack source(String cls,int meta){return item(Content.ENTRIES.stream().filter(e->e.source_class().equals(cls)&&e.meta()==meta).findFirst().orElseThrow().id());}
    public static boolean tick(int ignoredTick){
        int tick=elapsed;var mc=Minecraft.getInstance();int stage=tick/36,phase=tick%36;
        if(stage>=CASES.size()){if(finished)return false;finished=true;Thaumcraft.LOG.info("THAUMCRAFT_GUI_PARITY_PASS screens={} hud=1",CASES.size()-1);return true;}
        String id=CASES.get(stage);
        if(id.equals("occultic_enchanter")){if(OcculticGuiChecks.tick())elapsed+=36;return false;}
        if(phase==0){if(stage==0){mc.getWindow().setWindowed(1120,840);mc.options.fullscreen().set(false);}mc.options.guiScale().set(2);mc.options.hideGui=false;mc.resizeGui();mc.getSingleplayerServer().execute(()->open(id));}
        if(phase==20){
            if(id.equals("hud")){
                boolean ready=mc.screen==null&&mc.player.getVehicle() instanceof dev.thaumcraft.entity.CarpetEntity&&dev.thaumcraft.item.ItemState.getInt(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),"aura_vis",-1)>=0;
                if(!ready){check(++responseWait<200,"HUD receives aura and carpet data");return false;}
                check(((dev.thaumcraft.entity.CarpetEntity)mc.player.getVehicle()).endurance()==450,"Carpet endurance reaches client HUD");
                Thaumcraft.LOG.info("THAUMCRAFT_HUD_PARITY_PASS detector_and_carpet");
            }
            else
            if(MachineLayout.ALL.containsKey(id)){
                check(mc.screen instanceof MachineScreen&&mc.player.containerMenu instanceof MachineMenu,"Dedicated machine screen "+id);
                var menu=(MachineMenu)mc.player.containerMenu;check(menu.machineId().equals(id),"Correct machine identifier "+id);
                check(menu.getType()==Content.machineMenu(id),"Correct client menu type "+id);
                for(var slot:menu.slots)check(slot.x>=0&&slot.y>=0&&slot.x+16<=menu.layout.width()&&slot.y+16<=menu.layout.height(),"Slot fits the original background "+id);
                if(id.equals("darkness_generator"))check(menu.slots.size()==38&&menu.layout.playerY()==84,"Dark generator retains original player inventory");
                if(id.equals("arcane_furnace"))check(menu.status(24)>0&&menu.getSlot(9).getContainerSlot()==27,"Fuel and burn data synchronized");
                if(id.equals("quaesitum"))check(menu.status(120)>0&&menu.status(123)>=0,"Research odds synchronized");
            }else if(id.contains("trunk")){
                check(mc.screen instanceof TrunkScreen&&mc.player.containerMenu instanceof TrunkMenu menu&&menu.rows==(id.equals("trunk")?3:4),"Original trunk screen and capacity "+id);
                if(id.equals("roomy_trunk")){var menu=(TrunkMenu)mc.player.containerMenu;check(menu.status(4)==5&&menu.status(5)==0,"Trunk upgrade installation order reaches the client");}
            }
            else if(id.equals("void_chest"))check(mc.screen instanceof VoidScreen&&mc.player.containerMenu instanceof VoidMenu menu&&menu.slots.size()==108,"Original 72-slot chest screen");
            else check(mc.screen instanceof ResearchScreen,"Original item screen "+id);
            Screenshot.grab(mc.gameDirectory,"thaumcraft-gui-"+id+".png",mc.getMainRenderTarget(),1,result->{});
        }
        if(phase==24&&!id.equals("hud")){
            int x=(mc.screen.width-176)/2;
            if(id.equals("thaumic_duplicator"))click(mc,x+63,(mc.screen.height-166)/2+49);
            if(id.equals("thaumic_enchanter"))click(mc,x+65,(mc.screen.height-182)/2+25);
            if(id.contains("trunk"))click(mc,x+160,(mc.screen.height-166)/2+89);
            if(id.equals("thaumonomicon"))click(mc,8,mc.screen.height/2-45+3*20+2);
            if(id.equals("crystal_ball"))click(mc,36,mc.screen.height/2-64+5*20+2);
        }
        if(phase==33){
            boolean received=switch(id){
                case "thaumic_duplicator"->mc.player.containerMenu instanceof MachineMenu menu&&menu.status(27)==1;
                case "thaumic_enchanter"->mc.player.containerMenu instanceof MachineMenu menu&&(menu.status(35)>0||menu.getSlot(0).getItem().isEnchanted());
                case "trunk","roomy_trunk"->mc.player.containerMenu instanceof TrunkMenu menu&&menu.status(2)==1;
                case "thaumonomicon"->mc.screen instanceof ResearchScreen screen&&screen.category()==3&&screen.pageIndex()==0;
                case "crystal_ball"->mc.screen instanceof ResearchScreen screen&&screen.selectedRunes()[1]==5;
                default->true;
            };
            if(!received){check(++responseWait<200,"Timed out waiting for authoritative control result: "+id);return false;}
        }
        responseWait=0;elapsed++;
        return false;
    }
    private static void click(Minecraft mc,int x,int y){check(mc.screen.mouseClicked(new MouseButtonEvent(x,y,new MouseButtonInfo(0,0)),false),"Visible control consumes click");}
    private static void open(String id){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        for(var player:server.getPlayerList().getPlayers()){
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            for(var project:GameData.projects())ArcaneWorldData.researchData(player.level()).unlock(player.getUUID(),project.index());
            player.closeContainer();player.stopRiding();var level=player.level();BlockPos pos=player.blockPosition().below();
            if(id.equals("hud")){
                player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,source("ItemVisGoggles",0));
                var carpet=(dev.thaumcraft.entity.CarpetEntity)ModEntities.TYPES.get("flying_carpet").create(level,EntitySpawnReason.COMMAND);
                carpet.setPos(player.getX(),player.getY(),player.getZ());carpet.setEndurance(450);level.addFreshEntity(carpet);player.startRiding(carpet);continue;
            }
            if(id.contains("trunk")){
                var trunk=(TravelingTrunk)ModEntities.TYPES.get("traveling_trunk").create(level,EntitySpawnReason.COMMAND);trunk.setPos(player.getX()+1,player.getY(),player.getZ());trunk.setOwner(player.getUUID());trunk.setNoAi(true);trunk.setHealth(trunk.getMaxHealth()*.75f);level.addFreshEntity(trunk);
                if(id.equals("roomy_trunk")){check(trunk.installUpgrade(source("ItemUpgrades",5)),"Roomy trunk accepts capacity upgrade");check(trunk.installUpgrade(source("ItemUpgrades",0)),"Roomy trunk accepts a second upgrade");}
                for(int i=0;i<trunk.getContainerSize();i++)trunk.setItem(i,new ItemStack(Items.EMERALD,i+1));player.openMenu(trunk);continue;
            }
            if(id.equals("crystal_ball")){var ball=source("ItemCrystalBall",0);player.setItemInHand(InteractionHand.MAIN_HAND,ball);ResearchBook.openCrystalBall(player,ball,InteractionHand.MAIN_HAND,new int[]{0,1,2},true);continue;}
            if(id.equals("thaumonomicon")){var book=item(id);player.setItemInHand(InteractionHand.MAIN_HAND,book);ResearchBook.openTome(player,book,InteractionHand.MAIN_HAND);continue;}
            if(id.equals("discovery")){var project=GameData.projects().stream().filter(p->p.type()==2).findFirst().orElseThrow();var book=new GameData.StackDef(project.discovery(),1).create();player.setItemInHand(InteractionHand.MAIN_HAND,book);ResearchBook.openDiscovery(player,book,InteractionHand.MAIN_HAND,project.index());continue;}
            level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,Content.block(id).defaultBlockState());var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.setOwner(player.getUUID());
            machine.processes.boost=6;machine.insertVis(200,false);machine.insertVis(100,true);
            switch(id){
                case "quaesitum"->{machine.setItem(0,ResearchLogic.theory(0));machine.setItem(3,new ItemStack(Items.PAPER,64));}
                case "arcane_furnace"->{machine.setItem(0,new ItemStack(Items.RAW_IRON,32));machine.setItem(27,new ItemStack(Items.COAL,3));}
                case "vis_condenser"->machine.setItem(0,item("vis_crystal"));
                case "thaumic_crystalizer"->machine.setItem(0,new ItemStack(Content.item("depleted_crystal"),32));
                case "thaumic_duplicator"->machine.setItem(0,new ItemStack(Items.DIAMOND,32));
                case "thaumic_restorer"->{for(int i=0;i<6;i++){var stack=new ItemStack(Items.DIAMOND_PICKAXE);stack.setDamageValue(800);machine.setItem(i,stack);}}
                case "thaumic_infuser","dark_infuser"->{var recipe=GameData.infusions().stream().filter(r->r.dark()==id.equals("dark_infuser")&&r.ingredients().size()<=5).findFirst().orElseThrow();int slot=0;for(String ingredient:recipe.ingredients()){
                    ItemStack stack;if(ingredient.startsWith("#"))stack=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.Identifier.parse(ingredient.substring(1)))).orElseThrow().iterator().next());else stack=new GameData.StackDef(ingredient,1).create();machine.setItem(slot++,stack);
                }}
                case "thaumic_enchanter","occultic_enchanter"->machine.setItem(0,new ItemStack(Items.DIAMOND_SWORD));
                case "brazier_of_souls"->machine.setItem(0,new ItemStack(Content.item("soul_fragment"),2));
                case "void_chest"->{for(int i=0;i<72;i++)machine.setItem(i,new ItemStack(Items.EMERALD,i%64+1));}
            }
            player.openMenu(machine);
        }
    }
}
