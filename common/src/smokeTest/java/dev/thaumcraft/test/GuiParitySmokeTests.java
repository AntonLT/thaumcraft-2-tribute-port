package dev.thaumcraft.test;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.*;
import dev.thaumcraft.item.ItemState;
import dev.thaumcraft.machine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.*;

/** Regression checks for the original public controls and their authoritative server effects. */
public final class GuiParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("GUI parity: "+message);}
    private static final class Player extends ServerPlayer {
        int openedMenus;
        Player(MinecraftServer server,ServerLevel level){super(server,level,new GameProfile(UUID.fromString("df39e035-957a-4809-b07a-94552f61e790"),"GuiParity"),ClientInformation.createDefault());}
        @Override public void sendSystemMessage(Component message,boolean overlay){}
        @Override public void openItemGui(ItemStack stack,InteractionHand hand){}
        @Override public OptionalInt openMenu(net.minecraft.world.MenuProvider provider){openedMenus++;return OptionalInt.of(openedMenus);}
    }
    private static MachineBlockEntity place(ServerLevel level,BlockPos pos,String id,Player player){
        level.getChunkAt(pos);level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,Content.block(id).defaultBlockState());
        var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.setOwner(player.getUUID());player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);return machine;
    }
    private static void ticks(MachineBlockEntity machine,ServerLevel level,int count){for(int i=0;i<count;i++)machine.processes.tick(level);}
    private static ItemStack item(String id){return new ItemStack(Content.item(id));}
    static int run(MinecraftServer server){
        checks=0;var level=server.overworld();var player=new Player(server,level);var pos=new BlockPos(208,280,208);
        for(String id:List.of("everfull_urn","brain_in_a_jar","arcane_seal","arcane_bellows","vis_conduit","vis_filter","vis_pump","vis_purifier","crucible")){
            var machine=place(level,pos,id,player);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
            check(machine.getBlockState().useWithoutItem(level,player,hit)==net.minecraft.world.InteractionResult.PASS,"Unhandled click allows held item use: "+id);
            for(String source:List.of("ItemRunicEssence","ItemUpgrades")){
                if(id.equals("arcane_seal")&&source.equals("ItemRunicEssence"))continue;
                var held=new ItemStack(Content.item(Content.ENTRIES.stream().filter(e->e.source_class().equals(source)).findFirst().orElseThrow().id()));
                check(!machine.getBlockState().useItemOn(held,level,player,InteractionHand.MAIN_HAND,hit).consumesAction(),"Unsupported item does not consume click: "+source+" on "+id);
            }
        }
        var urn=place(level,pos,"everfull_urn",player);
        var urnHit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
        int badVibes=ArcaneWorldData.get(level).aura(level,pos).badVibes();
        check(urn.getBlockState().useItemOn(player.getMainHandItem(),level,player,InteractionHand.MAIN_HAND,urnHit).consumesAction()
                &&player.getMainHandItem().is(Items.WATER_BUCKET),"Urn still fills a held bucket");
        check(ArcaneWorldData.get(level).aura(level,pos).badVibes()==badVibes+1,"Urn filling retains its taint charge");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        for(String id:List.of("everfull_urn","brain_in_a_jar","arcane_seal","void_chest","void_interface","quaesitum","arcane_bore","darkness_generator","brazier_of_souls","totem_of_dawn","totem_of_dusk")){
            check(!MachineConnections.revealsVis(level,pos,Content.block(id).defaultBlockState()),"Goggles omit non-IConnection apparatus: "+id);
        }
        for(String id:List.of("arcane_bellows","vis_conduit","vis_filter","vis_valve","advanced_vis_valve","vis_pump","vis_purifier","vis_storage_tank","thaumium_reinforced_tank","crucible","crucible_of_eyes","thaumium_crucible","crucible_of_souls","vis_condenser","thaumic_generator","thaumic_infuser","dark_infuser","thaumic_restorer","arcane_furnace","thaumic_crystalizer","thaumic_enchanter","occultic_enchanter","thaumic_duplicator")){
            check(MachineConnections.revealsVis(level,pos,Content.block(id).defaultBlockState()),"Goggles retain empty IConnection apparatus: "+id);
        }
        for(String id:List.of("vis_valve","advanced_vis_valve","quaesitum","void_chest")){
            var machine=place(level,pos,id,player);boolean enabled=machine.enabled();int channel=machine.channel(),opened=player.openedMenus;
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
            check(machine.getBlockState().useWithoutItem(level,player,hit).consumesAction(),"Supported click is handled: "+id);
            check(id.equals("vis_valve")?machine.enabled()!=enabled:id.equals("advanced_vis_valve")?machine.channel()==(channel+1)%3:player.openedMenus==opened+1,"Supported click performs its action: "+id);
            player.setShiftKeyDown(true);
            check(machine.getBlockState().useWithoutItem(level,player,hit)==net.minecraft.world.InteractionResult.PASS,"Sneaking bypasses apparatus interaction: "+id);
            player.setShiftKeyDown(false);
        }
        Map<String,Integer> counts=Map.ofEntries(Map.entry("quaesitum",49),Map.entry("arcane_furnace",55),Map.entry("arcane_bore",38),Map.entry("vis_condenser",38),Map.entry("darkness_generator",38),Map.entry("thaumic_crystalizer",43),Map.entry("thaumic_duplicator",46),Map.entry("thaumic_restorer",48),Map.entry("thaumic_infuser",44),Map.entry("dark_infuser",42),Map.entry("thaumic_enchanter",37),Map.entry("occultic_enchanter",37),Map.entry("brazier_of_souls",37),Map.entry("thaumic_generator",0));
        for(var expected:counts.entrySet()){
            var machine=place(level,pos,expected.getKey(),player);var menu=(MachineMenu)machine.createMenu(51,player.getInventory(),player);
            check(menu.getType()==Content.machineMenu(expected.getKey()),"Dedicated menu type before initial slot packet: "+expected.getKey());
            check(menu.slots.size()==expected.getValue(),"Original total slot count: "+expected.getKey());
            check(menu.stillValid(player)&&!menu.clickMenuButton(player,999),"Valid distance and invalid button rejection: "+expected.getKey());
            for(var slot:menu.slots.subList(0,menu.machineSlotCount()))if(slot.getContainerSlot()>=9&&slot.getContainerSlot()<18)check(!slot.mayPlace(new ItemStack(Items.DIRT)),"Output refuses insertion: "+expected.getKey()+"/"+slot.getContainerSlot());
            player.setPos(pos.getX()+20,pos.getY(),pos.getZ());check(!menu.stillValid(player)&&!menu.clickMenuButton(player,0),"Out-of-range controls rejected: "+expected.getKey());
        }
        var furnace=place(level,pos,"arcane_furnace",player);var menu=(MachineMenu)furnace.createMenu(52,player.getInventory(),player);
        player.getInventory().setItem(9,new ItemStack(Items.COAL,3));menu.quickMoveStack(player,19);
        check(furnace.getItem(MachineBlockEntity.FUEL_SLOT).getCount()==3&&furnace.getItem(0).isEmpty(),"Shift-click coal goes to separate fuel slot");
        furnace.setItem(0,new ItemStack(Items.RAW_IRON));furnace.setItem(MachineBlockEntity.FUEL_SLOT,ItemStack.EMPTY);ticks(furnace,level,240);
        check(furnace.getItem(9).isEmpty()&&furnace.getItem(0).is(Items.RAW_IRON),"Furnace requires fuel");
        furnace.setItem(MachineBlockEntity.FUEL_SLOT,new ItemStack(Items.COAL));ticks(furnace,level,200);
        check(furnace.getItem(9).is(Items.IRON_INGOT)&&furnace.getItem(0).isEmpty(),"Fuel burns and smelting result enters visible output");
        check(furnace.data.get(24)>0&&furnace.data.get(25)==1600,"Fuel gauge uses remaining and initial burn time");
        var lava=place(level,pos.east(2),"arcane_furnace",player);lava.setItem(0,new ItemStack(Items.RAW_IRON));lava.setItem(27,new ItemStack(Items.LAVA_BUCKET));ticks(lava,level,1);
        check(lava.getItem(27).is(Items.BUCKET)&&java.util.stream.IntStream.range(9,18).noneMatch(i->lava.getItem(i).is(Items.BUCKET)),"Lava fuel returns exactly one bucket");level.setBlockAndUpdate(pos.east(2),Blocks.AIR.defaultBlockState());
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        menu.quickMoveStack(player,10);check(furnace.getItem(9).isEmpty(),"Shift-click result removes visible furnace output");
        var saved=furnace.saveWithFullMetadata(level.registryAccess());var restored=(MachineBlockEntity)BlockEntity.loadStatic(pos,furnace.getBlockState(),saved,level.registryAccess());
        check(restored!=null&&restored.processes.burnTime==furnace.processes.burnTime,"Fuel state survives save/load");

        var duplicator=place(level,pos,"thaumic_duplicator",player);menu=(MachineMenu)duplicator.createMenu(53,player.getInventory(),player);
        check(!duplicator.processes.repeat,"Duplicator defaults to a single operation");duplicator.setItem(0,new ItemStack(Items.COBBLESTONE));duplicator.insertVis(4,false);ticks(duplicator,level,8);
        check(duplicator.getItem(0).isEmpty()&&duplicator.getItem(9).getCount()==2&&Math.abs(duplicator.pureVis()-2)<.001f,"Single copy moves original and new item into output");
        check(menu.clickMenuButton(player,0)&&duplicator.processes.repeat,"Repeat checkbox changes authoritative state");duplicator.setItem(0,new ItemStack(Items.COBBLESTONE));ticks(duplicator,level,8);
        check(duplicator.getItem(0).getCount()==1&&duplicator.getItem(9).getCount()==3,"Repeat preserves template and produces one copy");
        saved=duplicator.saveWithFullMetadata(level.registryAccess());restored=(MachineBlockEntity)BlockEntity.loadStatic(pos,duplicator.getBlockState(),saved,level.registryAccess());check(restored!=null&&restored.processes.repeat,"Repeat preference persists");

        var basic=place(level,pos,"thaumic_enchanter",player);menu=(MachineMenu)basic.createMenu(54,player.getInventory(),player);basic.setItem(0,new ItemStack(Items.BOOK,64));ticks(basic,level,2);check(basic.basicEnchanting.offer(0)==0&&!menu.clickMenuButton(player,0),"Oversized automated input cannot enchant a stack of books");basic.setItem(0,new ItemStack(Items.DIAMOND_SWORD));ticks(basic,level,2);
        check(basic.basicEnchanting.offer(0)>0&&basic.basicEnchanting.offer(1)>0&&basic.basicEnchanting.offer(2)>0,"Three selectable enchantment offers");
        int[] offers={basic.basicEnchanting.offer(0),basic.basicEnchanting.offer(1),basic.basicEnchanting.offer(2)};ticks(basic,level,20);
        check(Arrays.equals(offers,new int[]{basic.basicEnchanting.offer(0),basic.basicEnchanting.offer(1),basic.basicEnchanting.offer(2)})&&!basic.getItem(0).isEnchanted(),"Offers remain stable and never enchant without selection");
        int row=offers[2]>=2?2:offers[1]>=2?1:0;int cost=menu.status(40+row);basic.insertVis(cost,false);
        check(menu.clickMenuButton(player,row)&&!menu.clickMenuButton(player,row),"Offer starts once and cannot be replaced while active");ticks(basic,level,cost/10+4);
        check(basic.getItem(0).isEnchanted()&&basic.getItem(9).isEmpty()&&basic.pureVis()<.001f,"Basic enchantment charges displayed vis and returns item to its input slot");
        check(player.experienceLevel==0,"Original enchanter does not require experience levels");

        var occultic=place(level,pos,"occultic_enchanter",player);occultic.setItem(0,new ItemStack(Items.BOOK,64));check(!occultic.enchanting.click(2),"Occultic selection rejects stacked books");
        var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var repair=registry.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,dev.thaumcraft.Thaumcraft.id("repair")));
        check(!net.minecraft.world.item.enchantment.Enchantment.areCompatible(repair,registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING)),"Self Repair excludes Unbreaking as in the original GUI");
        var research=place(level,pos,"quaesitum",player);research.setItem(0,ResearchLogic.theory(0));research.setItem(3,new ItemStack(Items.PAPER));var odds=ResearchLogic.odds(research);
        check(odds.success()>0&&odds.failure()>0&&odds.loss()==100&&odds.theoryProgress()==0,"Theory odds and five-step indicator exposed before processing");
        research.setItem(0,ItemStack.EMPTY);check(research.data.get(120)==0&&research.data.get(123)==-1,"Empty research input clears its indicators");
        var data=ArcaneWorldData.get(level);check(data.learnSeal("0,1,2")&&!data.learnSeal("0,1,2"),"Seal discovery deduplicated");
        data.changeBoost(level,pos,1000);check(data.aura(level,pos).boost()==100,"Aura boost bounded at original maximum");
        var decoded=ArcaneWorldData.CODEC.parse(JsonOps.INSTANCE,ArcaneWorldData.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow()).getOrThrow();
        check(decoded.knownSeals().contains("0,1,2")&&decoded.aura(level,pos).boost()==100,"Seal discovery and aura boost survive persistence");
        var ball=Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemCrystalBall")).findFirst().orElseThrow();var ballStack=item(ball.id());
        ResearchBook.openCrystalBall(player,ballStack,InteractionHand.MAIN_HAND,new int[]{0,1,2},false);
        check(ItemState.getString(ballStack,"book_seals","").contains("0,1,2")&&ItemState.getInt(ballStack,"book_rune_2",-1)==2,"Crystal-ball screen receives known combinations and initial runes");

        var crystal=place(level,pos,"thaumic_crystalizer",player);crystal.setItem(0,item("vis_crystal"));crystal.insertVis(20,false);ticks(crystal,level,900);
        int crystals=0;for(int i=9;i<15;i++){crystals+=crystal.getItem(i).getCount();if(!crystal.getItem(i).isEmpty())check(Content.entry(crystal.getItem(i)).meta()==i-9,"Crystal output placed at its matching element position");}
        check(crystals==1&&crystal.getItem(0).isEmpty()&&crystal.pureVis()<.002f,"Undepleted crystal conversion uses two-thirds of the vis cost");
        var crystalTank=place(level,pos.below(),"vis_storage_tank",player);crystalTank.insertVis(40,false);
        crystal=place(level,pos,"thaumic_crystalizer",player);crystal.setItem(0,item("depleted_crystal"));
        crystal.processes.recipe("crystalization",30,0);crystal.processes.pureWork=29.9995f;
        ticks(crystal,level,1);
        check(crystal.getItem(0).isEmpty()&&crystal.sequence==1,"Network-fed crystalizer pays a final balance below the network transfer minimum");
        check(crystalTank.pureVis()<40&&crystal.pureVis()>0&&crystal.pureVis()<.001f,"Crystalizer consumes only its remaining cost and retains excess transferred vis");
        for(String input:List.of("depleted_crystal","vis_crystal")){
            crystalTank.extractVis(crystalTank.pureVis(),false);crystalTank.insertVis(40,false);
            crystal=place(level,pos,"thaumic_crystalizer",player);crystal.setItem(0,item(input));
            ticks(crystal,level,1400);
            check(crystal.getItem(0).isEmpty()&&crystal.sequence==1,"Network-fed crystalizer completes a full cycle for "+input);
            float crystalCost=input.equals("depleted_crystal")?30:20;
            check(Math.abs(40-crystalTank.pureVis()-crystal.pureVis()-crystalCost)<.002f,"Full crystalizer cycle charges the correct Vis cost for "+input);
        }
        level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());
        var restorer=place(level,pos,"thaumic_restorer",player);for(int i=0;i<6;i++){var tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(1);restorer.setItem(i,tool);}restorer.insertVis(2,false);ticks(restorer,level,1);
        int repaired=0;for(int i=9;i<15;i++)if(!restorer.getItem(i).isEmpty()&&!restorer.getItem(i).isDamaged())repaired++;
        check(repaired==6,"Restorer repairs all six inputs and exposes six outputs");
        var trunk=(dev.thaumcraft.entity.TravelingTrunk)dev.thaumcraft.entity.ModEntities.TYPES.get("traveling_trunk").create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        trunk.setPos(player.getX()+1,player.getY(),player.getZ());trunk.setOwner(player.getUUID());
        var trunkMenu=new TrunkMenu(55,player.getInventory(),trunk,3);
        check(trunkMenu.slots.size()==63&&trunkMenu.status(0)==Math.round(trunk.getHealth()*10),"Trunk has 27 storage slots and synchronized health");
        trunk.setItem(26,new ItemStack(Items.EMERALD,17));trunkMenu.quickMoveStack(player,26);check(trunk.getItem(26).isEmpty(),"Last original trunk slot participates in shift transfer");
        check(trunkMenu.clickMenuButton(player,0)&&trunk.staying()&&trunkMenu.status(2)==1,"Trunk stay checkbox changes behavior");
        trunk.setOwner(UUID.randomUUID());check(!trunkMenu.stillValid(player)&&!trunkMenu.clickMenuButton(player,0),"Non-owner cannot use trunk controls");trunk.setOwner(player.getUUID());
        var roomy=Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemUpgrades")&&e.meta()==5).findFirst().orElseThrow();trunk.installUpgrade(item(roomy.id()));
        check(!trunkMenu.stillValid(player),"Old trunk menu closes when capacity changes");trunkMenu.removed(player);
        trunkMenu=new TrunkMenu(56,player.getInventory(),trunk,4);trunk.setItem(35,new ItemStack(Items.EMERALD,19));trunkMenu.quickMoveStack(player,35);
        check(trunkMenu.slots.size()==72&&trunk.getItem(35).isEmpty(),"All 36 roomy trunk slots are accessible");trunkMenu.removed(player);trunk.discard();
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());return checks;
    }
}
