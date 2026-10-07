package dev.thaumcraft.world;

import com.mojang.serialization.Codec;
import dev.thaumcraft.Thaumcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EldritchIndex extends SavedData {
    private static final Codec<EldritchIndex> CODEC=BlockPos.CODEC.listOf().xmap(EldritchIndex::new,EldritchIndex::snapshot);
    private static final SavedDataType<EldritchIndex> TYPE=new SavedDataType<>(Thaumcraft.id("monoliths"),EldritchIndex::new,CODEC,null);
    private final List<BlockPos> monoliths;
    private final java.util.Set<BlockPos> generating=new java.util.HashSet<>();
    // Loaded on the server thread before chunk generation; workers never touch SavedDataStorage.
    private static final java.util.Map<ServerLevel,EldritchIndex> LEVELS=new java.util.WeakHashMap<>();
    public EldritchIndex() {this(List.of());}
    private EldritchIndex(List<BlockPos> positions) {monoliths=new ArrayList<>(positions);}
    public static synchronized EldritchIndex get(ServerLevel level) {
        var index=LEVELS.get(level);
        if(index==null){
            if(!level.getServer().isSameThread())throw new IllegalStateException("Monolith index was not initialized before generation");
            index=level.getDataStorage().computeIfAbsent(TYPE);LEVELS.put(level,index);
        }
        return index;
    }
    private synchronized List<BlockPos> snapshot(){return List.copyOf(monoliths);}
    public synchronized boolean reserve(BlockPos pos){
        if(monoliths.stream().anyMatch(p->p.distSqr(pos)<=300*300)||generating.stream().anyMatch(p->p.distSqr(pos)<=300*300))return false;
        return generating.add(pos.immutable());
    }
    public synchronized void cancel(BlockPos pos){generating.remove(pos);}
    public synchronized void record(BlockPos pos) {generating.remove(pos);if(!monoliths.contains(pos)){monoliths.add(pos.immutable());setDirty();}}
    public synchronized Optional<BlockPos> closest(BlockPos pos) {return monoliths.stream().min(java.util.Comparator.comparingDouble(p->p.distSqr(pos)));}
    public Optional<BlockPos> compassTarget(ServerLevel level,net.minecraft.world.phys.Vec3 player){
        // 1.2.5 World.getBlockTileEntity loaded the chunk, even outside the player's view.
        // Check nearest first so we need not load every recorded monolith to find the same minimum.
        return snapshot().stream().sorted(java.util.Comparator.comparingDouble(p->player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p))))
                .filter(p->level.getBlockEntity(p)!=null).findFirst();
    }
    public Optional<BlockPos> closest(ServerLevel level,BlockPos pos){
        var removed=snapshot().stream().filter(candidate->{
            var chunk=level.getChunkSource().getChunkNow(candidate.getX()>>4,candidate.getZ()>>4);
            return chunk!=null&&!chunk.getBlockState(candidate).is(dev.thaumcraft.content.Content.block("eldritch_core"));
        }).toList();
        synchronized(this){if(monoliths.removeAll(removed))setDirty();}
        return closest(pos);
    }
}
