package dev.thaumcraft.network;

import dev.thaumcraft.Thaumcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Original bore pulses and item wisps, emitted by the server mining cycle. */
public record BoreEffect(int kind,int focus,Vec3 origin,Vec3 target) implements CustomPacketPayload {
    public static final int PULSE=0,TRAIL=1,COLLECT=2;
    public static final Type<BoreEffect> TYPE=new Type<>(Thaumcraft.id("bore_effect"));
    public static BiConsumer<ServerPlayer,BoreEffect> sender=(player,effect)->{};
    public static Consumer<BoreEffect> receiver=effect->{};
    public static final StreamCodec<RegistryFriendlyByteBuf,BoreEffect> CODEC=new StreamCodec<>() {
        public BoreEffect decode(RegistryFriendlyByteBuf b){return new BoreEffect(b.readVarInt(),b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()));}
        public void encode(RegistryFriendlyByteBuf b,BoreEffect p){b.writeVarInt(p.kind);b.writeVarInt(p.focus);b.writeDouble(p.origin.x);b.writeDouble(p.origin.y);b.writeDouble(p.origin.z);b.writeDouble(p.target.x);b.writeDouble(p.target.y);b.writeDouble(p.target.z);}
    };
    public Type<BoreEffect> type(){return TYPE;}
    public static void send(ServerLevel level,int kind,int focus,Vec3 origin,Vec3 target){
        var effect=new BoreEffect(kind,focus,origin,target);
        for(var player:level.players())if(player.distanceToSqr(origin)<64*64||player.distanceToSqr(target)<64*64)sender.accept(player,effect);
    }
}
