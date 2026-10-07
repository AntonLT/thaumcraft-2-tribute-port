package dev.thaumcraft.world;

import com.mojang.serialization.Codec;
import dev.thaumcraft.PortConfig;
import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Optional;

/** Original scattered deposits, with an optional cosmetic variant for deepslate hosts. */
public final class CinnabarFeature extends Feature<CinnabarFeature.Configuration> {
    public record Configuration(Optional<BlockState> deepslateState) implements FeatureConfiguration {
        public static final Codec<Configuration> CODEC=BlockState.CODEC.optionalFieldOf("deepslate_state")
                .xmap(Configuration::new,Configuration::deepslateState).codec();
    }

    public CinnabarFeature(){super(Configuration.CODEC);}

    @Override public boolean place(FeaturePlaceContext<Configuration> context){
        if(!PortConfig.worldGeneration)return false;
        var level=context.level();var random=context.random();var origin=context.origin();boolean placed=false;
        ArcaneFeatures.seedAura(level,origin);
        int top=Math.min(51,level.getMaxY());
        int attempts=ArcaneFeatures.depositAttempts(15,51,level.getMinY(),top,random);
        var regular=Content.block("cinnabar_ore").defaultBlockState();
        for(int i=0;i<attempts;i++){
            BlockPos pos=new BlockPos(origin.getX()+random.nextInt(16),level.getMinY()+random.nextInt(top-level.getMinY()),origin.getZ()+random.nextInt(16));
            if(!level.ensureCanWrite(pos))continue;
            var host=level.getBlockState(pos);
            if(!host.is(BlockTags.STONE_ORE_REPLACEABLES)&&!host.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES))continue;
            var ore=host.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)?context.config().deepslateState().orElse(regular):regular;
            level.setBlock(pos,ore,2);placed=true;
        }
        return placed;
    }
}
