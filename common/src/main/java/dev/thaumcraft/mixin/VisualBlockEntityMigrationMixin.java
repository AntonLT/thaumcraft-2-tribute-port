package dev.thaumcraft.mixin;

import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Old saves predate the visual-only block entities. Populate only those missing records. */
@Mixin(LevelChunk.class)
public class VisualBlockEntityMigrationMixin {
    @Inject(method="registerAllBlockEntitiesAfterLevelLoad",at=@At("TAIL"))
    private void thaumcraft$restoreVisualEntities(CallbackInfo callback){
        var chunk=(LevelChunk)(Object)this;var blocks=Content.visualBlocks();
        for(int sectionIndex=0;sectionIndex<chunk.getSections().length;sectionIndex++){
            var section=chunk.getSections()[sectionIndex];if(section.hasOnlyAir()||!section.getStates().maybeHas(state->blocks.contains(state.getBlock())))continue;
            int baseY=chunk.getSectionYFromSectionIndex(sectionIndex)*16;
            for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++)if(blocks.contains(section.getBlockState(x,y,z).getBlock())){
                var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,baseY+y,chunk.getPos().getMinBlockZ()+z);
                var entity=chunk.getBlockEntity(pos,LevelChunk.EntityCreationType.IMMEDIATE);
                if(chunk.getBlockState(pos).is(Content.block("eldritch_core"))){
                    if(entity instanceof dev.thaumcraft.world.VisualBlockEntity){
                        chunk.removeBlockEntity(pos);
                        chunk.addAndRegisterBlockEntity(new dev.thaumcraft.world.MonolithBlockEntity(pos,chunk.getBlockState(pos)));
                        chunk.markUnsaved();
                    }
                    // The level's tick container is registered after this load callback.
                    if(!chunk.getLevel().isClientSide())chunk.getBlockTicks().schedule(new net.minecraft.world.ticks.ScheduledTick<>(Content.block("eldritch_core"),pos,chunk.getLevel().getGameTime()+1,chunk.getLevel().nextSubTickCount()));
                }
            }
        }
    }
}
