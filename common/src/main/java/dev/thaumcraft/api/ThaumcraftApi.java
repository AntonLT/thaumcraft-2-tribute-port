package dev.thaumcraft.api;

import dev.thaumcraft.gameplay.*;
import dev.thaumcraft.network.ResearchSync;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Common addon entry point on Fabric and NeoForge. Catalog reads go through {@link #server()} or {@link #client()}
 * and are safe from any thread. Methods taking a {@link MinecraftServer} read or change saved research and must run
 * on the server thread.
 */
public final class ThaumcraftApi {
    private static final int API_VERSION=1;
    private static final ThaumcraftCatalog SERVER=new CatalogView(false),CLIENT=new CatalogView(true);
    private static volatile int generatorEnergyMultiplier=1;
    private ThaumcraftApi() {}

    /** Runtime API version. Compare against the version you compiled for; this is not a compile-time constant. */
    public static int apiVersion(){return API_VERSION;}
    /** Scale generator FE production, storage and output. Call from common addon initialization on both sides. */
    public static void setGeneratorEnergyMultiplier(int multiplier){
        if(multiplier<1||multiplier>Integer.MAX_VALUE/40000)throw new IllegalArgumentException("Generator energy multiplier is out of range");
        generatorEnergyMultiplier=multiplier;
    }
    public static int generatorEnergyMultiplier(){return generatorEnergyMultiplier;}
    /** Authoritative definitions of the running server, including an integrated server. */
    public static ThaumcraftCatalog server(){return SERVER;}
    /** Definitions the client last received. Built-in definitions only until a server sends its catalog. */
    public static ThaumcraftCatalog client(){return CLIENT;}

    private static void serverThread(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Thaumcraft world API requires the server thread");}
    private static ArcaneWorldData data(MinecraftServer server){serverThread(server);return ArcaneWorldData.researchData(server.overworld());}

    /** Saved knowledge, including IDs of projects whose addon is currently absent. */
    public static List<Identifier> known(MinecraftServer server,UUID player){return data(server).known(player);}
    public static boolean knows(MinecraftServer server,UUID player,Identifier research){return data(server).knows(player,research);}
    /**
     * Grants a project in the server catalog, persists it, synchronizes online knowledge owners and awards its crafting
     * recipes. Prerequisites are not checked. Returns false if already known.
     * @throws IllegalArgumentException if the project is not in the server catalog
     */
    public static boolean unlock(MinecraftServer server,UUID player,Identifier research){
        var data=data(server);
        var project=AddonData.catalog(false).projects().get(research);
        if(project==null)throw new IllegalArgumentException("Unknown research "+research);
        if(!data.unlock(player,research))return false;
        for(ServerPlayer online:server.getPlayerList().getPlayers())if(data.sameKnowledgeOwner(player,online.getUUID())){
            ResearchSync.sendTo(online);ResearchBook.awardRecipes(online,project.index());
            ProgressTriggers.RESEARCH_LEARNED.trigger(online,research);
        }
        ThaumcraftEvents.RESEARCH_UNLOCKED.post(listener->listener.onChanged(server,player,research));
        return true;
    }
    /** Removes a project, including a dormant one, and its recipe book entries. Returns false if it was not known. */
    public static boolean revoke(MinecraftServer server,UUID player,Identifier research){
        var data=data(server);
        if(!data.revoke(player,research))return false;
        for(ServerPlayer online:server.getPlayerList().getPlayers())if(data.sameKnowledgeOwner(player,online.getUUID())){
            ResearchSync.sendTo(online);
            var project=AddonData.catalog(false).projects().get(research);
            if(project!=null)ResearchBook.resetRecipes(online,project.index());
        }
        ThaumcraftEvents.RESEARCH_REVOKED.post(listener->listener.onChanged(server,player,research));
        return true;
    }
    /**
     * Whether an owner may produce a crafting or infusion recipe. Ownerless automation (null owner) may only produce
     * unlocked recipes. This checks authorization only, not recipe existence or input matching.
     */
    public static boolean canCraft(MinecraftServer server,UUID owner,Identifier recipe){
        serverThread(server);return !ResearchGate.locked(server.overworld(),owner,recipe);
    }
    public static boolean canCraft(ServerPlayer player,Identifier recipe){return canCraft(player.level().getServer(),player.getUUID(),recipe);}

    /** Known research the client last received for the local player. */
    public static Set<Identifier> clientKnown(){return ClientResearch.known();}
    public static boolean clientKnows(Identifier research){return ClientResearch.known().contains(research);}

    /**
     * Java vis override, above data pack and configured values. Call from common initialization so client and server
     * agree. Unknown item IDs are accepted, because other mods may register later, and are reported on catalog load.
     */
    public static void registerVis(Identifier item,float value){GameData.registerVis(item,value);}
    /** Returns false if no Java override was registered for the item. */
    public static boolean unregisterVis(Identifier item){return GameData.unregisterVis(item);}
}
