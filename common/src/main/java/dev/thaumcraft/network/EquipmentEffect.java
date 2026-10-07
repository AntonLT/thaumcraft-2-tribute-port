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

/** Successful equipment actions send their original particles to nearby observers. */
public record EquipmentEffect(int kind,Vec3 origin) implements CustomPacketPayload {
    public static final int WISP=0,TILL=1,POOF=2,SMELT=3,TRADE=4,SOUL=5,MONOLITH=6,DUPLICATOR=7,VAMPIRIC=8,SOUL_MARK=9,BONE_MARK=10,VOID_POOF=11;
    public static final Type<EquipmentEffect> TYPE=new Type<>(Thaumcraft.id("equipment_effect"));
    public static BiConsumer<ServerPlayer,EquipmentEffect> sender=(player,effect)->{};
    public static Consumer<EquipmentEffect> receiver=effect->{};
    public static final StreamCodec<RegistryFriendlyByteBuf,EquipmentEffect> CODEC=new StreamCodec<>() {
        public EquipmentEffect decode(RegistryFriendlyByteBuf b){return new EquipmentEffect(b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()));}
        public void encode(RegistryFriendlyByteBuf b,EquipmentEffect p){b.writeVarInt(p.kind);b.writeDouble(p.origin.x);b.writeDouble(p.origin.y);b.writeDouble(p.origin.z);}
    };
    public Type<EquipmentEffect> type(){return TYPE;}
    public static void send(ServerLevel level,int kind,Vec3 origin){
        var effect=new EquipmentEffect(kind,origin);
        for(var player:level.players())if(player.distanceToSqr(origin)<64*64)sender.accept(player,effect);
    }
}
