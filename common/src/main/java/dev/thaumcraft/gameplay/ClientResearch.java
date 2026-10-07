package dev.thaumcraft.gameplay;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** Client mirror of the player's known research. Updated by ResearchSync; JEI refreshes from it. */
public final class ClientResearch {
    private static final Set<Identifier> KNOWN = new HashSet<>();
    public static Runnable onChange = () -> {};

    private ClientResearch() {}

    public static void update(Collection<Identifier> known) {
        Set<Identifier> snapshot;
        synchronized (ClientResearch.class) {
            KNOWN.clear();
            KNOWN.addAll(known);
            snapshot = Set.copyOf(KNOWN);
        }
        onChange.run();
        dev.thaumcraft.api.ThaumcraftEvents.CLIENT_RESEARCH_CHANGED.post(listener -> listener.onChanged(snapshot));
    }

    public static synchronized boolean knows(int project) {
        return project < 0 || KNOWN.contains(GameData.researchId(project));
    }

    public static synchronized Set<Identifier> known() {
        return Set.copyOf(KNOWN);
    }
}
