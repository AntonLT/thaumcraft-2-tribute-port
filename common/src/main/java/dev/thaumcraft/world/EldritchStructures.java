package dev.thaumcraft.world;

import com.google.gson.Gson;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Original 15×15 chamber blueprints and entrance elevation. */
public final class EldritchStructures {
    private static final int[][][] ENTRANCE=load("TileVoidCube"),TREASURE=load("TileVoidLock");
    public static final ResourceKey<LootTable> LOOT=ResourceKey.create(Registries.LOOT_TABLE,Thaumcraft.id("chests/eldritch"));
    private static int[][][] load(String name) {
        try(var stream=EldritchStructures.class.getResourceAsStream("/thaumcraft2tp/"+name+".json")) {
            if(stream==null)throw new IllegalStateException("Missing chamber "+name);
            return new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),int[][][].class);
        } catch(Exception e) {throw new ExceptionInInitializerError(e);}
    }
    private EldritchStructures() {}
    private static boolean blocksGeneration(ServerLevel level,BlockPos pos,BlockPos initiating){
        var entity=level.getBlockEntity(pos);
        // Original chamber/shaft placement replaces crystal tiles; modern visual entities
        // contain no independent data and must not turn natural crystals into blockers.
        if(entity==null)return false;
        if(pos.equals(initiating))return false;
        if(level.getBlockState(pos).is(Content.block("eldritch_receptacle")))
            return !(level.getBlockEntity(initiating) instanceof MonolithBlockEntity core&&MonolithBlockEntity.coreAt(level,pos)==core&&core.index(pos)>=0);
        return !(entity instanceof VisualBlockEntity);
    }
    private static boolean prepare(ServerLevel level,BlockPos center,int layers,BlockPos replaceable) {
        if(!level.getWorldBorder().isWithinBounds(center.offset(-7,0,-7))||!level.getWorldBorder().isWithinBounds(center.offset(7,layers,7)))return false;
        for(int x=-7;x<=7;x+=7)for(int z=-7;z<=7;z+=7)level.getChunkAt(center.offset(x,0,z));
        for(BlockPos p:BlockPos.betweenClosed(center.offset(-7,0,-7),center.offset(7,layers,7)))
            if(level.isOutsideBuildHeight(p)||blocksGeneration(level,p,replaceable))return false;
        return true;
    }
    public static boolean openEntrance(ServerLevel level,BlockPos core) {
        // Older port worlds could generate cores below Y20; keep those puzzles solvable.
        BlockPos base=new BlockPos(core.getX(),core.getY()<20?level.getMinY()+12:6,core.getZ());
        if(core.getY()<base.getY()+8)return false;
        if(!prepare(level,base,8,core))return false;
        for(BlockPos p:BlockPos.betweenClosed(base.above(8).offset(-1,0,-1),core.offset(1,0,1)))if(blocksGeneration(level,p,core))return false;
        build(level,base,ENTRANCE,false);
        BlockState wall=Content.block("eldritch_structure_stone").defaultBlockState();
        for(int y=base.getY()+8;y<=core.getY();y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
            BlockPos p=new BlockPos(core.getX()+x,y,core.getZ()+z);
            level.setBlock(p,x==0&&z==0?Blocks.AIR.defaultBlockState():wall,3);
        }
        dev.thaumcraft.content.ModSounds.play(level,core,"rumble",net.minecraft.sounds.SoundSource.BLOCKS,4,1);
        dev.thaumcraft.network.EquipmentEffect.send(level,dev.thaumcraft.network.EquipmentEffect.MONOLITH,net.minecraft.world.phys.Vec3.atLowerCornerOf(core));
        return true;
    }
    public static boolean unlockRoom(ServerLevel level,BlockPos lock,Direction direction) {
        // Locks sit three layers above the floor, including in already generated port chambers.
        BlockPos center=lock.below(3).relative(direction,7);
        if(!prepare(level,center,7,lock))return false;
        build(level,center,TREASURE,true);
        for(int across=-2;across<=2;across++)for(int y=-2;y<=2;y++)level.setBlockAndUpdate(lock.relative(direction.getClockWise(),across).above(y),Content.block("eldritch_structure_stone").defaultBlockState());
        for(int across=-1;across<=1;across++)for(int y=-1;y<=1;y++)for(int depth=0;depth<=2;depth++) {
            BlockPos p=lock.relative(direction,depth).relative(direction.getClockWise(),across).above(y);
            level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
            dev.thaumcraft.network.EquipmentEffect.send(level,dev.thaumcraft.network.EquipmentEffect.VOID_POOF,net.minecraft.world.phys.Vec3.atLowerCornerOf(p));
        }
        BlockPos rumble=lock.relative(direction,7);
        dev.thaumcraft.content.ModSounds.playAt(level,rumble.getX(),rumble.getY(),rumble.getZ(),"rumble",net.minecraft.sounds.SoundSource.BLOCKS,4,1);
        return true;
    }
    private static void build(ServerLevel level,BlockPos center,int[][][] blueprint,boolean treasure) {
        BlockState stone=Content.block("eldritch_structure_stone").defaultBlockState();
        for(int y=0;y<blueprint.length;y++)for(int x=0;x<15;x++)for(int z=0;z<15;z++) {
            int code=blueprint[y][x][z];if(code<0)continue;
            BlockPos pos=center.offset(x-7,y,z-7);
            BlockState state=switch(code) {
                case 0 -> Blocks.AIR.defaultBlockState();
                case 4 -> !level.getBlockState(pos.below()).isAir()&&level.getRandom().nextInt(8)==0?Content.block("void_interface").defaultBlockState():Blocks.AIR.defaultBlockState();
                case 2 -> (treasure?level.getRandom().nextInt(3)==0:level.getRandom().nextInt(4)!=0)?Content.block("void_chest").defaultBlockState():Blocks.AIR.defaultBlockState();
                case 3 -> treasure?Content.block("eldritch_monolith").defaultBlockState():stone;
                case 5,6,7,8 -> Content.block("eldritch_lock").defaultBlockState().setValue(EldritchBlock.FACING,switch(code){case 5->Direction.WEST;case 6->Direction.EAST;case 7->Direction.NORTH;default->Direction.SOUTH;});
                default -> stone;
            };
            level.setBlock(pos,state,3);
            if(level.getBlockEntity(pos) instanceof dev.thaumcraft.machine.MachineBlockEntity machine){
                if(state.is(Content.block("void_chest"))){
                    dev.thaumcraft.gameplay.ArtifactLoot.fillOriginalEldritch(machine,level.getRandom());
                    if(treasure){
                        if(level.getRandom().nextInt(4)==0)machine.setItem(38,new net.minecraft.world.item.ItemStack(Content.item("void_compass")));
                        if(level.getRandom().nextInt(4)==0)machine.setItem(40,new net.minecraft.world.item.ItemStack(Content.item("void_crusher")));
                        if(level.getRandom().nextInt(4)==0)machine.setItem(42,new net.minecraft.world.item.ItemStack(Content.item("void_cutter")));
                    }
                } else if(state.is(Content.block("void_interface"))){machine.setChannel(level.getRandom().nextInt(6));dev.thaumcraft.gameplay.VoidNetworks.get(level).register(level,pos);}
            }
        }
    }
}
