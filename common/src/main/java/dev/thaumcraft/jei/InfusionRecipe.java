package dev.thaumcraft.jei;

import dev.thaumcraft.gameplay.GameData;

/** JEI view of one infusion recipe. Instances are stable so JEI can hide/unhide them. */
public record InfusionRecipe(GameData.Infusion infusion) {
}
