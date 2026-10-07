package dev.thaumcraft.network;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.gameplay.ClientResearch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Server-owned research list mirrored to clients for JEI filtering. */
public record ResearchSync(List<Identifier> known) implements CustomPacketPayload {
    public static final Type<ResearchSync> TYPE = new Type<>(Thaumcraft.id("research_sync_v2"));
    private static final int MAX_PROJECTS=16384;
    public ResearchSync {known=List.copyOf(known);if(known.size()>MAX_PROJECTS)throw new IllegalArgumentException("Too many research IDs");}
    public static BiConsumer<ServerPlayer, ResearchSync> sender = (player, payload) -> {};
    public static Consumer<ResearchSync> receiver = payload -> {};

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchSync> CODEC = new StreamCodec<>() {
        public ResearchSync decode(RegistryFriendlyByteBuf b) {
            int size = b.readVarInt();
            if(size<0||size>MAX_PROJECTS)throw new IllegalArgumentException("Invalid research ID count "+size);
            var known = new ArrayList<Identifier>(size);
            for (int i = 0; i < size; i++) known.add(b.readIdentifier());
            return new ResearchSync(List.copyOf(known));
        }

        public void encode(RegistryFriendlyByteBuf b, ResearchSync p) {
            b.writeVarInt(p.known.size());
            for (Identifier project : p.known) b.writeIdentifier(project);
        }
    };

    public Type<ResearchSync> type() {
        return TYPE;
    }

    public static void sendTo(ServerPlayer player) {
        if (player.connection == null) return;
        try {
            sender.accept(player, new ResearchSync(ArcaneWorldData.researchData(player.level()).known(player.getUUID())));
        } catch (Exception e) {
            Thaumcraft.LOG.warn("Skipping research sync for {}", player.getGameProfile().name(), e);
        }
    }

    public static void handleClient(ResearchSync payload) {
        ClientResearch.update(payload.known);
    }
}
