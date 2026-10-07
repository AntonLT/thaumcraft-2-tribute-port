package dev.thaumcraft.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.List;

/** Immutable view of one research project in a catalog snapshot. */
public interface Research {
    Identifier id();
    Identifier category();
    /** Translatable name, falling back to the literal definition text. */
    Component name();
    /** Translatable description, falling back to the literal definition text. */
    Component description();
    int difficulty();
    /** Successful Quaesitum steps needed to complete a theory. */
    int steps();
    /** Excluded from random selection; reachable through research source {@code special} lists or {@link ThaumcraftApi#unlock}. */
    boolean restricted();
    List<Identifier> prerequisites();
}
