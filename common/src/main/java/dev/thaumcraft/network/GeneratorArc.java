package dev.thaumcraft.network;
import dev.thaumcraft.Thaumcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
public record GeneratorArc(BlockPos source,BlockPos target,int amount) implements CustomPacketPayload {
    public static final Type<GeneratorArc> TYPE=new Type<>(Thaumcraft.id("generator_arc"));
    public static BiConsumer<ServerPlayer,GeneratorArc> sender=(player,payload)->{};
    public static Consumer<GeneratorArc> receiver=payload->{};
    public static final StreamCodec<RegistryFriendlyByteBuf,GeneratorArc> CODEC=new StreamCodec<>() {
        public GeneratorArc decode(RegistryFriendlyByteBuf b){return new GeneratorArc(b.readBlockPos(),b.readBlockPos(),b.readVarInt());}
        public void encode(RegistryFriendlyByteBuf b,GeneratorArc p){b.writeBlockPos(p.source);b.writeBlockPos(p.target);b.writeVarInt(p.amount);}
    };
    public Type<GeneratorArc> type(){return TYPE;}
}
