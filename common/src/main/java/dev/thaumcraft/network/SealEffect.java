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

/** Original seal particles, emitted only after the server performs the corresponding action. */
public record SealEffect(int kind,Vec3 origin,Vec3 target) implements CustomPacketPayload {
    public static final int BOOST=0,DETECT=1,ANCHOR=2,NULLIFY=3,HEAL=4;
    public static final int HYDRATE=5,GROW=6,REPLANT=7,HARVEST=8,TILL=9,PICKUP=10,POOF=11,SEED=12;
    public static final Type<SealEffect> TYPE=new Type<>(Thaumcraft.id("seal_effect"));
    public static BiConsumer<ServerPlayer,SealEffect> sender=(player,effect)->{};
    public static Consumer<SealEffect> receiver=effect->{};
    public static final StreamCodec<RegistryFriendlyByteBuf,SealEffect> CODEC=new StreamCodec<>() {
        public SealEffect decode(RegistryFriendlyByteBuf b){return new SealEffect(b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()));}
        public void encode(RegistryFriendlyByteBuf b,SealEffect p){b.writeVarInt(p.kind);b.writeDouble(p.origin.x);b.writeDouble(p.origin.y);b.writeDouble(p.origin.z);b.writeDouble(p.target.x);b.writeDouble(p.target.y);b.writeDouble(p.target.z);}
    };
    public Type<SealEffect> type(){return TYPE;}
    public static void send(ServerLevel level,int kind,Vec3 origin,Vec3 target){
        var effect=new SealEffect(kind,origin,target);
        for(var player:level.players())if(player.distanceToSqr(origin)<64*64||player.distanceToSqr(target)<64*64)sender.accept(player,effect);
    }
}
