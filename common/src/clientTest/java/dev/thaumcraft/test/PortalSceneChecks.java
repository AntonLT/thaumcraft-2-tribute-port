package dev.thaumcraft.test;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlock;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.world.SealPortals;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Exercise real loader packets into a destination unavailable in the player's chunk cache. */
public final class PortalSceneChecks {
    public static final BlockPos SOURCE=new BlockPos(0,124,30),TARGET=new BlockPos(8192,300,30);
    private static net.minecraft.client.renderer.LevelRenderer mainRenderer;
    private static net.minecraft.client.multiplayer.ClientChunkCache mainChunks;
    private static int ticks,mainDistance;
    public static boolean tick(){
        var mc=Minecraft.getInstance();
        if(++ticks==1){
            mc.setScreen(null);mc.options.hideGui=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);
            mainDistance=mc.options.getEffectiveRenderDistance();capture();
            mc.getSingleplayerServer().execute(()->{
                var level=mc.getSingleplayerServer().overworld();
                mc.getSingleplayerServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
                SealPortals.remove(level,TARGET);level.removeBlock(TARGET,false);
                level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,new AABB(TARGET).inflate(12)).forEach(net.minecraft.world.entity.Entity::discard);
                for(var player:mc.getSingleplayerServer().getPlayerList().getPlayers()){
                    player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                    player.teleportTo(level,.5,123.1,32.5,java.util.Set.of(),180,0,true);
                    level.setBlockAndUpdate(SOURCE.south(2).below(2),Blocks.STONE.defaultBlockState());
                    for(var pos:java.util.List.of(SOURCE,new BlockPos(15,122,24))){
                        level.setBlockAndUpdate(pos,Content.block("arcane_seal").defaultBlockState().setValue(MachineBlock.FACING,pos.equals(SOURCE)?Direction.SOUTH:Direction.NORTH));
                        var seal=(MachineBlockEntity)level.getBlockEntity(pos);seal.setOwner(player.getUUID());
                        for(int rune=0;rune<2;rune++){
                            int value=rune;var entry=Content.DEFINITIONS.values().stream().filter(e->e.source_class().equals("ItemRunicEssence")&&e.meta()==value).findFirst().orElseThrow();
                            seal.setItem(18+rune,new ItemStack(Content.item(entry.id())));
                        }
                    }
                }
            });
        }
        SodiumPortalChecks.tick(ticks);
        if(ticks==200){
            check(dev.thaumcraft.client.legacy.PortalViews.renderedViews()>5,"near destination renders with the installed terrain renderer");
            ShaderPortalChecks.captureNear();
            mc.getSingleplayerServer().execute(()->build(mc.getSingleplayerServer().overworld()));
        }
        if(ticks==500){
            verify();
            check(mc.options.getEffectiveRenderDistance()==mainDistance,"portal distance override is cleared after rendering");
            check(mc.options.renderDistance().get()==8,"saved render-distance option is unchanged");
            ShaderPortalChecks.reload();
            dev.thaumcraft.client.legacy.PortalViews.reset();
            ShaderPortalChecks.closed();
            check(mc.levelRenderer==mainRenderer,"portal cleanup preserves the main renderer");
            dev.thaumcraft.Thaumcraft.LOG.info("THAUMCRAFT_PORTAL_SMOKE_PASS");return true;
        }
        return false;
    }
    public static void capture(){var mc=Minecraft.getInstance();mainRenderer=mc.levelRenderer;mainChunks=mc.level.getChunkSource();ShaderPortalChecks.capture();}
    private static void check(boolean value,String message){if(!value)throw new AssertionError("Remote portal: "+message);}
    public static void build(ServerLevel level){
        level.removeBlock(new BlockPos(15,122,24),false);
        // Replace the earlier buried fixture when reusing a client smoke world.
        var previousTarget=new BlockPos(TARGET.getX(),124,TARGET.getZ());
        SealPortals.remove(level,previousTarget);level.removeBlock(previousTarget,false);
        for(BlockPos pos:BlockPos.betweenClosed(TARGET.offset(-7,-2,-9),TARGET.offset(7,-2,4))){level.getChunkAt(pos);level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());}
        level.setBlockAndUpdate(TARGET,Content.block("arcane_seal").defaultBlockState().setValue(MachineBlock.FACING,Direction.NORTH));
        var seal=(MachineBlockEntity)level.getBlockEntity(TARGET);
        for(int rune=0;rune<2;rune++){
            int value=rune;var entry=Content.DEFINITIONS.values().stream().filter(e->e.source_class().equals("ItemRunicEssence")&&e.meta()==value).findFirst().orElseThrow();
            seal.setItem(18+rune,new ItemStack(Content.item(entry.id())));
        }
        level.setBlockAndUpdate(TARGET.north(6),Blocks.GOLD_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(TARGET.north(6).above(),Blocks.GLOWSTONE.defaultBlockState());
        level.setBlockAndUpdate(TARGET.north(5).east(2),Blocks.CHEST.defaultBlockState());
        var wisp=dev.thaumcraft.entity.ModEntities.WISP.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        wisp.snapTo(TARGET.getX()+.5,TARGET.getY()+1,TARGET.getZ()-4.5,0,0);wisp.setNoAi(true);wisp.setCustomName(Component.literal("Remote wisp"));wisp.setCustomNameVisible(true);level.addFreshEntity(wisp);
        var item=new net.minecraft.world.entity.item.ItemEntity(level,TARGET.getX()+2.5,TARGET.getY(),TARGET.getZ()-3.5,new ItemStack(Items.DIAMOND,3));item.setNoGravity(true);item.setDeltaMovement(0,0,0);level.addFreshEntity(item);
        SealPortals.tick(level,seal,-1);
    }
    public static void verify(){
        var mc=Minecraft.getInstance();
        check(mc.levelRenderer==mainRenderer&&mc.level.getChunkSource()==mainChunks,"main renderer and chunk cache identities remain intact");
        check(!dev.thaumcraft.client.legacy.PortalViews.loaded(mc.level,TARGET),"main player cache remains free of far destination chunks");
        var remote=dev.thaumcraft.client.legacy.PortalViews.remoteLevel(SOURCE);
        check(remote!=null&&remote!=mc.level,"far destination owns an isolated client level");
        check(remote.getBlockState(TARGET.north(6)).is(Blocks.GOLD_BLOCK),"server gold marker arrives through chunk snapshot");
        check(remote.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,TARGET.north(6).above())==15,"actual server block light arrives");
        var chest=remote.getBlockEntity(TARGET.north(5).east(2));
        check(chest instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,"remote chunk restores its chest block entity");
        check(remote.getBlockEntity(TARGET) instanceof MachineBlockEntity seal&&seal.visualRune(0)==0&&seal.visualRune(1)==1,"actual seal renderer data arrives in block entity update tags");
        check(remote.getEntitiesOfClass(dev.thaumcraft.entity.WispEntity.class,new AABB(TARGET).inflate(10)).stream().anyMatch(e->e.hasCustomName()&&e.getCustomName().getString().equals("Remote wisp")),"far creature type and tracked custom name arrive");
        check(remote.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(TARGET).inflate(10)).stream().anyMatch(e->e.getItem().is(Items.DIAMOND)&&e.getItem().getCount()==3),"far dropped item type and count arrive");
        check(dev.thaumcraft.client.legacy.PortalViews.remoteTerrainReady(SOURCE,TARGET.north(6)),"gold marker section is compiled and terrain is visible");
        check(remote.getBrightness(net.minecraft.world.level.LightLayer.SKY,TARGET.north(6).above(2))==15,"open destination sky light reaches scene entities");
        check(dev.thaumcraft.client.legacy.PortalViews.remoteFrames(SOURCE)>5,"destination frames continue rendering");
        ShaderPortalChecks.verify();SodiumTerrainChecks.verify();
        var visualWorld=new dev.thaumcraft.client.legacy.LegacyCompat.World(mc.level,SOURCE.asLong());
        dev.thaumcraft.client.legacy.LegacyCompat.capture(visualWorld,SOURCE,0xf000f0,mc.level.getGameTime(),()->{
            var sourceVisual=dev.thaumcraft.client.legacy.LegacyVisuals.snapshot(visualWorld,SOURCE);
            check(sourceVisual!=null&&sourceVisual.pSize>=1.3f,"source portal stays fully expanded across remote-level render passes");
        });
        net.minecraft.client.Screenshot.grab(mc.gameDirectory,"thaumcraft-portal-far-framebuffer.png",dev.thaumcraft.client.legacy.PortalViews.framebuffer(SOURCE),1,message->dev.thaumcraft.Thaumcraft.LOG.info("Remote framebuffer screenshot: {}",message.getString()));
    }
}
