package dev.thaumcraft.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Immutable view of one research book category. */
public interface ResearchCategory {
    Identifier id();
    Component name();
    /** Sort key among addon categories; the four original categories always come first. */
    int order();
}
