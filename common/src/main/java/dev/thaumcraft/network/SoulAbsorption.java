package dev.thaumcraft.network;

import dev.thaumcraft.Thaumcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** One drained creature, including its position before it is removed. */
public record SoulAbsorption(BlockPos target,double x,double y,double z,float height) implements CustomPacketPayload {
    public static final Type<SoulAbsorption> TYPE=new Type<>(Thaumcraft.id("soul_absorption"));
    public static BiConsumer<ServerPlayer,SoulAbsorption> sender=(player,payload)->{};
    public static Consumer<SoulAbsorption> receiver=payload->{};
    public static final StreamCodec<RegistryFriendlyByteBuf,SoulAbsorption> CODEC=new StreamCodec<>() {
        public SoulAbsorption decode(RegistryFriendlyByteBuf b){return new SoulAbsorption(b.readBlockPos(),b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat());}
        public void encode(RegistryFriendlyByteBuf b,SoulAbsorption p){b.writeBlockPos(p.target);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeFloat(p.height);}
    };
    public Type<SoulAbsorption> type(){return TYPE;}
}
