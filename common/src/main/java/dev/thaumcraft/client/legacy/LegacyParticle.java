package dev.thaumcraft.client.legacy;

import dev.thaumcraft.Thaumcraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;

/** Original wisp/sparkle lifetime, color, motion and sprite animation. */
final class LegacyParticle extends SingleQuadParticle {
    private final boolean sparkle,shrink,tinkle;
    private final int multiplier;
    private final float originalSize;
    private final Layer layer;
    LegacyParticle(LegacyCompat.FXWisp effect){
        super((ClientLevel)effect.world.level,effect.x,effect.y,effect.z,null);
        sparkle=effect instanceof LegacyCompat.FXSparkle;shrink=effect.shrink;tinkle=effect.tinkle;multiplier=effect.multiplier;
        lifetime=effect.lifetime;gravity=effect.gravity;friction=.98f;xd=effect.dx;yd=effect.dy;zd=effect.dz;
        setColor(effect.red,effect.green,effect.blue);alpha=sparkle?1:.5f;
        originalSize=(sparkle?.1f:.5f)*effect.size*(random.nextFloat()*.5f+.5f)*2;
        var texture=Thaumcraft.id("textures/legacy/"+(sparkle?"particles.png":"p_large.png"));
        // Texture uploads must happen before the particle render pass is opened.
        net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(texture);
        layer=new Layer(true,texture,LegacyPipelines.pipeline(true,770,effect.blend,false));
    }
    @Override protected Layer getLayer(){return layer;}
    @Override protected int getLightCoords(float partial){return 0xf000f0;}
    @Override public float getQuadSize(float partial){
        if(sparkle)return originalSize*(lifetime-age+1f)/lifetime;
        float scale=shrink?1-age/(float)lifetime:age/(float)Math.max(1,lifetime/2);if(!shrink&&scale>1)scale=2-scale;
        return originalSize*Math.max(0,scale);
    }
    private int frame(){return 16+Math.min(2,age/multiplier);}
    @Override protected float getU0(){return sparkle?(frame()%8)/8f:0;}
    @Override protected float getU1(){return sparkle?getU0()+.124875f:1;}
    @Override protected float getV0(){return sparkle?(frame()/8)/8f:0;}
    @Override protected float getV1(){return sparkle?getV0()+.124875f:1;}
    @Override public void tick(){
        if(age==0&&tinkle&&random.nextInt(sparkle?10:3)==0)level.playLocalSound(x,y,z,net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.AMBIENT,.02f,(sparkle?.7f:.5f)*((random.nextFloat()-random.nextFloat())*.6f+2),false);
        super.tick();var player=net.minecraft.client.Minecraft.getInstance().player;
        if(player==null||player.distanceToSqr(x,y,z)>(dev.thaumcraft.PortConfig.lowGfx?625:2500))remove();
    }
}
