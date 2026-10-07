package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.CrystalBlock;
import dev.thaumcraft.entity.ModEntities;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.gameplay.GameData;
import dev.thaumcraft.gameplay.VoidNetworks;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineMenu;
import dev.thaumcraft.machine.VoidMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;

/** Opt-in client fixture: exercise resource reload, entity renderers, blocks and a menu. */
public final class ClientSmokeTests {
    private static int ticks;
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError("Client fixture: "+message);}
    private static void screenshot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),1,result->Thaumcraft.LOG.info("Client screenshot: {}",result.getString()));}
    private static BlockPos findInfuser(net.minecraft.world.level.Level level){
        for(int x=0;x<=21;x+=3)for(int z=0;z<=12;z+=3){
            BlockPos p=new BlockPos(x,120,z);
            if(level.getBlockEntity(p) instanceof MachineBlockEntity m&&m.machineId().equals("thaumic_infuser"))return p;
        }
        return null;
    }
    private static boolean occulticAnimationChecked;
    private static dev.thaumcraft.client.legacy.LegacyCompat.TileEntity snapshotInfuser(Minecraft mc){
        BlockPos pos=findInfuser(mc.level);check(pos!=null,"Exhibition infuser found");
        var world=new dev.thaumcraft.client.legacy.LegacyCompat.World(mc.level,0);
        var ref=new dev.thaumcraft.client.legacy.LegacyCompat.TileEntity[1];
        dev.thaumcraft.client.legacy.LegacyCompat.capture(world,pos,0xf000f0,mc.level.getGameTime(),()->{ref[0]=dev.thaumcraft.client.legacy.LegacyVisuals.snapshot(world,pos);});
        return ref[0];
    }
    public static void tick() {
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        if(Boolean.getBoolean("thaumcraft.localizationClientSmoke")){if(LocalizationClientChecks.tick())mc.stop();return;}
        if(mc.getSingleplayerServer()==null)return;
        if(Boolean.getBoolean("thaumcraft.combatMachineVisualSmoke")){if(dev.thaumcraft.client.legacy.CombatMachineVisualChecks.tick())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.addonClientSmoke")){if(AddonClientChecks.tick())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.crucibleMotionSmoke")){if(CrucibleMotionChecks.verify())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.heldToolFallSmoke")){HeldToolFallChecks.verify();mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.jeiSmoke")){if(JeiVisualChecks.tick())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.compassSmoke")){if(dev.thaumcraft.client.VoidCompassChecks.tick())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.monolithSmoke")){if(MonolithVisualChecks.tick())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.portalSmoke")){if(PortalSceneChecks.tick())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.braceletSmoke")){BraceletVisualChecks.verify();mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.wandSmoke")){if(dev.thaumcraft.client.legacy.WandVisualChecks.tick())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.carpetSmoke")){if(CarpetClientChecks.tick())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.sealSmoke")){if(dev.thaumcraft.client.legacy.SealVisualChecks.tick())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.conduitSmoke")){if(ConduitVisualChecks.tick())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.enchanterGuiSmoke")){if(EnchanterGuiChecks.tick())mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.brainJarSmoke")){BrainJarRenderChecks.tick();return;}
        if(!occulticAnimationChecked){dev.thaumcraft.client.legacy.OcculticAnimationChecks.verify();occulticAnimationChecked=true;}
        if(!MonolithVisualChecks.tick())return;
        if(!dev.thaumcraft.client.legacy.DarknessVisualChecks.tick())return;
        if(!QuaesitumParityChecks.tick())return;
        if(!InfuserVisualChecks.tick())return;
        if(Boolean.getBoolean("thaumcraft.infuserSmoke")){mc.stop();return;}
        if(Boolean.getBoolean("thaumcraft.voidChestSmoke")){VoidChestRenderChecks.tick();return;}
        if(!TrunkPacketChecks.tick())return;
        if(Boolean.getBoolean("thaumcraft.guiSmoke")){if(GuiParityChecks.tick(ticks++)){Thaumcraft.LOG.info("THAUMCRAFT_GUI_ONLY_SMOKE_PASS");mc.stop();}return;}
        if(++ticks==20) {
            mc.getWindow().setWindowed(1120,840);mc.options.fullscreen().set(false);
            mc.setScreen(null);
            mc.options.guiScale().set(2);
            mc.options.hideGui=true;
            mc.getSingleplayerServer().execute(()->{
                var server=mc.getSingleplayerServer();var level=server.overworld();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");server.setWeatherParameters(6000,0,false,false);
                level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new net.minecraft.world.phys.AABB(-5,118,-5,29,145,29),entity->!(entity instanceof net.minecraft.world.entity.player.Player)).forEach(net.minecraft.world.entity.Entity::discard);
                for(int x=-3;x<25;x++)for(int z=-3;z<26;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.SMOOTH_STONE.defaultBlockState());
                int i=0;
                for(var block:Content.machineBlocks()) {
                    BlockPos pos=new BlockPos((i%8)*3,120,(i/8)*3);level.setBlockAndUpdate(pos,block.defaultBlockState());i++;
                }
                i=0;
                for(var type:ModEntities.TYPES.entrySet()) {
                    // The default relic is a live singularity, not a static exhibition entity.
                    if(type.getKey().equals("arcane_relic"))continue;
                    var entity=type.getValue().create(level,EntitySpawnReason.COMMAND);
                    if(entity!=null){entity.snapTo((i%8)*3,120,15+(i/8)*3,180,0);if(entity instanceof net.minecraft.world.entity.Mob mob)mob.setNoAi(true);level.addFreshEntity(entity);}i++;
                }
                i=0;
                for(String id:new String[]{"vis_ore","vaporous_vis_ore","aqueous_vis_ore","earthen_vis_ore","fiery_vis_ore","tainted_vis_ore"}) {
                    level.setBlockAndUpdate(new BlockPos(i*3,120,22),Content.block(id).defaultBlockState().setValue(CrystalBlock.AMOUNT,5));i++;
                }
                level.setBlockAndUpdate(new BlockPos(21,120,22),Content.block("taint_spore_pod").defaultBlockState().setValue(dev.thaumcraft.world.TaintPodBlock.AGE,12));
                for(var player:server.getPlayerList().getPlayers()) {
                    player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
                    player.teleportTo(level,11,132,30,Set.of(),180,30,true);
                }
            });
        }
        if(ticks==180)screenshot("thaumcraft-models.png");
        if(ticks==200) {
            mc.options.hideGui=false;
            mc.getSingleplayerServer().execute(()->{
                var server=mc.getSingleplayerServer();
                for(var player:server.getPlayerList().getPlayers()) {
                    BlockPos pos=player.blockPosition().below();
                    var level=player.level();level.setBlockAndUpdate(pos,Content.block("quaesitum").defaultBlockState());
                    if(level.getBlockEntity(pos) instanceof dev.thaumcraft.machine.MachineBlockEntity machine) {machine.setOwner(player.getUUID());player.openMenu(machine);}
                }
            });
        }
        if(ticks==240) {
            check(mc.player.containerMenu instanceof MachineMenu menu&&menu.machineId().equals("quaesitum"),"Quaesitum menu opened");
            screenshot("thaumcraft-quaesitum.png");
        }
        if(ticks==260)mc.getSingleplayerServer().execute(()->{
            var server=mc.getSingleplayerServer();
            for(var player:server.getPlayerList().getPlayers()) {
                player.closeContainer();var level=player.level();BlockPos pos=player.blockPosition().below();
                level.setBlockAndUpdate(pos,Content.block("occultic_enchanter").defaultBlockState());
                var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.setOwner(player.getUUID());
                machine.setItem(0,new ItemStack(Items.DIAMOND_SWORD));
                for(var project:GameData.projects())ArcaneWorldData.researchData(level).unlock(player.getUUID(),project.index());
                check(machine.enchanting.click(2),"Enchanter accepts an available selection");player.openMenu(machine);
            }
        });
        if(ticks==300) {
            check(mc.player.containerMenu instanceof MachineMenu menu&&menu.machineId().equals("occultic_enchanter")&&menu.enchantmentData(15)>0,"Selected enchantment reaches client menu");
            screenshot("thaumcraft-enchanter.png");
        }
        // A 1120 by 840 viewport fits this desktop and permits a compact GUI at scale three.
        if(ticks==310) {mc.options.guiScale().set(4);mc.resizeGui();}
        if(ticks==320) {
            check(mc.getWindow().getGuiScaledWidth()<400,"Compact enchanter viewport reached");
            check(mc.screen instanceof dev.thaumcraft.client.MachineScreen,"Original enchanter remains usable at compact scale");
            int x=(mc.screen.width-176)/2,y=(mc.screen.height-198)/2;
            check(mc.screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x+65,y+78,new net.minecraft.client.input.MouseButtonInfo(0,0)),false),"Selected enchantment glyph accepts compact-screen click");
        }
        if(ticks==335)screenshot("thaumcraft-enchanter-compact.png");
        if(ticks==345) {mc.options.guiScale().set(2);mc.resizeGui();}
        if(ticks==380)mc.getSingleplayerServer().execute(()->{
            var server=mc.getSingleplayerServer();
            for(var player:server.getPlayerList().getPlayers()) {
                player.closeContainer();var level=player.level();BlockPos a=player.blockPosition().below(2),b=a.east(4);
                for(BlockPos pos:java.util.List.of(a,b)) {
                    level.setBlockAndUpdate(pos,Content.block("void_chest").defaultBlockState());
                    var chest=(MachineBlockEntity)level.getBlockEntity(pos);chest.setOwner(player.getUUID());
                    for(int slot=0;slot<72;slot++)chest.setItem(slot,new ItemStack(pos.equals(a)?Content.item("vis_crystal"):Items.EMERALD,slot%64+1));
                    level.setBlockAndUpdate(pos.above(),Content.block("void_interface").defaultBlockState());
                    var portal=(MachineBlockEntity)level.getBlockEntity(pos.above());portal.setOwner(player.getUUID());portal.setChannel(2);VoidNetworks.get(level).register(level,pos.above());
                }
                player.openMenu((MachineBlockEntity)level.getBlockEntity(a.above()));
            }
        });
        if(ticks==420)check(mc.player.containerMenu instanceof VoidMenu menu&&menu.pages()==2&&menu.page()==0&&menu.slots.size()==108,"72-slot interface opens its first linked chest");
        if(ticks==440) {
            check(mc.gameMode!=null,"Client game mode available for menu packet");
            mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,1);
        }
        if(ticks==470) {
            check(mc.player.containerMenu instanceof VoidMenu menu&&menu.page()==1&&menu.getSlot(71).getItem().is(Items.EMERALD),"Page-change packet displays the second backing chest");
            screenshot("thaumcraft-void-interface.png");
        }
        if(ticks==490)mc.getSingleplayerServer().execute(()->{
            for(var player:mc.getSingleplayerServer().getPlayerList().getPlayers()) {
                player.closeContainer();var tome=new ItemStack(Content.item("thaumonomicon"));player.setItemInHand(InteractionHand.MAIN_HAND,tome);
                tome.getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
            }
        });
        if(ticks==530) {
            check(mc.screen instanceof dev.thaumcraft.client.ResearchScreen,"Thaumonomicon opens the original research browser");
            screenshot("thaumcraft-thaumonomicon.png");
        }
        if(ticks==550){
            mc.setScreen(null);mc.options.hideGui=true;
            mc.getSingleplayerServer().execute(()->{
                var server=mc.getSingleplayerServer();var level=server.overworld();
                for(var player:server.getPlayerList().getPlayers()){
                    player.closeContainer();
                    for(BlockPos pos:java.util.List.of(new BlockPos(0,124,30),new BlockPos(15,122,24))){
                        level.setBlockAndUpdate(pos,Content.block("arcane_seal").defaultBlockState().setValue(dev.thaumcraft.machine.MachineBlock.FACING,pos.getX()==0?net.minecraft.core.Direction.SOUTH:net.minecraft.core.Direction.NORTH));
                        var seal=(MachineBlockEntity)level.getBlockEntity(pos);seal.setOwner(player.getUUID());
                        for(int rune=0;rune<2;rune++){
                            int value=rune;var entry=Content.DEFINITIONS.values().stream().filter(e->e.source_class().equals("ItemRunicEssence")&&e.meta()==value).findFirst().orElseThrow();
                            seal.setItem(18+rune,new ItemStack(Content.item(entry.id())));
                        }
                    }
                    // Stay inside the forward activation prism, outside the seal block itself.
                    player.teleportTo(level,.5,123.1,32.5,Set.of(),180,0,true);
                }
            });
        }
        if(ticks==680){
            var seal=(MachineBlockEntity)mc.level.getBlockEntity(new BlockPos(0,124,30));
            check(seal!=null&&seal.visualPortalOpen()&&seal.visualPortalTarget()!=null,"Portal destination is synchronized");
            check(dev.thaumcraft.client.legacy.PortalViews.renderedViews()>5,"Live portal destination rendered into its framebuffer");
            screenshot("thaumcraft-portal.png");
            PortalSceneChecks.capture();
            mc.getSingleplayerServer().execute(()->PortalSceneChecks.build(mc.getSingleplayerServer().overworld()));
        }
        if(ticks==820){PortalSceneChecks.verify();screenshot("thaumcraft-portal-far.png");}
        if(ticks==840)mc.getSingleplayerServer().execute(()->{
            var level=mc.getSingleplayerServer().overworld();RenderParityChecks.build(level);
            for(var player:mc.getSingleplayerServer().getPlayerList().getPlayers())player.teleportTo(level,49,136,62,Set.of(),180,43,true);
        });
        if(ticks==1040){RenderParityChecks.verify();screenshot("thaumcraft-block-states.png");}
        if(ticks==1045)mc.getSingleplayerServer().execute(()->{
            var server=mc.getSingleplayerServer();var level=server.overworld();
            BlockPos pos=findInfuser(level);check(pos!=null,"Exhibition infuser found");
            var machine=(MachineBlockEntity)level.getBlockEntity(pos);
            for(int i=0;i<6;i++)machine.setItem(i,ItemStack.EMPTY);
            machine.setItem(9,ItemStack.EMPTY);machine.setItem(10,ItemStack.EMPTY);machine.setItem(18,ItemStack.EMPTY);
            machine.setItem(0,new ItemStack(Items.GLOWSTONE_DUST,4));machine.setItem(1,new ItemStack(Items.REDSTONE,4));
            machine.extractVis(machine.pureVis(),false);machine.insertVis(20,false);machine.processes.boost=0;
            level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());machine.setChanged();
        });
        if(ticks==1060) {
            var tile=snapshotInfuser(mc);
            check(tile!=null&&tile.sucked>0,"Active infuser publishes absorption");
            check(tile.angle>=0&&tile.angle<=360,"Active infuser angle is finite");
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();var machine=(MachineBlockEntity)level.getBlockEntity(findInfuser(level));
                machine.extractVis(machine.pureVis(),false);
                machine.processes.pureWork=2.5f;machine.setChanged();
            });
        }
        if(ticks==1070) {
            check(snapshotInfuser(mc).angle==90,"Infuser angle at 25%");
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();var machine=(MachineBlockEntity)level.getBlockEntity(findInfuser(level));
                machine.processes.pureWork=5;machine.setChanged();
            });
        }
        if(ticks==1080) {
            check(snapshotInfuser(mc).angle==180,"Infuser angle at 50%");
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();var machine=(MachineBlockEntity)level.getBlockEntity(findInfuser(level));
                machine.processes.pureWork=7.5f;machine.setChanged();
            });
        }
        if(ticks==1090) {
            check(snapshotInfuser(mc).angle==270,"Infuser angle at 75%");
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();var machine=(MachineBlockEntity)level.getBlockEntity(findInfuser(level));
                machine.processes.pureWork=0;machine.setChanged();
            });
        }
        if(ticks==1100) {
            check(snapshotInfuser(mc).angle==0,"Infuser angle at 0%");
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();BlockPos pos=findInfuser(level);var machine=(MachineBlockEntity)level.getBlockEntity(pos);
                machine.extractVis(machine.pureVis(),false);
                machine.processes.pureWork=5;machine.setChanged();
            });
        }
        if(ticks==1110) {
            var tile=snapshotInfuser(mc);
            check(tile.sucked==0&&tile.angle==180,"Starved infuser keeps angle with no absorption");
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();BlockPos pos=findInfuser(level);var machine=(MachineBlockEntity)level.getBlockEntity(pos);
                machine.insertVis(10,false);
                level.setBlockAndUpdate(pos.above(),Blocks.REDSTONE_BLOCK.defaultBlockState());machine.setChanged();
            });
        }
        if(ticks==1120) {
            check(snapshotInfuser(mc).sucked==0,"Powered infuser absorbs nothing");
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();BlockPos pos=findInfuser(level);var machine=(MachineBlockEntity)level.getBlockEntity(pos);
                level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
                machine.setItem(9,new ItemStack(Items.DIRT,5));machine.setChanged();
            });
        }
        if(ticks==1130) {
            check(snapshotInfuser(mc).sucked==0,"Output-blocked infuser absorbs nothing");
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();BlockPos pos=findInfuser(level);var machine=(MachineBlockEntity)level.getBlockEntity(pos);
                machine.setItem(9,ItemStack.EMPTY);
                machine.extractVis(machine.pureVis(),false);machine.insertVis(20,false);
                machine.setItem(18,new ItemStack(Content.item("quicksilver_core")));machine.setChanged();
            });
        }
        if(ticks==1140) {
            var tile=snapshotInfuser(mc);
            check(tile.sucked>0,"Speed-upgraded infuser still absorbs");
            screenshot("thaumcraft-infuser.png");
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();BlockPos pos=findInfuser(level);var machine=(MachineBlockEntity)level.getBlockEntity(pos);
                for(int i=0;i<6;i++)machine.setItem(i,ItemStack.EMPTY);machine.setItem(18,ItemStack.EMPTY);machine.setChanged();
            });
        }
        if(ticks==1150) {
            check(snapshotInfuser(mc).sucked==0,"No absorption particles after absorption stops");
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();BlockPos pos=findInfuser(level);var m=(MachineBlockEntity)level.getBlockEntity(pos);
                for(int i=0;i<6;i++)m.setItem(i,ItemStack.EMPTY);
                m.setItem(9,ItemStack.EMPTY);m.setItem(10,ItemStack.EMPTY);m.setItem(18,ItemStack.EMPTY);
                m.setItem(0,new ItemStack(Items.GLOWSTONE_DUST,2));m.setItem(1,new ItemStack(Items.REDSTONE,2));
                m.extractVis(m.pureVis(),false);m.processes.tick(level);
                m.extractVis(m.pureVis(),false);m.insertVis(10,false);m.processes.boost=0;
                boolean sawLastTick=false;
                for(int i=0;i<40&&!sawLastTick;i++) {
                    boolean hadOutput=!m.getItem(9).isEmpty();
                    m.processes.tick(level);
                    if(!hadOutput&&!m.getItem(9).isEmpty()) {
                        check(m.processes.infuserAbsorbed>0&&m.processes.pureWork==0,"Last craft tick keeps absorption after progress resets");
                        sawLastTick=true;
                    }
                }
                check(sawLastTick,"Infuser completed for last-tick check");
            });
        }
        if(ticks>=1160&&GuiParityChecks.tick(ticks-1080)){Thaumcraft.LOG.info("THAUMCRAFT_CLIENT_SMOKE_PASS: blocks and state matrix, entities, all original machine and item screens, compact enchanter, 72-slot interface page packet and live portal rendered");mc.stop();}
    }
}
