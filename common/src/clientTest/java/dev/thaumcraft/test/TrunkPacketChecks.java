package dev.thaumcraft.test;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.entity.ModEntities;
import dev.thaumcraft.entity.TravelingTrunk;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntitySpawnReason;

/** A real server hop must reach the client animation without a protocol disconnect. */
final class TrunkPacketChecks {
    private static volatile int entityId=-1;
    private static int ticks;
    private static boolean sent,finished;

    static boolean tick(){
        if(finished)return true;
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();
        if(++ticks>200)throw new AssertionError("Trunk hop packet did not animate the client");
        if(ticks==1)server.execute(()->{
            var player=server.getPlayerList().getPlayers().getFirst();
            var trunk=(TravelingTrunk)ModEntities.TYPES.get("traveling_trunk").create(player.level(),EntitySpawnReason.COMMAND);
            trunk.setOwner(player.getUUID());trunk.setNoAi(true);trunk.setNoGravity(true);
            trunk.setPos(player.getX()+6,player.getY(),player.getZ());
            player.level().addFreshEntity(trunk);entityId=trunk.getId();
        });
        if(!(mc.level.getEntity(entityId) instanceof TravelingTrunk trunk))return false;
        if(!sent){
            sent=true;
            server.execute(()->{
                var player=server.getPlayerList().getPlayers().getFirst();
                var original=(TravelingTrunk)player.level().getEntity(entityId);
                original.setPos(player.getX()+6,player.getY(),player.getZ());
                original.setOnGround(true);
                // Exercise the production event producer after the client tracks this entity.
                for(int i=0;i<31&&original.squish(1)<=0;i++)original.getMoveControl().tick();
                if(original.squish(1)<=0)throw new AssertionError("Trunk fixture failed to produce a server hop");
            });
        }
        if(trunk.squish(1)<=0)return false;
        if(!mc.getConnection().getConnection().isConnected())throw new AssertionError("Trunk hop disconnected the client");
        server.execute(()->{
            var player=server.getPlayerList().getPlayers().getFirst();
            var original=player.level().getEntity(entityId);
            if(original!=null)original.discard();
        });
        finished=true;
        Thaumcraft.LOG.info("THAUMCRAFT_TRUNK_PACKET_PASS server_hop_animates_client");
        return true;
    }
}
