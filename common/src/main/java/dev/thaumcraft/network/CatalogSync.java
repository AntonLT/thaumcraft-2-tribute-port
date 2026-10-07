package dev.thaumcraft.network;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.gameplay.AddonData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.BiConsumer;

/** Bounded full catalog, sent before knowledge on join and after a successful reload. */
public record CatalogSync(String json) implements CustomPacketPayload {
    private static final int MAX_BYTES=900000;
    public static final Type<CatalogSync> TYPE=new Type<>(Thaumcraft.id("addon_catalog_v1"));
    public static BiConsumer<ServerPlayer,CatalogSync> sender=(player,payload)->{};
    public CatalogSync {validate(json);}
    public static void validate(String json){if(json.length()>MAX_BYTES/3||json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>MAX_BYTES)throw new IllegalArgumentException("Addon catalog exceeds 900000 UTF-8 bytes");}
    public static final StreamCodec<RegistryFriendlyByteBuf,CatalogSync> CODEC=new StreamCodec<>(){
        public CatalogSync decode(RegistryFriendlyByteBuf buffer){return new CatalogSync(buffer.readUtf(MAX_BYTES/3));}
        public void encode(RegistryFriendlyByteBuf buffer,CatalogSync payload){buffer.writeUtf(payload.json,MAX_BYTES/3);}
    };
    public Type<CatalogSync> type(){return TYPE;}
    public static void send(ServerPlayer player,String json){if(player.connection!=null)sender.accept(player,new CatalogSync(json));}
    public static void handle(CatalogSync payload){AddonData.receive(payload.json);}
}
