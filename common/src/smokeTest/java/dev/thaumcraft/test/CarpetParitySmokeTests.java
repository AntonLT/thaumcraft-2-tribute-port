package dev.thaumcraft.test;

import com.mojang.authlib.GameProfile;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

final class CarpetParitySmokeTests {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError("Carpet parity: "+message);}
    private static final class Rider extends ServerPlayer {
        net.minecraft.world.phys.HitResult placementHit;
        Rider(MinecraftServer server){super(server,server.overworld(),new GameProfile(UUID.randomUUID(),"CarpetParity"),ClientInformation.createDefault());
            new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),this,net.minecraft.server.network.CommonListenerCookie.createInitial(getGameProfile(),false));
        }
        boolean dismountRequested(){return wantsToStopRiding();}
        @Override public net.minecraft.world.phys.HitResult pick(double range,float partial,boolean fluids){return placementHit==null?super.pick(range,partial,fluids):placementHit;}
    }
    static int run(MinecraftServer server){
        checks=0;var level=server.overworld();var pos=new BlockPos(400,280,400);
        ArcanaParitySmokeTests.prepareEntities(level,pos);
        var rider=new Rider(server);rider.setPos(pos.getBottomCenter());rider.setYRot(0);
        var other=new Rider(server);other.setPos(rider.position());
        var carpet=ModEntities.CARPET.create(level,EntitySpawnReason.COMMAND);carpet.setPos(rider.position());
        try{
            carpet.interact(rider,InteractionHand.MAIN_HAND,Vec3.ZERO);
            check(rider.getVehicle()==carpet,"Right click mounts");
            check(!carpet.isClientAuthoritative()&&carpet.isLocalInstanceAuthoritative(),"Mounted flight remains server authoritative");
            check(carpet.getInterpolation()!=null,"Remote movement has an interpolation handler");
            other.setShiftKeyDown(true);carpet.interact(other,InteractionHand.MAIN_HAND,Vec3.ZERO);
            check(!carpet.isRemoved()&&rider.getVehicle()==carpet&&!other.isPassenger(),"Another player's sneak interaction cannot destroy an occupied carpet");
            rider.setLastClientInput(new Input(true,false,false,false,true,false,false));
            var start=carpet.position();carpet.tick();
            check(carpet.getZ()>start.z&&carpet.getY()>start.y,"Forward and jump move the mounted carpet");
            carpet.setDeltaMovement(Vec3.ZERO);rider.setShiftKeyDown(true);
            rider.setLastClientInput(new Input(false,false,false,false,false,true,false));
            check(!rider.dismountRequested(),"Sneak alone descends without requesting dismount");
            start=carpet.position();carpet.tick();check(carpet.getY()<start.y,"Sneak accelerates downward");
            rider.setLastClientInput(new Input(false,false,false,false,true,true,false));
            check(!rider.dismountRequested(),"Sneak and jump do not dismount");
            carpet.setDeltaMovement(Vec3.ZERO);start=carpet.position();carpet.tick();
            check(carpet.getY()==start.y,"Opposite vertical inputs cancel");
            check(Math.abs(carpet.getBbWidth()-1.5)<.0001&&Math.abs(carpet.getBbHeight()-.6)<.0001,"Original collision dimensions");
            rider.tickCount=0;carpet.positionRider(rider);double high=rider.getY();
            rider.tickCount=12;carpet.positionRider(rider);
            check(Math.abs(rider.getY()-high)>.05&&rider.getY()<carpet.getY()+.4,"Standing rider follows the original bob");
            rider.setShiftKeyDown(false);rider.setLastClientInput(new Input(true,false,false,false,false,false,false));
            carpet.setDeltaMovement(Vec3.ZERO);carpet.setOnGround(false);carpet.tick();
            double straight=carpet.getDeltaMovement().horizontalDistance();
            check(Math.abs(straight-.02*.98*.91*2.3*.91)<.000001,"Legacy rider acceleration and drag");
            rider.setLastClientInput(new Input(true,false,true,false,false,false,false));
            carpet.setDeltaMovement(Vec3.ZERO);carpet.tick();
            check(carpet.getDeltaMovement().horizontalDistance()<straight*1.03,"Diagonal input is normalized");
            rider.setLastClientInput(new Input(true,false,false,false,false,true,false));
            carpet.setDeltaMovement(Vec3.ZERO);carpet.tick();
            check(Math.abs(carpet.getDeltaMovement().horizontalDistance()-straight*.3)<.000001,"Sneak slows horizontal rider motion");
            rider.setShiftKeyDown(true);
            carpet.interact(rider,InteractionHand.MAIN_HAND,Vec3.ZERO);
            check(!rider.isPassenger()&&!carpet.isRemoved(),"Right click toggles riding without dropping the carpet");
            check(rider.dismountRequested(),"Ordinary sneak dismount behavior is preserved away from carpets");
            carpet.interact(rider,InteractionHand.MAIN_HAND,Vec3.ZERO);
            check(rider.getVehicle()==carpet,"Sneak interaction can remount immediately as in the original");
            carpet.interact(rider,InteractionHand.MAIN_HAND,Vec3.ZERO);
            carpet.setDeltaMovement(.3,0,0);carpet.setYRot(0);carpet.tick();
            check(carpet.getYRot()!=0,"Unoccupied carpet turns while drifting");
            var neighbor=ModEntities.CARPET.create(level,EntitySpawnReason.COMMAND);neighbor.setPos(carpet.position().add(1,0,0));level.addFreshEntity(neighbor);
            carpet.setDeltaMovement(Vec3.ZERO);carpet.tick();
            check(neighbor.getDeltaMovement().horizontalDistance()>0&&carpet.getDeltaMovement().horizontalDistance()>0,"Nearby carpets push each other");neighbor.discard();
            carpet.setPos(pos.getBottomCenter());carpet.setDeltaMovement(Vec3.ZERO);
            level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState());carpet.tick();
            check(level.getBlockState(pos).isAir(),"Carpet clears snow beneath its corners");
            carpet.setPos(pos.getBottomCenter());carpet.setDeltaMovement(.2,0,0);
            level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
            level.setBlockAndUpdate(pos.above(),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
            start=carpet.position();carpet.tick();
            check(carpet.timeSinceHit()==10&&carpet.damageTaken()==10&&carpet.position().equals(start),"Water hit pauses movement and damage decay");
            level.removeBlock(pos,false);level.removeBlock(pos.above(),false);
            carpet.setEndurance(400);carpet.hurtServer(level,carpet.damageSources().generic(),5);
            var drops=level.getEntitiesOfClass(ItemEntity.class,carpet.getBoundingBox().inflate(2));
            check(carpet.isRemoved()&&drops.stream().anyMatch(e->e.getItem().is(Content.item("flying_carpet"))&&e.getItem().getDamageValue()==200),"Breaking the carpet preserves its remaining endurance");
            drops.forEach(ItemEntity::discard);
            var ground=pos.offset(3,-1,0);level.setBlockAndUpdate(ground,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(ground.above(),net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState());
            rider.placementHit=new net.minecraft.world.phys.BlockHitResult(ground.above().getCenter(),net.minecraft.core.Direction.UP,ground.above(),false);
            rider.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var stack=new net.minecraft.world.item.ItemStack(Content.item("flying_carpet"));stack.setDamageValue(123);
            var entry=Content.entry(stack);
            dev.thaumcraft.entity.RelicEntities.use(level,rider,InteractionHand.MAIN_HAND,stack,entry);
            var placed=level.getEntitiesOfClass(dev.thaumcraft.entity.CarpetEntity.class,new net.minecraft.world.phys.AABB(ground).inflate(2));
            check(stack.isEmpty()&&placed.size()==1,"Creative placement consumes exactly one carpet");
            check(placed.getFirst().getY()==ground.getY()+1&&placed.getFirst().endurance()==477,"Snow placement uses the supporting block and preserves charge");
            placed.forEach(net.minecraft.world.entity.Entity::discard);
            level.setBlockAndUpdate(ground.above(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            rider.placementHit=new net.minecraft.world.phys.BlockHitResult(ground.getCenter(),net.minecraft.core.Direction.UP,ground,false);
            stack=new net.minecraft.world.item.ItemStack(Content.item("flying_carpet"));
            dev.thaumcraft.entity.RelicEntities.use(level,rider,InteractionHand.MAIN_HAND,stack,entry);
            placed=level.getEntitiesOfClass(dev.thaumcraft.entity.CarpetEntity.class,new net.minecraft.world.phys.AABB(ground).inflate(2));
            check(stack.isEmpty()&&placed.size()==1,"Legacy placement does not reject destination overlap");placed.forEach(net.minecraft.world.entity.Entity::discard);
            var obstruction=net.minecraft.world.entity.EntityType.COW.create(level,EntitySpawnReason.COMMAND);obstruction.setPos(rider.getEyePosition().add(0,-.5,0));level.addFreshEntity(obstruction);
            stack=new net.minecraft.world.item.ItemStack(Content.item("flying_carpet"));
            var result=dev.thaumcraft.entity.RelicEntities.use(level,rider,InteractionHand.MAIN_HAND,stack,entry);
            check(result==net.minecraft.world.InteractionResult.FAIL&&stack.getCount()==1,"Eye inside a collidable entity blocks placement without consumption");obstruction.discard();
            level.removeBlock(ground.above(),false);level.removeBlock(ground,false);
        }finally{rider.stopRiding();other.stopRiding();carpet.discard();rider.discard();other.discard();}
        return checks;
    }
}
