package dev.thaumcraft.gameplay;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;

/** Single owner for research-gated recipe checks. Server data is authoritative; clients mirror it. */
public final class ResearchGate {
    private ResearchGate() {}

    public static int projectForRecipe(Identifier recipe) {
        return AddonData.requirement(recipe);
    }

    public static boolean locked(ServerPlayer player, Identifier recipe) {
        int project = projectForRecipe(recipe);
        if (project < 0) return project == AddonData.UNAVAILABLE;
        return !ArcaneWorldData.knows(player, project);
    }

    public static boolean locked(ServerLevel level, UUID owner, Identifier recipe) {
        int project = projectForRecipe(recipe);
        if (project < 0) return project == AddonData.UNAVAILABLE;
        var data = ArcaneWorldData.researchData(level);
        return owner == null || !data.knows(owner, project);
    }

    public static boolean locked(Set<Identifier> known, Identifier recipe) {
        int project = projectForRecipe(recipe);
        if (project < 0) return project == AddonData.UNAVAILABLE;
        return !known.contains(GameData.researchId(project));
    }

    public static boolean infusionLocked(Set<Identifier> known, GameData.Infusion recipe) {
        return recipe.requiredResearch()!=null && !known.contains(recipe.requiredResearch());
    }

    public static String researchName(int project) {
        return project < 0 ? "" : GameData.findProject(GameData.researchId(project)).map(GameData.Project::name).orElse("Unavailable");
    }

    public static String discoveryId(int project) {
        return project < 0 ? "" : GameData.findProject(GameData.researchId(project)).map(GameData.Project::discovery).orElse("thaumcraft2tp:discovery_generic");
    }
}
