package dev.thaumcraft.network;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.world.EldritchIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import java.util.Optional;
import java.util.function.Consumer;

/** A texture-clock lookup; the server owns the registry, player position and tile validation. */
public record VoidCompassTarget(long requestId,ResourceKey<Level> dimension,Optional<BlockPos> target) implements CustomPacketPayload {
    public static final Type<VoidCompassTarget> TYPE=new Type<>(Thaumcraft.id("void_compass_target"));
    public static Consumer<Request> requestSender=request->{};
    public static Consumer<VoidCompassTarget> receiver=target->{};
    private static final java.util.Map<ServerPlayer,Integer> LAST_REQUEST=new java.util.WeakHashMap<>();
    public static final StreamCodec<RegistryFriendlyByteBuf,VoidCompassTarget> CODEC=new StreamCodec<>(){
        public VoidCompassTarget decode(RegistryFriendlyByteBuf b){return new VoidCompassTarget(b.readLong(),b.readResourceKey(Registries.DIMENSION),b.readOptional(RegistryFriendlyByteBuf::readBlockPos));}
        public void encode(RegistryFriendlyByteBuf b,VoidCompassTarget p){b.writeLong(p.requestId);b.writeResourceKey(p.dimension);b.writeOptional(p.target,RegistryFriendlyByteBuf::writeBlockPos);}
    };
    public Type<VoidCompassTarget> type(){return TYPE;}
    public record Request(long requestId) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(Thaumcraft.id("void_compass_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=new StreamCodec<>(){
            public Request decode(RegistryFriendlyByteBuf b){return new Request(b.readLong());}
            public void encode(RegistryFriendlyByteBuf b,Request p){b.writeLong(p.requestId);}
        };
        public Type<Request> type(){return TYPE;}
    }
    public static void handle(ServerPlayer player,Request request,Consumer<VoidCompassTarget> reply){
        int now=player.level().getServer().getTickCount();Integer previous=LAST_REQUEST.get(player);
        if(previous!=null&&now-previous<20)return;
        LAST_REQUEST.put(player,now);
        if(player.inventoryMenu.slots.stream().noneMatch(slot->slot.getItem().is(Content.item("void_compass"))))return;
        reply.accept(new VoidCompassTarget(request.requestId,player.level().dimension(),EldritchIndex.get(player.level()).compassTarget(player.level(),player.position())));
    }
}
