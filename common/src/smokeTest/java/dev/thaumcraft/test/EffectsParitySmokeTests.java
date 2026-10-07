package dev.thaumcraft.test;

import com.mojang.authlib.GameProfile;
import dev.thaumcraft.entity.ArcaneMote;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

/** Exercises moving effects through their registered server entities and actual collision queries. */
public final class EffectsParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Effect parity: "+message);}
    private static List<ArcaneMote> motes(ServerLevel level,Vec3 origin){return level.getEntitiesOfClass(ArcaneMote.class,new AABB(origin.subtract(20,20,20),origin.add(20,20,20)));}
    private static ArcaneMote only(ServerLevel level,Vec3 origin){var all=motes(level,origin);check(all.size()==1,"One effect was spawned");return all.getFirst();}
    static int run(MinecraftServer server){
        checks=0;var level=server.overworld();BlockPos pos=new BlockPos(304,280,304);ArcanaParitySmokeTests.prepareEntities(level,pos);Vec3 origin=pos.getCenter();
        ArcaneMote.wind(level,origin,origin.add(2,0,0),false);var wind=only(level,origin);wind.tick();
        check(Math.abs(wind.getX()-origin.x-.1)<.00001&&wind.lifetime()==20,"Wind moves at one tenth block per tick for distance times ten");wind.discard();
        ArcaneMote.wind(level,origin,origin.add(2,0,0),true);wind=only(level,origin);
        check(Math.abs(wind.getX()-origin.x-2)<.00001&&Math.abs(wind.getDeltaMovement().x+.1)<.00001,"Pull wind begins at the far end and moves inward");wind.discard();
        var target=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);target.setPos(origin.add(0,0,10));level.addFreshEntity(target);
        ArcaneMote.beam(level,origin,target,3,false,true,2);var beam=only(level,origin);Vec3 firstVelocity=beam.getDeltaMovement();
        check(Math.abs(firstVelocity.length()-1.0/3)<.00001&&target.getHealth()==20,"Beam constructor retains original one-third speed and does not damage at launch");
        target.setPos(origin.add(8,0,10));beam.tick();check(beam.getDeltaMovement().equals(firstVelocity),"Non-homing beam retains its initial heading after target movement");beam.discard();
        ArcaneMote.beam(level,origin,target,3,true,false,2);beam=only(level,origin);target.setPos(origin.add(-8,0,10));beam.tick();
        check(beam.getDeltaMovement().x<0&&Math.abs(beam.getDeltaMovement().length()-.5)<.00001,"Homing beam updates heading at the configured inverse speed");beam.discard();target.discard();
        var slime=EntityType.SLIME.create(level,EntitySpawnReason.COMMAND);slime.setSize(3,true);slime.setPos(origin.add(.4,-.2,0));level.addFreshEntity(slime);float health=slime.getHealth();
        ArcaneMote.scorch(level,origin,slime,1,true,true,false,false);var scorch=only(level,origin);scorch.tick();
        check(scorch.isRemoved()&&slime.getHealth()<health,"Scorch monster filtering includes Enemy implementations such as slimes");slime.discard();
        var blocker=new ItemEntity(level,origin.x+.4,origin.y,origin.z,new ItemStack(Items.STONE));level.addFreshEntity(blocker);
        target=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);target.setPos(origin.add(.4,-.9,0));level.addFreshEntity(target);
        ArcaneMote.scorch(level,origin,target,1,true,true,true,false);scorch=only(level,origin);scorch.tick();
        check(!scorch.isRemoved()&&target.getHealth()==20,"A first non-living collision blocks a flame from selecting a later living entity");scorch.discard();blocker.discard();target.discard();
        var player=new ServerPlayer(server,level,new GameProfile(UUID.fromString("2c517000-2f39-4201-bd3a-80c509c3c321"),"EffectsParity"),ClientInformation.createDefault());
        new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false));
        player.setPos(origin);player.setYRot(-90);player.setXRot(0);
        Vec3 guideOrigin=player.getEyePosition().add(player.getLookAngle().scale(.3));BlockPos wall=BlockPos.containing(guideOrigin.add(1,0,0));
        level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(wall.above(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(wall.below(),Blocks.STONE.defaultBlockState());
        ArcaneMote.guide(level,player,guideOrigin.add(4,0,0));var guide=only(level,origin);check(Math.abs(guide.getBbWidth()-.2)<.00001&&guide.lifetime()==1000,"Guide retains original collision footprint and source lifespan");
        for(int i=0;i<100;i++)guide.tick();check(guide.isAlive()&&guide.getX()<=wall.getX()-.09,"Guide movement collides with solid blocks instead of passing through them");guide.discard();
        level.setBlockAndUpdate(wall,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(wall.above(),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(wall.below(),Blocks.AIR.defaultBlockState());
        ArcaneMote.guide(level,player,guideOrigin);guide=only(level,origin);guide.tick();check(guide.isRemoved(),"Guide arrival within three quarters of a block ends the source effect");
        return checks;
    }
}
