package dev.thaumcraft.content;

import com.mojang.serialization.MapCodec;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class ArcaneLeavesBlock extends TintedParticleLeavesBlock {
    public static final MapCodec<ArcaneLeavesBlock> CODEC=simpleCodec(ArcaneLeavesBlock::new);
    public ArcaneLeavesBlock(Properties properties) {super(0.01f,properties);}
    @Override public void animateTick(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,RandomSource random){VisualEffects.ambient.accept(level,pos);}
    @Override public MapCodec<? extends TintedParticleLeavesBlock> codec() {return CODEC;}
    @Override protected boolean decaying(BlockState state){return !state.getValue(PERSISTENT)&&state.getValue(DISTANCE)>4;}
    @Override protected boolean isRandomlyTicking(BlockState state){return true;}
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){
        var tool=params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL);
        if(tool!=null&&tool.is(net.minecraft.world.item.Items.SHEARS))return java.util.List.of(new net.minecraft.world.item.ItemStack(this));
        var random=params.getLevel().getRandom();String drop=null;
        if(state.is(Content.block("greatwood_leaves"))&&random.nextInt(50)==0)drop="greatwood_sapling";
        else if(state.is(Content.block("silverwood_leaves"))&&random.nextInt(30)==0)drop="quicksilver";
        else if(state.is(Content.block("tainted_leaves"))&&random.nextInt(100)==0)drop=random.nextBoolean()?"tainted_fruit":"tainted_branch";
        return drop==null?java.util.List.of():java.util.List.of(new net.minecraft.world.item.ItemStack(Content.item(drop)));
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        var data=ArcaneWorldData.get(level);
        if(state.is(Content.block("silverwood_leaves")))data.drainVibes(level,pos,1,true);
        else if(state.is(Content.block("tainted_leaves"))) {
            var aura=data.aura(level,pos);
            if(TaintBlock.shouldHeal(aura,random)){TaintBlock.purify(level,pos);return;}
            TaintBlock.spreadFrom(level,pos,aura,random);
        }
        super.randomTick(state,level,pos,random);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moving) {
        super.affectNeighborsAfterRemoval(state,level,pos,moving);
        if(state.is(Content.block("tainted_leaves")))dev.thaumcraft.world.TaintMemory.get(level).forget(pos);
    }
}
