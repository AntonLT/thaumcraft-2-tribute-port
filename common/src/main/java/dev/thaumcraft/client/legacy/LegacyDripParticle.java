package dev.thaumcraft.client.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;

/** Retains the 40-tick hanging phase and original vis color through the landing phase. */
final class LegacyDripParticle extends SingleQuadParticle {
    private int hanging=40;
    LegacyDripParticle(LegacyCompat.FXWisp effect){
        super((ClientLevel)effect.world.level,effect.x,effect.y,effect.z,null);
        setColor(effect.red,effect.green,effect.blue);setSize(.01f,.01f);lifetime=(int)(64/(random.nextDouble()*.8+.2));
        xd=yd=zd=0;select("hang");
    }
    private void select(String phase){sprite=Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_PARTICLES,Identifier.withDefaultNamespace("drip_"+phase)));}
    @Override protected Layer getLayer(){return Layer.OPAQUE;}
    @Override public void tick(){
        xo=x;yo=y;zo=z;yd-=.06;
        if(hanging-->0){xd*=.02;yd*=.02;zd*=.02;select("hang");}else select("fall");
        move(xd,yd,zd);xd*=.98;yd*=.98;zd*=.98;if(age++>=lifetime)remove();
        if(onGround){select("land");xd*=.7;zd*=.7;}
        BlockPos pos=BlockPos.containing(x,y,z);var state=level.getBlockState(pos);var fluid=state.getFluidState();
        boolean fragile=state.getBlock() instanceof dev.thaumcraft.machine.MachineBlock machine&&dev.thaumcraft.content.Content.DEFINITIONS.get(machine.id()).legacy().endsWith("blockAppFragile");
        if(!fragile&&((!fluid.isEmpty()&&y<pos.getY()+fluid.getHeight(level,pos))||(state.isSolid()&&y<pos.getY()+1)))remove();
        var player=Minecraft.getInstance().player;if(player==null||player.distanceToSqr(x,y,z)>(dev.thaumcraft.PortConfig.lowGfx?225:900))remove();
    }
}
