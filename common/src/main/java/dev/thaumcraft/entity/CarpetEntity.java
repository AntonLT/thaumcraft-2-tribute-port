package dev.thaumcraft.entity;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.VisualEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class CarpetEntity extends Entity {
    private int movementTicks;
    private final InterpolationHandler interpolation=new InterpolationHandler(this);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> HIT=SynchedEntityData.defineId(CarpetEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT),DIRECTION=SynchedEntityData.defineId(CarpetEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DAMAGE=SynchedEntityData.defineId(CarpetEntity.class,net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ENDURANCE=SynchedEntityData.defineId(CarpetEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    public int endurance(){return entityData.get(ENDURANCE);}
    public int timeSinceHit(){return entityData.get(HIT);}
    public float damageTaken(){return entityData.get(DAMAGE);}
    public int forwardDirection(){return entityData.get(DIRECTION);}
    public CarpetEntity(EntityType<? extends CarpetEntity> type,Level level){super(type,level);setNoGravity(true);setYRot(90);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {builder.define(ENDURANCE,600);builder.define(HIT,0);builder.define(DIRECTION,1);builder.define(DAMAGE,0f);}
    @Override public boolean isPickable(){return true;}
    @Override public boolean canBeCollidedWith(Entity other){return true;}
    @Override public boolean isPushable(){return true;}
    @Override protected void positionRider(Entity passenger,MoveFunction move){
        float bob=Mth.sin((passenger.tickCount+4)/4f)*.0725f+.0725f;
        // Legacy carpet position was bottom + .3, and the standing player's feet
        // were mountedYOffset - .5 above that position.
        move.accept(passenger,getX(),getY()+.4-bob,getZ());
    }
    @Override protected boolean canAddPassenger(Entity passenger){return getPassengers().isEmpty();}
    // Keep Entity's null controlling passenger: flight uses input on the server,
    // not the client-controlled vehicle movement protocol.
    @Override public InterpolationHandler getInterpolation(){return interpolation;}
    @Override public InteractionResult interact(Player player,InteractionHand hand,Vec3 location) {
        if(!level().isClientSide()) {
            if(player.getVehicle()==this)player.stopRiding();
            else if(!isVehicle())player.startRiding(this,true,true);
        }
        return InteractionResult.SUCCESS;
    }
    private void drop(ServerLevel level) {
        ItemStack carpet=new ItemStack(Content.item(endurance()>0?"flying_carpet":"inert_carpet"));if(endurance()>0)carpet.setDamageValue(600-endurance());
        spawnAtLocation(level,carpet);
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount) {
        if(isInvulnerableToBase(source))return false;
        entityData.set(DIRECTION,-forwardDirection());entityData.set(HIT,10);entityData.set(DAMAGE,damageTaken()+amount*10);hurtMarked=true;
        if(damageTaken()>40){drop(level);ejectPassengers();discard();}return true;
    }
    @Override public void tick() {
        super.tick();
        if(getFirstPassenger()!=null){getFirstPassenger().resetFallDistance();resetFallDistance();}
        if(!level().isClientSide()){
            if(movementTicks>=20){movementTicks-=20;setEndurance(endurance()-1);}
            if(endurance()<=0){drop((ServerLevel)level());ejectPassengers();discard();return;}
            if(isInWater()&&timeSinceHit()<6){hurtServer((ServerLevel)level(),damageSources().drown(),1);return;}
        }
        if(timeSinceHit()>0)entityData.set(HIT,timeSinceHit()-1);if(damageTaken()>0)entityData.set(DAMAGE,Math.max(0,damageTaken()-1));
        if(level().isClientSide()){VisualEffects.entity.accept(this);interpolation.interpolate();return;}
        if(getDeltaMovement().horizontalDistance()>.15)movementTicks++;
        if(getFirstPassenger() instanceof ServerPlayer player && endurance()>0) {
            var input=player.getLastClientInput();
            float forward=(input.forward()?1:0)-(input.backward()?1:0),strafe=(input.left()?1:0)-(input.right()?1:0);
            // Reconstruct the old airborne rider step from input on the server:
            // input damping, sneak scaling, moveFlying normalization, then drag.
            float damping=.98f*(input.shift()?.3f:1);
            forward*=damping;strafe*=damping;
            double acceleration=(input.sprint()?.026:.02)*.91*2.3/Math.max(1,Math.hypot(forward,strafe));
            double yaw=Math.toRadians(player.getYRot());
            Vec3 velocity=getDeltaMovement();
            double dy=velocity.y+(input.jump()?.0275:0)-(input.shift()?.0275:0);
            setDeltaMovement(velocity.x+(-Math.sin(yaw)*forward+Math.cos(yaw)*strafe)*acceleration,dy,velocity.z+(Math.cos(yaw)*forward+Math.sin(yaw)*strafe)*acceleration);
            setYRot(getYRot()+(player.getYRot()-getYRot())*.2f);
        }else if(getFirstPassenger()!=null){
            var motion=getFirstPassenger().getDeltaMovement();
            setDeltaMovement(getDeltaMovement().add(motion.x*2.3,0,motion.z*2.3));
        }
        var velocity=getDeltaMovement();
        setDeltaMovement(Math.clamp(velocity.x,-.5,.5),velocity.y,Math.clamp(velocity.z,-.5,.5));
        if(onGround())setDeltaMovement(getDeltaMovement().multiply(.5,1,.5));
        var previous=position();
        move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(.91));
        setXRot(0);
        if(!isVehicle()){
            double dx=previous.x-getX(),dz=previous.z-getZ();
            if(dx*dx+dz*dz>.001){
                double turn=Math.toDegrees(Math.atan2(dz,dx))-(getYRot()-90);
                while(turn>=90)turn-=180;
                while(turn< -180)turn+=360;
                setYRot(getYRot()+(float)Math.clamp(turn,-20,20));
            }
        }
        for(var other:level().getEntities(this,getBoundingBox().inflate(.2,0,.2)))
            if(other instanceof CarpetEntity&&other.isPushable())other.push(this);
        for(int corner=0;corner<4;corner++){
            var pos=BlockPos.containing(getX()+(corner%2-.5)*.8,getY()+.3,getZ()+(corner/2-.5)*.8);
            if(level().getBlockState(pos).is(Blocks.SNOW))level().removeBlock(pos,false);
        }
    }
    public void setEndurance(int endurance){entityData.set(ENDURANCE,Math.clamp(endurance,0,600));}
    @Override protected void readAdditionalSaveData(ValueInput input){setEndurance(input.getIntOr("endurance",600));movementTicks=input.getIntOr("movementTicks",0);}
    @Override protected void addAdditionalSaveData(ValueOutput output){output.putInt("endurance",endurance());output.putInt("movementTicks",movementTicks);}
}
