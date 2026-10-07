package dev.thaumcraft.world;

import com.mojang.serialization.Codec;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.SealLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

/** Persistent replacement for SpecialTileHM/addActiveChunks and the wand's chunk outline. */
public final class ChunkAnchors extends SavedData {
    private static final Codec<ChunkAnchors> CODEC=Codec.LONG.listOf().xmap(ChunkAnchors::new,d->d.positions.stream().toList());
    private static final SavedDataType<ChunkAnchors> TYPE=new SavedDataType<>(Thaumcraft.id("chunk_anchors"),ChunkAnchors::new,CODEC,null);
    private final Set<Long> positions=new LinkedHashSet<>();
    private final Map<UUID,Overlay> overlays=new HashMap<>();
    private record Overlay(long until,int height) {}
    public ChunkAnchors(){}
    private ChunkAnchors(List<Long> saved){positions.addAll(saved);}
    private static ChunkAnchors get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
    public static void register(ServerLevel level,BlockPos pos){var data=get(level);if(data.positions.add(pos.asLong()))data.setDirty();}
    public static void remove(ServerLevel level,BlockPos pos){var data=get(level);if(data.positions.remove(pos.asLong()))data.setDirty();}
    public static void toggleOverlay(ServerPlayer player,BlockPos pos){
        var data=get(player.level());var previous=data.overlays.get(player.getUUID());
        if(previous!=null&&previous.until()>player.level().getGameTime())data.overlays.remove(player.getUUID());
        else data.overlays.put(player.getUUID(),new Overlay(player.level().getGameTime()+2400,pos.getY()));
    }
    public static int radius(MachineBlockEntity machine){
        if(machine.machineId().equals("void_interface"))return 0;
        if(!machine.machineId().equals("arcane_seal"))return -1;
        int[] r=SealLogic.runes(machine);
        if(r[0]!=0)return -1;
        if(r[1]==1)return 0;
        return r[1]==3?(r[2]==-1?1:r[2]==3?2:-1):-1;
    }
    /**
     * Keeps every chunk within {@code area} of {@code chunk} block-ticking (tile entities, crops, random ticks), like the
     * original active chunks. Ticket level is 33-radius and block ticking needs 32, hence one ring beyond the area.
     */
    public static void ticket(ServerLevel level,ChunkPos chunk,int area){level.getChunkSource().addTicketWithRadius(Thaumcraft.ANCHOR_TICKET,chunk,area+1);}
    public static void tick(ServerLevel level){
        if(level.getGameTime()%20!=0)return;
        var data=get(level);Set<ChunkPos> active=new HashSet<>();
        for(long packed:new ArrayList<>(data.positions)){
            BlockPos pos=BlockPos.of(packed);
            if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)){remove(level,pos);continue;}
            level.getChunkAt(pos);
            if(!(level.getBlockEntity(pos) instanceof MachineBlockEntity machine)){remove(level,pos);continue;}
            int radius=radius(machine);if(radius<0){remove(level,pos);continue;}
            ChunkPos chunk=new ChunkPos(pos.getX()>>4,pos.getZ()>>4);ticket(level,chunk,radius);
            for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)active.add(new ChunkPos(chunk.x()+x,chunk.z()+z));
        }
        data.overlays.entrySet().removeIf(e->e.getValue().until()<=level.getGameTime()||level.getPlayerByUUID(e.getKey())==null);
        for(var entry:data.overlays.entrySet()){
            if(!(level.getPlayerByUUID(entry.getKey()) instanceof ServerPlayer player))continue;
            double y=entry.getValue().height()+.5;
            for(ChunkPos chunk:active){
                double x=chunk.getMinBlockX(),z=chunk.getMinBlockZ(),offset=level.getRandom().nextDouble()*16;
                if(player.distanceToSqr(x+8,y,z+8)>128*128)continue;
                // Server particles preserve the original visible chunk edges on both loaders.
                level.sendParticles(player,ParticleTypes.END_ROD,false,false,x,y,z+offset,1,0,0,0,.015);
                level.sendParticles(player,ParticleTypes.END_ROD,false,false,x+16,y,z+offset,1,0,0,0,.015);
                level.sendParticles(player,ParticleTypes.END_ROD,false,false,x+offset,y,z,1,0,0,0,.015);
                level.sendParticles(player,ParticleTypes.END_ROD,false,false,x+offset,y,z+16,1,0,0,0,.015);
            }
        }
    }
}
