package dev.thaumcraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/** Portable-hole originals are persisted before a block is replaced, including across restarts. */
public final class TemporarySpace extends SavedData {
    public record Restoration(BlockPos pos,BlockState state,long expires,Optional<CompoundTag> blockEntity) {
        static final Codec<Restoration> CODEC=RecordCodecBuilder.create(i->i.group(BlockPos.CODEC.fieldOf("pos").forGetter(Restoration::pos),
                BlockState.CODEC.fieldOf("state").forGetter(Restoration::state),Codec.LONG.fieldOf("expires").forGetter(Restoration::expires),
                CompoundTag.CODEC.optionalFieldOf("blockEntity").forGetter(Restoration::blockEntity)).apply(i,Restoration::new));
    }
    public static final Codec<TemporarySpace> CODEC=Restoration.CODEC.listOf().xmap(TemporarySpace::new,d->new ArrayList<>(d.restorations.values()));
    private static final SavedDataType<TemporarySpace> TYPE=new SavedDataType<>(Thaumcraft.id("temporary_space"),TemporarySpace::new,CODEC,null);
    private final Map<BlockPos,Restoration> restorations=new HashMap<>();
    public TemporarySpace() {}
    private TemporarySpace(List<Restoration> saved){saved.forEach(r->restorations.put(r.pos(),r));}
    public static TemporarySpace get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
    public int open(ServerLevel level,BlockPos start,Direction inward){return open(level,start,inward,32);}
    public int open(ServerLevel level,BlockPos start,Direction inward,int remainingCharge){return open(level,start,inward,remainingCharge,null);}
    /** {@code actor} is the player using the hole, reported to {@code BLOCK_REMOVING} listeners. */
    public int open(ServerLevel level,BlockPos start,Direction inward,int remainingCharge,java.util.UUID actor) {
        var pending=new LinkedHashMap<BlockPos,BlockState>();int length=-1;
        boolean horizontal=inward.getAxis().isHorizontal();
        for(int distance=0;distance<32;distance++){
            BlockPos upper=start.relative(inward,distance);boolean exit=true;
            for(int row=0;row<(horizontal?2:1);row++){
                BlockPos pos=upper.below(row);
                if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos))return 0;
                BlockState state=level.getBlockState(pos);
                if(state.getDestroySpeed(level,pos)<0||(!horizontal&&state.hasBlockEntity())||!state.getFluidState().isEmpty()||state.is(Content.TEMPORARY_SPACE)||state.is(dev.thaumcraft.content.ModTags.PORTABLE_HOLE_IMMUNE))return 0;
                if(!state.isAir())exit=false;
                if(!state.isAir()&&!dev.thaumcraft.api.ThaumcraftEvents.removalAllowed(level,pos,state,actor))return 0;
                pending.put(pos.immutable(),state);
            }
            if(exit){
                length=distance;
                pending.remove(upper);if(horizontal)pending.remove(upper.below());
                break;
            }
        }
        if(length<=0||length>remainingCharge||restorations.size()+pending.size()>4096)return 0;
        long expires=level.getGameTime()+Math.max(20*length,50);
        for(var entry:pending.entrySet()){
            var pos=entry.getKey();
            var entity=level.getBlockEntity(pos);
            var tag=entity==null?Optional.<CompoundTag>empty():Optional.of(entity.saveWithFullMetadata(level.registryAccess()));
            restorations.put(pos,new Restoration(pos,entry.getValue(),expires,tag));
        }
        setDirty();
        for(var pos:pending.keySet()){
            // Suppress container removal side effects so saved inventories cannot spill.
            level.setBlock(pos,Content.TEMPORARY_SPACE.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            level.scheduleTick(pos,Content.TEMPORARY_SPACE,Math.max(20*length,50));
        }
        return length;
    }
    public void restore(ServerLevel level,BlockPos pos) {
        Restoration saved=restorations.get(pos);
        if(saved==null){if(level.getBlockState(pos).is(Content.TEMPORARY_SPACE))level.removeBlock(pos,false);return;}
        if(level.getGameTime()<saved.expires() || !level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(pos)).isEmpty()) {
            level.scheduleTick(pos,Content.TEMPORARY_SPACE,20);return;
        }
        if(level.getBlockState(pos).is(Content.TEMPORARY_SPACE)){
            BlockEntity entity=saved.blockEntity().map(tag->BlockEntity.loadStatic(pos,saved.state(),tag.copy(),level.registryAccess())).orElse(null);
            if(saved.blockEntity().isPresent()&&entity==null){level.scheduleTick(pos,Content.TEMPORARY_SPACE,20);return;}
            if(!level.setBlock(pos,saved.state(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE)){
                level.scheduleTick(pos,Content.TEMPORARY_SPACE,20);return;
            }
            if(entity!=null){level.setBlockEntity(entity);entity.setChanged();}
            level.updateNeighborsAt(pos,saved.state().getBlock());
        }
        restorations.remove(pos);setDirty();
    }
}
