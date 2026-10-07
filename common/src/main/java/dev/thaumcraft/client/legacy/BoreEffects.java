package dev.thaumcraft.client.legacy;

import dev.thaumcraft.network.BoreEffect;
import net.minecraft.core.BlockPos;
import static dev.thaumcraft.client.legacy.LegacyCompat.*;

public final class BoreEffects {
    private BoreEffects(){}
    public static void receive(BoreEffect event){
        var level=net.minecraft.client.Minecraft.getInstance().level;if(level==null)return;
        boolean low=dev.thaumcraft.PortConfig.lowGfx;
        if(low&&(event.kind()!=BoreEffect.TRAIL||!level.getRandom().nextBoolean()))return;
        World world=new World(level,level.getRandom().nextLong());world.effectsAllowed=true;
        LegacyCompat.capture(world,BlockPos.ZERO,0xf000f0,level.getGameTime(),()->{
            var p=event.origin();var t=event.target();int color=event.focus()==0?5:event.focus();
            FXWisp wisp=switch(event.kind()){
                case BoreEffect.PULSE->new FXWisp(world,p.x,p.y,p.z,t.x,t.y,t.z,.6,color);
                case BoreEffect.TRAIL->new FXWisp(world,p.x,p.y,p.z,.4,color);
                case BoreEffect.COLLECT->new FXWisp(world,p.x,p.y,p.z,1,color);
                default->null;
            };
            if(wisp==null)return;
            wisp.shrink=true;
            if(event.kind()!=BoreEffect.COLLECT)wisp.blend=1;
            new Effects().addEffect(wisp);
        });
    }
}
