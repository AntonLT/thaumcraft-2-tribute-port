package dev.thaumcraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.thaumcraft.Thaumcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Original foliage states survive chunk unload and world restart until purification. */
public final class TaintMemory extends SavedData {
    public record Entry(BlockPos pos,BlockState state) {
        public static final Codec<Entry> CODEC=RecordCodecBuilder.create(i->i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),BlockState.CODEC.fieldOf("state").forGetter(Entry::state)).apply(i,Entry::new));
    }
    public static final Codec<TaintMemory> CODEC=Entry.CODEC.listOf().xmap(TaintMemory::new,data->data.originals.entrySet().stream().map(e->new Entry(e.getKey(),e.getValue())).toList());
    private static final SavedDataType<TaintMemory> TYPE=new SavedDataType<>(Thaumcraft.id("taint_memory"),TaintMemory::new,CODEC,null);
    private final Map<BlockPos,BlockState> originals=new HashMap<>();
    public TaintMemory(){this(List.of());}
    private TaintMemory(List<Entry> entries){for(Entry entry:entries)originals.put(entry.pos().immutable(),entry.state());}
    public static TaintMemory get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
    public void remember(BlockPos pos,BlockState state){originals.put(pos.immutable(),state);setDirty();}
    public Optional<BlockState> original(BlockPos pos){return Optional.ofNullable(originals.get(pos));}
    public Optional<BlockState> consume(BlockPos pos){BlockState state=originals.remove(pos);if(state!=null)setDirty();return Optional.ofNullable(state);}
    public void forget(BlockPos pos){consume(pos);}
}
