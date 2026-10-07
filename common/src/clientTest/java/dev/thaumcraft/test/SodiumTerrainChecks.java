package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Exercise Sodium's actual default-model emitter, including the optional terrain mixin. */
final class SodiumTerrainChecks {
    static void verify(){
        try{Class.forName("net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext");}
        catch(ClassNotFoundException e){return;}
        Probe.verify();
    }
    private static final class Probe extends net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext {
        private final ArrayList<net.minecraft.client.renderer.texture.TextureAtlasSprite> candidates=new ArrayList<>();
        Probe(){
            var mc=Minecraft.getInstance();level=mc.level;slice=new net.caffeinemc.mods.sodium.client.world.LevelSlice(mc.level);
            for(int tile:new int[]{203,204,205,206,207,233,234,235,236})candidates.add(dev.thaumcraft.client.legacy.LegacyTerrain.sprite(tile));
            for(int tile:new int[]{208,209})candidates.add(mc.getAtlasManager().get(new net.minecraft.client.resources.model.sprite.SpriteId(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS,Thaumcraft.id("block/atlas_"+tile+"_greatwood"))));
        }
        @Override protected void processQuad(net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl quad) {}
        Set<String> sprites(BlockState block,BlockPos position){
            var mc=Minecraft.getInstance();
            state=block;pos=position;random=RandomSource.create(216);
            var parts=new ArrayList<BlockStateModelPart>();mc.getModelManager().getBlockStateModelSet().get(block).collectParts(random,parts);
            var sprites=new HashSet<String>();
            for(var part:parts)bufferDefaultModel(part,face->false,quad->{
                int tint=dev.thaumcraft.client.legacy.LegacyTerrain.tint(block);
                if(tint!=-1)for(int i=0;i<4;i++)if(quad.baseColor(i)!=tint)
                    throw new AssertionError("Sodium must retain the Greatwood ARGB vertex tint");
                float u=0,v=0;for(int i=0;i<4;i++){u+=quad.getTexU(i)/4;v+=quad.getTexV(i)/4;}
                boolean found=false;
                for(var sprite:candidates)if(u>sprite.getU0()&&u<sprite.getU1()&&v>sprite.getV0()&&v<sprite.getV1()){
                    sprites.add(sprite.contents().name().getPath());found=true;break;
                }
                if(!found)throw new AssertionError("Sodium terrain emitted unexpected atlas UVs: "+u+", "+v);
            });
            if(sprites.isEmpty())throw new AssertionError("Sodium terrain emitted no quads");
            return sprites;
        }
        static void verify(){
            var probe=new Probe();var taint=new HashSet<String>();var logs=new HashSet<String>();
            for(int y=120;y<144;y++){
                taint.addAll(probe.sprites(Content.block("tainted_grass").defaultBlockState(),new BlockPos(1,y,31)));
                logs.addAll(probe.sprites(Content.block("tainted_log").defaultBlockState(),new BlockPos(1,y,31)));
            }
            if(taint.size()<2||!logs.containsAll(Set.of("block/atlas_233","block/atlas_234","block/atlas_235")))
                throw new AssertionError("Sodium must retain taint and log texture variation: "+taint+" / "+logs);
            var option=Minecraft.getInstance().options.cutoutLeaves();boolean old=option.get();
            try{
                for(boolean fancy:new boolean[]{true,false}){
                    option.set(fancy);
                    String expected="block/atlas_"+(fancy?208:209)+"_greatwood";
                    if(!probe.sprites(Content.block("greatwood_leaves").defaultBlockState(),BlockPos.ZERO).equals(Set.of(expected)))
                        throw new AssertionError("Sodium must switch the leaf sprite with graphics settings");
                }
            }finally{option.set(old);}
            Thaumcraft.LOG.info("THAUMCRAFT_SODIUM_TERRAIN_PASS taint_log_leaf_variation_tint");
        }
    }
}
