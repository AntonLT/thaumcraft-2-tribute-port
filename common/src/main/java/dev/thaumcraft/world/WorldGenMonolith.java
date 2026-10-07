package dev.thaumcraft.world;

import dev.thaumcraft.content.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Original monolith footprint, weathered pillars and buried activation core. */
public final class WorldGenMonolith {
    private WorldGenMonolith(){}
    public static boolean generate(WorldGenLevel level,BlockPos center,RandomSource random){
        if(level.getLevel().dimension()!=net.minecraft.world.level.Level.OVERWORLD)return false;
        var cover=level.getBlockState(center);
        if(cover.is(Blocks.SNOW)||cover.is(Blocks.SHORT_GRASS)||cover.is(Blocks.TALL_GRASS)||cover.is(Blocks.FERN)||cover.is(Blocks.LARGE_FERN))center=center.below();
        if(center.getY()<20)return false;
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-3,-1,-3),center.offset(3,7,3)))if(!level.ensureCanWrite(pos))return false;
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-3,0,-3),center.offset(3,0,3))){
            var state=level.getBlockState(pos);
            if(!state.is(Blocks.DIRT)&&!state.is(Blocks.GRASS_BLOCK)&&!state.is(Blocks.STONE)&&!state.is(Blocks.SAND))return false;
        }
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-3,-1,-3),center.offset(3,7,3)))if(level.getBlockEntity(pos)!=null)return false;
        var index=EldritchIndex.get(level.getLevel());
        if(!index.reserve(center))return false;
        boolean generated=false;
        try {generated=build(level,center,random);return generated;}
        finally {if(!generated)index.cancel(center);}
    }
    private static boolean build(WorldGenLevel level,BlockPos center,RandomSource random){
        var biome=level.getBiome(center);
        boolean sandy=biome.is(net.minecraft.world.level.biome.Biomes.DESERT)||biome.is(BiomeTags.IS_BEACH);
        boolean swamp=biome.is(net.minecraft.world.level.biome.Biomes.SWAMP)||biome.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP);
        boolean vines=swamp||biome.is(BiomeTags.IS_JUNGLE)||biome.is(BiomeTags.IS_FOREST);
        for(int i=0;i<75;i++){
            BlockPos pos=center.offset(random.nextInt(4)-random.nextInt(4),0,random.nextInt(4)-random.nextInt(4));
            BlockState state=sandy?Blocks.SANDSTONE.defaultBlockState():swamp?Blocks.MOSSY_COBBLESTONE.defaultBlockState():switch(random.nextInt(3)){case 1->Blocks.MOSSY_STONE_BRICKS.defaultBlockState();case 2->Blocks.CRACKED_STONE_BRICKS.defaultBlockState();default->Blocks.STONE_BRICKS.defaultBlockState();};
            level.setBlock(pos,state,2);
        }
        for(int[] side:new int[][]{{2,2},{2,-2},{-2,-2},{-2,2}}){
            for(int height=0;height<random.nextInt(5);height++){
                BlockPos pos=center.offset(side[0],height+1,side[1]);
                BlockState material=sandy?switch(height){case 1,3->Blocks.CHISELED_SANDSTONE.defaultBlockState();case 2,4->Blocks.CUT_SANDSTONE.defaultBlockState();default->Blocks.SANDSTONE.defaultBlockState();}:swamp&&height<3?Blocks.MOSSY_COBBLESTONE.defaultBlockState():Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
                level.setBlock(pos,material,2);
                if(!sandy&&height>1&&vines)for(Direction direction:Direction.Plane.HORIZONTAL)if(random.nextBoolean())level.setBlock(pos.relative(direction),Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(direction.getOpposite()),true),2);
                if(!sandy&&height==3){
                    BlockState cap=swamp?Blocks.MOSSY_COBBLESTONE.defaultBlockState():Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
                    if(random.nextBoolean())level.setBlock(pos.offset(-side[0]/2,0,0),cap,2);
                    if(random.nextBoolean())level.setBlock(pos.offset(0,0,-side[1]/2),cap,2);
                }
            }
        }
        for(int height=3;height<=7;height++)level.setBlock(center.above(height),Content.block("eldritch_monolith").defaultBlockState(),2);
        for(int x=-1;x<=1;x++)for(int y=-1;y<=0;y++)for(int z=-1;z<=1;z++)
            level.setBlock(center.offset(x,y,z),x==0&&z==0&&y==0?Blocks.AIR.defaultBlockState():Content.block("eldritch_structure_stone").defaultBlockState(),2);
        level.setBlock(center,Content.block("eldritch_core").defaultBlockState(),2);
        for(Direction side:MonolithBlockEntity.SIDES)level.setBlock(center.relative(side),Content.block("eldritch_receptacle").defaultBlockState(),2);
        if(level.getBlockEntity(center) instanceof MonolithBlockEntity core)core.initialize(random);
        level.scheduleTick(center.above(3),Content.block("eldritch_monolith"),1);
        var server=level.getLevel();
        server.getServer().execute(()->{
            EldritchIndex.get(server).record(center);
            dev.thaumcraft.gameplay.ArcaneWorldData.get(server).changeAura(server,center,(int)(dev.thaumcraft.PortConfig.auraMax*.0666f),(int)(dev.thaumcraft.PortConfig.auraMax*.0333f));
        });
        return true;
    }
}
