package dev.thaumcraft.test;

import dev.thaumcraft.entity.LightningGeometry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;

/** Fractal determinism and numerical edge cases across wand, seal and coincident endpoints. */
public final class LightningParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Lightning parity: "+message);}
    static int run(MinecraftServer server){
        checks=0;var end=new Vec3(0,0,20);var a=new LightningGeometry(end,216,.3f,6);var b=new LightningGeometry(end,216,.3f,6);
        check(a.mainSegments()==128&&a.segments().size()>128,"Seven subdivisions retain a 128-segment main bolt plus branches");
        check(a.segments().size()==b.segments().size(),"The same seed creates the same branch count");
        for(int i=0;i<a.segments().size();i++)check(a.segments().get(i).end.position().equals(b.segments().get(i).end.position()),"Seeded branch endpoints are reproducible");
        check(a.segments().stream().anyMatch(s->s.light<1),"Forks retain weaker light intensity");
        for(Vec3 target:java.util.List.of(Vec3.ZERO,new Vec3(0,20,0),new Vec3(20,0,0)))for(var s:new LightningGeometry(target,11,1.25f,3).segments())
            check(Double.isFinite(s.start.position().lengthSqr())&&Double.isFinite(s.previousSine)&&s.previousSine>0,"Degenerate and axis-aligned bolts remain finite");
        check(LightningGeometry.rotate(new Vec3(1,0,0),90,new Vec3(0,0,1)).distanceTo(new Vec3(0,-1,0))<.00001,"WRMat4 rotation orientation is preserved");
        return checks;
    }
}
