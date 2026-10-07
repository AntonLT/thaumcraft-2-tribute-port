package dev.thaumcraft;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Thaumcraft {
    public static final String MOD_ID = "thaumcraft2tp";
    public static final Logger LOG = LoggerFactory.getLogger("Thaumcraft 2 Tribute Port");

    public static net.minecraft.server.level.TicketType ANCHOR_TICKET;
    /** Loads, but never simulates, a portal destination chunk until its view snapshot is sent. Not saved. */
    public static net.minecraft.server.level.TicketType PORTAL_VIEW_TICKET;
    public static net.minecraft.server.level.TicketType createPortalViewTicket(){return new net.minecraft.server.level.TicketType(2,net.minecraft.server.level.TicketType.FLAG_LOADING);}
    /** Set by each loader. Addon definitions use it for {@code required_mods}. */
    public static java.util.function.Predicate<String> modLoaded=id->id.equals("minecraft")||id.equals(MOD_ID);

    private Thaumcraft() {}
    private static final java.util.Map<net.minecraft.server.level.ServerLevel,Long> NEXT_DATA_SAVE=new java.util.WeakHashMap<>();
    /** Original configurable wall-clock autosave, using modern saved-data serialization. */
    public static void tickData(net.minecraft.server.level.ServerLevel level){
        if(PortConfig.autosaveMinutes==0){NEXT_DATA_SAVE.remove(level);return;}
        long now=System.currentTimeMillis(),delay=PortConfig.autosaveMinutes*60000L;
        long next=NEXT_DATA_SAVE.computeIfAbsent(level,ignored->now+delay);
        if(now>=next){NEXT_DATA_SAVE.put(level,now+delay);level.getDataStorage().scheduleSave().exceptionally(error->{LOG.error("Cannot autosave Thaumcraft world data",error);return null;});}
    }

    public static void serverStarted(net.minecraft.server.MinecraftServer server) {
        dev.thaumcraft.gameplay.GameData.bindRecipes(server.getRecipeManager());
        dev.thaumcraft.gameplay.AddonData.reload(server,true);
        dev.thaumcraft.gameplay.GameData.validate();
        if(Boolean.getBoolean("thaumcraft.smokeTest")) {
            try {Class.forName("dev.thaumcraft.test.ServerSmokeTests").getMethod("run",net.minecraft.server.MinecraftServer.class).invoke(null,server);}
            catch(ReflectiveOperationException e) {throw new IllegalStateException("Thaumcraft server smoke test failed",e);}
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
