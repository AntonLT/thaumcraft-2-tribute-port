package dev.alts.tc2tppatches;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/** Items each player has dissolved in a crucible, keyed by player UUID. Stored with the overworld. */
public final class VisKnowledge extends SavedData {
    private static final Codec<VisKnowledge> CODEC=Codec.unboundedMap(Codec.STRING,Identifier.CODEC.listOf())
            .xmap(VisKnowledge::new,data->data.known);
    public static final SavedDataType<VisKnowledge> TYPE=new SavedDataType<>(AltsPatches.id("vis_knowledge"),VisKnowledge::new,CODEC,null);
    private final Map<String,List<Identifier>> known=new HashMap<>();

    public VisKnowledge() {}
    private VisKnowledge(Map<String,List<Identifier>> saved) {
        saved.forEach((player,items)->known.put(player,new ArrayList<>(new LinkedHashSet<>(items))));
    }
    public static VisKnowledge get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(TYPE);}

    public boolean knows(UUID player,Identifier item){return known.getOrDefault(player.toString(),List.of()).contains(item);}
    public List<Identifier> known(UUID player){return List.copyOf(known.getOrDefault(player.toString(),List.of()));}
    public boolean learn(UUID player,Identifier item) {
        var items=known.computeIfAbsent(player.toString(),ignored->new ArrayList<>());
        if(items.contains(item))return false;
        items.add(item);setDirty();return true;
    }
    public boolean forget(UUID player,Identifier item) {
        var items=known.get(player.toString());
        if(items==null||!items.remove(item))return false;
        setDirty();return true;
    }
}
