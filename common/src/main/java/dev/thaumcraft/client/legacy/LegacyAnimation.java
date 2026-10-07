package dev.thaumcraft.client.legacy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import static dev.thaumcraft.client.legacy.LegacyCompat.*;

/** Client animation state, advanced at 20 Hz using the original tile update equations. */
final class LegacyAnimation {
    private static final Map<Level,Map<BlockPos,LegacyAnimation>> LEVEL_STATES=new java.util.WeakHashMap<>();
    private final Random random;
    private long tick=Long.MIN_VALUE;
    private int count,pressDelay,previousProgress,podAge=-1;
    private boolean expanding,pressing;
    private float rotation,previousRotation,target,open,previousOpen,page,previousPage,pageTarget,pageSpeed;
    private float bellows=1,press=.125f,condenserAngle,condenserSpeed,boreAngle,generatorAngle,portalSize,podGrowth;
    private LegacyAnimation(BlockPos pos){random=new Random(pos.asLong());}
    static void apply(TileEntity tile){
        Level level=tile.worldObj.level;BlockPos pos=new BlockPos(tile.xCoord,tile.yCoord,tile.zCoord);
        // A portal pass can render another level between two main-world frames.
        // Keep each level's growth/animation history without retaining closed scenes.
        var states=LEVEL_STATES.computeIfAbsent(level,key->new LinkedHashMap<>(256,.75f,true));
        LegacyAnimation animation=states.computeIfAbsent(pos,LegacyAnimation::new);
        if(states.size()>4096)states.remove(states.keySet().iterator().next());
        long now=level.getGameTime();int steps=animation.tick==Long.MIN_VALUE?1:(int)Math.clamp(now-animation.tick,0,20);
        for(int i=0;i<steps;i++)animation.step(tile,pos);
        animation.tick=now;animation.copy(tile);
    }
    static void release(Level level){LEVEL_STATES.remove(level);}
    private static float wrap(float angle){while(angle>=Math.PI)angle-=2*(float)Math.PI;while(angle< -Math.PI)angle+=2*(float)Math.PI;return angle;}
    private void step(TileEntity tile,BlockPos pos){
        count++;previousRotation=rotation;previousOpen=open;previousPage=page;
        boolean book=tile instanceof TileEnchanter;
        var player=tile.worldObj.level.getNearestPlayer(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,tile instanceof TileBrain||tile instanceof TileEnchanterAdvanced?5:3,false);
        if(player!=null){
            target=(float)Math.atan2(player.getZ()-pos.getZ()-.5,player.getX()-pos.getX()-.5);open+=.1f;
            if(open<.5f||random.nextInt(40)==0){float old=pageTarget;do{pageTarget+=random.nextInt(4)-random.nextInt(4);}while(old==pageTarget);}
        }else{target+=book?.02f:.01f;open-=.1f;}
        target=wrap(target);rotation=wrap(rotation);rotation+=wrap(target-rotation)*(book?.4f:.04f);
        open=Math.clamp(open,0,1);pageSpeed+=(Math.clamp((pageTarget-page)*.4f,-.2f,.2f)-pageSpeed)*.9f;page+=pageSpeed;
        if(tile instanceof TileBellows){
            var state=tile.worldObj.level.getBlockState(pos);var facing=state.getValue(dev.thaumcraft.machine.MachineBlock.FACING);
            var neighbor=tile.worldObj.level.getBlockEntity(pos.relative(facing));
            boolean boosting=neighbor instanceof dev.thaumcraft.machine.MachineBlockEntity machine&&(machine.machineId().contains("crucible")||machine.machineId().equals("arcane_furnace")||!tile.powered&&machine.machineId().contains("vis_"));
            if(boosting){if(bellows>.35f&&!expanding)bellows-=.075f;if(bellows<=.35f&&!expanding)expanding=true;}else expanding=true;
            if(bellows<1&&expanding)bellows+=.025f;if(bellows>=1&&expanding)expanding=false;
        }
        if(tile instanceof TileDuplicator){
            if(tile.duplicatorCopyTime<previousProgress)pressing=true;previousProgress=tile.duplicatorCopyTime;
            if(press>0&&!pressing)press*=.97f;if(press<.125f&&!pressing)press=.125f;
            if(press<.1875f&&pressing)press*=1.1f;
            if(press>=.1875f&&pressing&&pressDelay<=0){pressDelay=12;press=.1875f;}
            if(pressDelay>0)pressDelay--;if(pressDelay<=0&&pressing&&press>=.1875f)pressing=false;
        }
        if(tile instanceof TileCondenser&&!tile.powered){
            if(tile.worldObj.level.getBlockEntity(pos) instanceof dev.thaumcraft.machine.MachineBlockEntity machine)condenserSpeed=machine.processes.speed;
            condenserAngle=(condenserAngle+condenserSpeed*5)%360;
        }
        if(tile instanceof TileBore&&tile.worked)boreAngle=(boreAngle+(tile.focus==1?4:2))%360;
        // Original single-player TileGenerator increments twice per tick, even while powered or empty.
        if(tile instanceof TileGenerator){generatorAngle+=2;if(generatorAngle>360)generatorAngle-=360;}
        if(tile instanceof TileSeal){
            if(tile.portalOpen&&portalSize<1.4f)portalSize+=.15f;
            if((!tile.portalOpen||tile.duplicatorCopyTime>0)&&portalSize>0)portalSize-=.25f;
            portalSize=Math.clamp(portalSize,0,1.4f);
        }
        if(tile instanceof TileTaintSeed){
            var state=tile.worldObj.level.getBlockState(pos);int age=state.getValue(dev.thaumcraft.world.TaintPodBlock.AGE);
            if(age!=podAge){podGrowth=age*50;podAge=age;}
            if(!state.getValue(dev.thaumcraft.world.TaintPodBlock.BURSTING))podGrowth=Math.min((age+1)*50,podGrowth+1);
        }
    }
    private void copy(TileEntity tile){
        tile.field_40068_a=tile.bobbin=count;tile.rota=tile.field_40069_h=rotation;tile.rotb=tile.field_40067_p=previousRotation;
        tile.field_40059_f=open;tile.field_40060_g=previousOpen;tile.field_40063_b=page;tile.field_40065_c=previousPage;
        tile.scale=bellows;tile.press=press;
        if(tile instanceof TileInfuser&&"thaumic_infuser".equals(tile.id))tile.angle=tile.currentItemCopyCost<=0?0:(int)(tile.duplicatorCopyTime*360.0f/tile.currentItemCopyCost);
        else tile.angle=condenserAngle;
        tile.rotation=boreAngle;tile.pSize=portalSize;
        if(tile instanceof TileGenerator)tile.rotation=generatorAngle;
        if(tile instanceof TileTaintSeed)tile.growth=podGrowth;
    }
}
