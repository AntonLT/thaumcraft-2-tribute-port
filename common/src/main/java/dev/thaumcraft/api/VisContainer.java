package dev.thaumcraft.api;

import net.minecraft.core.Direction;

/**
 * A block that joins the Thaumcraft vis network. Implement it on a block entity, or adapt another block entity with
 * {@link ThaumcraftVis#register}. Vis has two independent kinds, pure and tainted, selected by {@code tainted}.
 *
 * <p>Thaumcraft conduits carry vis toward the highest suction and only take vis; they never push it. A source reports
 * its stored vis and low suction, and conduits, pumps, and tanks with higher suction extract from it. A consumer
 * reports suction while it wants vis, so conduits carry vis to it, then draws with {@link ThaumcraftVis#pull}. Built-in
 * consumers use suction 50.
 *
 * <p>Thaumcraft calls these methods on the server thread. {@link #connects} is also called on the client to draw
 * conduit attachments.
 */
public interface VisContainer {
    /**
     * Whether a neighbor on this side may connect. Conduits recompute their attachments on neighbor shape updates, so
     * base this on the block state, or call {@code BlockState.updateNeighbourShapes} after it changes.
     */
    boolean connects(Direction side);
    /** Stored vis of one kind. */
    float vis(boolean tainted);
    float capacity();
    /** Pull toward this container, from 0. Values above 1,000 are treated as 1,000. */
    default int suction(boolean tainted){return 0;}
    /**
     * Removes and returns up to {@code amount}. Return exactly {@code min(amount, vis(tainted))} to act as a source,
     * or 0 to refuse extraction.
     */
    float extract(float amount,boolean tainted);
    /** Adds up to {@code amount} and returns the accepted amount, or 0 to refuse insertion. */
    float insert(float amount,boolean tainted);
}
