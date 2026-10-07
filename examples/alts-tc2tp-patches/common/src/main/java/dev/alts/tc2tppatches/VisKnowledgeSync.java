package dev.alts.tc2tppatches;

import dev.thaumcraft.api.ThaumcraftApi;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The player's learned items with their current vis values. Values are computed on the server because recipe-derived
 * vis needs the server's recipes. Always the full list, so it also replaces stale values after a reload.
 */
public record VisKnowledgeSync(Map<Identifier,Float> values) implements CustomPacketPayload {
    public static final Type<VisKnowledgeSync> TYPE=new Type<>(AltsPatches.id("vis_knowledge"));
    private static final int MAX_ITEMS=16384;
    public VisKnowledgeSync {values=Map.copyOf(values);if(values.size()>MAX_ITEMS)throw new IllegalArgumentException("Too many known items");}
    public static BiConsumer<ServerPlayer,VisKnowledgeSync> sender=(player,payload)->{};

    public static final StreamCodec<RegistryFriendlyByteBuf,VisKnowledgeSync> CODEC=new StreamCodec<>() {
        public VisKnowledgeSync decode(RegistryFriendlyByteBuf b) {
            int size=b.readVarInt();
            if(size<0||size>MAX_ITEMS)throw new IllegalArgumentException("Invalid known item count "+size);
            var values=new LinkedHashMap<Identifier,Float>(size);
            for(int i=0;i<size;i++)values.put(b.readIdentifier(),b.readFloat());
            return new VisKnowledgeSync(values);
        }
        public void encode(RegistryFriendlyByteBuf b,VisKnowledgeSync p) {
            b.writeVarInt(p.values.size());
            p.values.forEach((item,vis)->{b.writeIdentifier(item);b.writeFloat(vis);});
        }
    };
    public Type<VisKnowledgeSync> type(){return TYPE;}

    public static void sendTo(ServerPlayer player) {
        if(player.connection==null)return;
        var values=new LinkedHashMap<Identifier,Float>();
        // Items from removed mods stay saved but are not sent.
        for(Identifier id:VisKnowledge.get(player.level().getServer()).known(player.getUUID()))
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(item->values.put(id,ThaumcraftApi.server().vis(new ItemStack(item))));
        try{sender.accept(player,new VisKnowledgeSync(values));}
        catch(RuntimeException e){AltsPatches.LOG.warn("Skipping vis knowledge sync for {}",player.getGameProfile().name(),e);}
    }

    public static String format(float vis) {
        return new DecimalFormat("0.##",DecimalFormatSymbols.getInstance(Locale.ROOT)).format(vis);
    }
}
