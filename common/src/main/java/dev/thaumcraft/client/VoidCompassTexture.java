package dev.thaumcraft.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.ModSounds;
import dev.thaumcraft.network.VoidCompassTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** One shared, texture-clock compass, using TextureVoidCompassFX's original equations and pixels. */
public final class VoidCompassTexture {
    public static final VoidCompassTexture INSTANCE=new VoidCompassTexture();
    private long requestId;
    private VoidCompassTarget pending;
    private net.minecraft.client.multiplayer.ClientLevel requestLevel;
    private static final net.minecraft.resources.Identifier SPRITE=Thaumcraft.id("item/atlas_61");
    private TextureAtlasSprite sprite;
    private int[] background;
    int count;
    double lastX,lastZ,angle,velocity;

    public void tick(TextureAtlas atlas){
        var mc=Minecraft.getInstance();var player=mc.player;var level=mc.level;
        if(requestLevel!=level){requestLevel=level;requestId++;pending=null;}
        boolean carrying=player!=null&&player.inventoryMenu.slots.stream().anyMatch(slot->slot.getItem().is(Content.item("void_compass")));
        boolean sound=false;
        if(pending!=null){
            if(level!=null&&pending.dimension().equals(level.dimension())&&pending.requestId()==requestId)
                sound=updateTarget(carrying,pending.target().orElse(null));
            pending=null;
        }
        if(--count<=0){
            requestId++;
            if(level!=null&&player!=null){
                if(carrying)VoidCompassTarget.requestSender.accept(new VoidCompassTarget.Request(requestId));
                else updateTarget(false,null);
            }
            count=60;
        }
        if(sound)level.playLocalSound(player.getX(),player.getY(),player.getZ(),ModSounds.event("monolithfound"),SoundSource.PLAYERS,2,1,false);
        double target=0;
        if(level!=null&&player!=null)target=direction(player.position(),player.getYRot(),level.dimensionType().skybox()==net.minecraft.world.level.dimension.DimensionType.Skybox.OVERWORLD,Math::random);
        advance(target);
        upload(atlas);
    }

    public void receive(VoidCompassTarget target){if(target.requestId()==requestId)pending=target;}

    boolean updateTarget(boolean carrying,BlockPos target){
        double x=carrying&&target!=null?target.getX()+.5:0,z=carrying&&target!=null?target.getZ()+.5:0;
        // Preserve the original AND condition, including the cue on losing a target.
        boolean sound=carrying&&lastX!=x&&lastZ!=z;
        lastX=x;lastZ=z;return sound;
    }
    double direction(Vec3 player,float yaw,boolean surface,java.util.function.DoubleSupplier random){
        if(!surface||lastX==0&&lastZ==0)return random.getAsDouble()*Math.PI*2;
        return (yaw-90.0F)*Math.PI/180-Math.atan2(lastZ-player.z,lastX-player.x);
    }
    void advance(double target){
        double delta=target-angle;
        while(delta< -Math.PI)delta+=Math.PI*2;
        while(delta>=Math.PI)delta-=Math.PI*2;
        velocity=(velocity+Math.clamp(delta,-1,1)*.1)*.8;
        angle+=velocity;
    }
    static void drawNeedle(NativeImage image,double angle){
        int size=image.getWidth();double sine=Math.sin(angle),cosine=Math.cos(angle),length=.3*(size/16.0),cx=size/2.0+.5,cy=size/2.0-.5;
        for(int step=-4;step<=4;step++)image.setPixel((int)(cx+cosine*step*length),(int)(cy-sine*step*length*.5),0xff646464);
        for(int step=-8;step<=16;step++)image.setPixel((int)(cx+sine*step*length),(int)(cy+cosine*step*length*.5),step>=0?0xffaf14ff:0xff646464);
    }
    private void upload(TextureAtlas atlas){
        var current=atlas.getSprite(SPRITE);
        if(!current.contents().name().equals(SPRITE))return;
        if(sprite!=current){
            try(var input=Minecraft.getInstance().getResourceManager().open(Thaumcraft.id("textures/item/atlas_61.png"));var image=NativeImage.read(input)){
                if(image.getWidth()!=current.contents().width()||image.getHeight()!=current.contents().height()||image.getWidth()!=image.getHeight())
                    throw new IllegalStateException("Void Compass texture must be square and match its sprite");
                background=image.getPixels();sprite=current;
            }catch(java.io.IOException e){throw new IllegalStateException("Cannot load Void Compass texture",e);}
        }
        int size=sprite.contents().width();
        var frame=new NativeImage(size,size,false);
        for(int y=0;y<size;y++)for(int x=0;x<size;x++)frame.setPixel(x,y,background[y*size+x]);
        drawNeedle(frame,angle);
        var texture=atlas.getTexture();
        NativeImage[] mips={frame};
        try{
            mips=MipmapGenerator.generateMipLevels(SPRITE,mips,texture.getMipLevels()-1,MipmapStrategy.AUTO,0,sprite.transparency());
            int padding=Math.round(sprite.getU0()*texture.getWidth(0))-sprite.getX();
            var encoder=RenderSystem.getDevice().createCommandEncoder();
            for(int mip=0;mip<mips.length;mip++){
                var source=mips[mip];int border=padding>>mip,width=source.getWidth(),height=source.getHeight();
                // Keep atlas padding current as well, for filtered/minified item rendering.
                try(var padded=new NativeImage(width+border*2,height+border*2,false)){
                    for(int y=0;y<padded.getHeight();y++)for(int x=0;x<padded.getWidth();x++)
                        padded.setPixel(x,y,source.getPixel(Math.clamp(x-border,0,width-1),Math.clamp(y-border,0,height-1)));
                    encoder.writeToTexture(texture,padded,mip,0,sprite.getX()>>mip,sprite.getY()>>mip,padded.getWidth(),padded.getHeight(),0,0);
                }
            }
        }finally{for(var image:mips)image.close();}
    }
}
