package dev.thaumcraft.client.legacy;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Random;

/** Per-face textures retain the original coordinate formulas when chunks are meshed. */
public final class LegacyTerrain {
    private LegacyTerrain() {}
    public static int tint(BlockState state){return state.is(Content.block("greatwood_leaves"))?0xff48b518:-1;}
    public static TextureAtlasSprite sprite(int tile){return Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS,Thaumcraft.id("block/atlas_"+tile)));}
    public static BakedQuad texture(BakedQuad quad,BlockState state,BlockPos pos){
        var key=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if(!key.getNamespace().equals(Thaumcraft.MOD_ID))return quad;
        var entry=Content.DEFINITIONS.get(key.getPath());if(entry==null)return quad;int tile=-1,side=quad.direction().get3DDataValue();
        if(entry.legacy().endsWith("blockTaint")){
            if(entry.meta()<5||entry.meta()==15){var random=new Random(side+pos.getY()+pos.getX()*pos.getZ());tile=random.nextInt(100)<75?204:204+random.nextInt(4);}else tile=entry.meta()==10?255:203;
        }else if(entry.id().equals("tainted_log")&&quad.materialInfo().sprite().contents().name().getPath().endsWith("atlas_233")){
            int offset=side==4?1:side==5?2:side==3?3:0;tile=233+Math.abs((offset+pos.getY()+pos.getX()*pos.getZ())%3);
        }else if(entry.kind().equals("leaves"))tile=208+entry.meta()*2+(Minecraft.getInstance().options.cutoutLeaves().get()?0:1);
        if(tile<0)return quad;
        TextureAtlasSprite old=quad.materialInfo().sprite(),sprite=entry.id().equals("greatwood_leaves")
                ?Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS,Thaumcraft.id("block/atlas_"+tile+"_greatwood"))):sprite(tile);
        if(old==sprite)return quad;
        long[] uv=new long[4];for(int i=0;i<4;i++){
            float u=(UVPair.unpackU(quad.packedUV(i))-old.getU0())/(old.getU1()-old.getU0());
            float v=(UVPair.unpackV(quad.packedUV(i))-old.getV0())/(old.getV1()-old.getV0());uv[i]=UVPair.pack(sprite.getU(u),sprite.getV(v));
        }
        var info=quad.materialInfo();var material=new BakedQuad.MaterialInfo(sprite,info.layer(),info.itemRenderType(),info.tintIndex(),info.shade(),info.lightEmission());
        return new BakedQuad(quad.position0(),quad.position1(),quad.position2(),quad.position3(),uv[0],uv[1],uv[2],uv[3],quad.direction(),material);
    }
}
