package dev.thaumcraft.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.function.BiConsumer;

/** Installed by the client; common blocks never load client rendering classes on a server. */
public final class VisualEffects {
    public static java.util.function.Consumer<net.minecraft.world.entity.Entity> entity=entity->{};
    public static BiConsumer<Level,BlockPos> ambient=(level,pos)->{};
    private VisualEffects() {}
}
