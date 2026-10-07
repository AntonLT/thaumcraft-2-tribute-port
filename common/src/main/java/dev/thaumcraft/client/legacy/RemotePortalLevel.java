package dev.thaumcraft.client.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import java.util.BitSet;
import java.util.Iterator;

/** Independent destination chunks and light, never installed in the player's chunk cache. */
public final class RemotePortalLevel {
    public final ClientLevel level;
    private final LevelRenderer renderer;
    private final java.util.Set<Integer> entities=new java.util.HashSet<>();
    private long entityUpdate;
    public RemotePortalLevel(Minecraft mc,LevelRenderer renderer,BlockPos target,int radius){
        this.renderer=renderer;var source=mc.level;
        level=new ClientLevel(mc.getConnection(),new ClientLevel.ClientLevelData(source.getDifficulty(),source.getLevelData().isHardcore(),false),source.dimension(),source.dimensionTypeRegistration(),radius,0,renderer,source.isDebug(),0,source.getSeaLevel()){
            private net.minecraft.world.level.biome.BiomeManager portalBiomes;
            @Override public net.minecraft.world.level.biome.BiomeManager getBiomeManager(){
                if(portalBiomes==null)portalBiomes=source.getBiomeManager().withDifferentSource(this);
                return portalBiomes;
            }
        };
        level.getChunkSource().updateViewCenter(target.getX()>>4,target.getZ()>>4);
        level.setTimeFromServer(source.getGameTime());
    }
    public void apply(ClientboundLevelChunkWithLightPacket packet){
        int x=packet.getX(),z=packet.getZ();var data=packet.getChunkData();
        var chunk=level.getChunkSource().replaceWithPacketData(x,z,data.getReadBuffer(),data.getHeightmaps(),data.getBlockEntitiesTagsConsumer(x,z));
        if(chunk==null)return;
        var light=packet.getLightData();var engine=level.getChunkSource().getLightEngine();
        sections(x,z,LightLayer.SKY,light.getSkyYMask(),light.getEmptySkyYMask(),light.getSkyUpdates().iterator());
        sections(x,z,LightLayer.BLOCK,light.getBlockYMask(),light.getEmptyBlockYMask(),light.getBlockUpdates().iterator());
        engine.setLightEnabled(new ChunkPos(x,z),true);
        for(int i=0;i<chunk.getSections().length;i++)engine.updateSectionStatus(SectionPos.of(x,level.getSectionYFromSectionIndex(i),z),chunk.getSections()[i].hasOnlyAir());
        var mc=Minecraft.getInstance();var mainRenderer=mc.levelRenderer;
        try{((dev.thaumcraft.mixin.PortalMinecraftAccess)mc).thaumcraft$levelRenderer(renderer);engine.runLightUpdates();}
        finally{((dev.thaumcraft.mixin.PortalMinecraftAccess)mc).thaumcraft$levelRenderer(mainRenderer);}
        level.setSectionRangeDirty(x-1,level.getMinSectionY(),z-1,x+1,level.getMaxSectionY(),z+1);
        renderer.onChunkReadyToRender(chunk.getPos());
        SodiumPortals.lightReady(level,x,z);
    }
    public void applyEntities(java.util.List<dev.thaumcraft.network.PortalScenes.EntityView> snapshots){
        clearEntities();entityUpdate=System.currentTimeMillis();
        for(var snapshot:snapshots){
            var packet=snapshot.spawn();net.minecraft.world.entity.Entity entity;
            if(packet.getType()==net.minecraft.world.entity.EntityType.PLAYER){
                var info=Minecraft.getInstance().getConnection().getPlayerInfo(packet.getUUID());
                if(info==null)continue;entity=new net.minecraft.client.player.RemotePlayer(level,info.getProfile());
            }else entity=packet.getType().create(level,net.minecraft.world.entity.EntitySpawnReason.LOAD);
            if(entity==null)continue;
            entity.recreateFromPacket(packet);entity.getEntityData().assignValues(snapshot.data().packedItems());entity.tickCount=snapshot.ticks();
            if(entity instanceof net.minecraft.world.entity.LivingEntity living){
                living.yBodyRot=living.yBodyRotO=snapshot.bodyYaw();living.yHeadRot=living.yHeadRotO=packet.getYHeadRot();
                living.hurtTime=snapshot.hurt();living.deathTime=snapshot.death();
                if(snapshot.equipment()!=null)for(var slot:snapshot.equipment().getSlots())living.setItemSlot(slot.getFirst(),slot.getSecond().copy());
            }
            level.addEntity(entity);entities.add(entity.getId());
        }
        for(var snapshot:snapshots)if(snapshot.vehicle()>=0){
            var entity=level.getEntity(snapshot.spawn().getId());var vehicle=level.getEntity(snapshot.vehicle());
            if(entity!=null&&vehicle!=null)entity.startRiding(vehicle,true,false);
        }
    }
    public int entityCount(){return entities.size();}
    public void expireEntities(){if(System.currentTimeMillis()-entityUpdate>2000)clearEntities();}
    private void clearEntities(){for(int id:entities)level.removeEntity(id,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);entities.clear();}
    private void sections(int x,int z,LightLayer layer,BitSet mask,BitSet empty,Iterator<byte[]> updates){
        var engine=level.getChunkSource().getLightEngine();
        for(int index=0;index<engine.getLightSectionCount();index++)
            if(mask.get(index)||empty.get(index))engine.queueSectionData(layer,SectionPos.of(x,engine.getMinLightSection()+index,z),mask.get(index)?new DataLayer(updates.next().clone()):new DataLayer());
    }
}
