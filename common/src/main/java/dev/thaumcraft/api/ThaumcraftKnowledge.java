package dev.thaumcraft.api;

import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.gameplay.ResearchBook;
import dev.thaumcraft.network.ResearchSync;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.Objects;
import java.util.UUID;

/** Optional team ownership. Register once during common initialization; resolve offline UUIDs too. */
public final class ThaumcraftKnowledge {
    private ThaumcraftKnowledge() {}
    @FunctionalInterface public interface GroupResolver {
        /** A stable, namespaced group ID, or null for personal knowledge. Called on the server thread. */
        Identifier group(MinecraftServer server,UUID player);
    }
    private static GroupResolver resolver=(server,player)->null;
    /** Installs the team's resolver. Call synchronize after membership changes. */
    public static GroupResolver setGroupResolver(GroupResolver value){
        GroupResolver previous=resolver;resolver=Objects.requireNonNull(value);return previous;
    }
    /** The current group, or null when this UUID uses personal knowledge. */
    public static Identifier group(MinecraftServer server,UUID player){
        if(!server.isSameThread())throw new IllegalStateException("Thaumcraft knowledge requires the server thread");
        return resolver.group(server,player);
    }
    /** Refreshes clients and recipe books after the addon changes team membership. */
    public static void synchronize(MinecraftServer server){
        if(!server.isSameThread())throw new IllegalStateException("Thaumcraft knowledge requires the server thread");
        for(var player:server.getPlayerList().getPlayers())synchronize(player);
    }
    /** Refreshes one player, also called by both loaders on login. */
    public static void synchronize(ServerPlayer player){
        if(!player.level().getServer().isSameThread())throw new IllegalStateException("Thaumcraft knowledge requires the server thread");
        var data=ArcaneWorldData.researchData(player.level());
        ResearchSync.sendTo(player);
        for(var project:dev.thaumcraft.gameplay.AddonData.server().projects()){
            if(data.knows(player.getUUID(),project.key()))ResearchBook.awardRecipes(player,project.index());
            else ResearchBook.resetRecipes(player,project.index());
        }
    }
}
