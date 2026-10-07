package dev.thaumcraft.world;

import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Visuals whose complete authoritative data already lives in the blockstate. */
public final class VisualBlockEntity extends BlockEntity {
    public VisualBlockEntity(BlockPos pos,BlockState state){super(Content.VISUAL_ENTITY,pos,state);}
}
