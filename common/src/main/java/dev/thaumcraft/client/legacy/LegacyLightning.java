package dev.thaumcraft.client.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.*;
import static dev.thaumcraft.client.legacy.LegacyCompat.*;

/** Bounded block-local lifetimes for the original non-damaging display lightning. */
final class LegacyLightning {
    private record Bolt(LightningShape shape,long born,int initialAge,int type) {}
    private static final Map<BlockPos,List<Bolt>> BOLTS=new LinkedHashMap<>();
    private static Level level;
    private LegacyLightning() {}
    private static void level(World world){if(level!=world.level){BOLTS.clear();level=world.level;}}
    static void spawn(World world,BlockPos pos,WRVector3 from,WRVector3 to,int type,int duration,float multiplier){
        level(world);var bolts=BOLTS.computeIfAbsent(pos,ignored->new ArrayList<>());
        if(bolts.size()>=4)bolts.remove(0);
        var shape=new LightningShape(from,to,world.rand.nextLong(),duration,multiplier);
        bolts.add(new Bolt(shape,world.level.getGameTime(),shape.particleAge,type));
        if(BOLTS.size()>256)BOLTS.remove(BOLTS.keySet().iterator().next());
    }
    static void render(World world,BlockPos pos,float partial){
        level(world);var bolts=BOLTS.get(pos);if(bolts==null)return;
        long now=world.level.getGameTime();bolts.removeIf(b->now-b.born+b.initialAge>=b.shape.particleMaxAge);
        if(bolts.isEmpty()){BOLTS.remove(pos);return;}
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();var at=camera.position();var facing=camera.forwardVector();
        var draw=context().draw();draw.matrixMode(5888);draw.matrix().identity();draw.offset(pos.getX(),pos.getY(),pos.getZ());
        GL11.glEnable(3042);GL11.glDepthMask(false);
        for(var bolt:bolts){
            var shape=bolt.shape;shape.particleAge=bolt.initialAge+(int)(now-bolt.born);shape.view=new WRVector3(at.x-pos.getX(),at.y-pos.getY(),at.z-pos.getZ());
            for(int pass=0;pass<2;pass++){
                shape.particleRed=bolt.type==5?(pass==0?.6f:0):(pass==0?.6f:1);
                shape.particleGreen=bolt.type==5?(pass==0?.2f:0):(pass==0?.3f:.6f);shape.particleBlue=shape.particleRed;
                GL11.glBlendFunc(770,bolt.type==5?771:1);
                MinecraftForgeClient.bindTexture("/thaumcraft/resources/"+(pass==0?"p_large.png":"p_small.png"));
                Tessellator.instance.startDrawingQuads();Tessellator.instance.setBrightness(15728880);
                shape.renderBolt(Tessellator.instance,partial,new WRVector3(facing.x(),facing.y(),facing.z()),pass);Tessellator.instance.draw();
            }
        }
        GL11.glDepthMask(true);GL11.glDisable(3042);
    }
}
