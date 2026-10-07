package dev.thaumcraft.gameplay;

import java.util.UUID;

/** Internal bridge for the vanilla Crafter's persistent placing player. */
public interface CrafterResearchOwner {
    UUID thaumcraft$getOwner();
    void thaumcraft$setOwner(UUID owner);
}
