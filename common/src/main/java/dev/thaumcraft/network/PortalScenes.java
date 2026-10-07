package dev.thaumcraft.network;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.Consumer;

/** Authorized, bounded snapshots for a nearby seal's actual remote destination. */
public final class PortalScenes {
    public static final int MAX_RADIUS=4;
    /** Destination chunks one job may be loading at once. */
    private static final int MAX_LOADING=8;
    public static Consumer<Request> requestSender=request->{};
    public static Consumer<Snapshot> receiver=snapshot->{};
    public static Consumer<Entities> entityReceiver=snapshot->{};
    private static final Map<ServerPlayer,Long> LAST_REQUEST=new WeakHashMap<>();
    public record Request(BlockPos source,long token,int radius) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(Thaumcraft.id("portal_scene_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=new StreamCodec<>() {
            public Request decode(RegistryFriendlyByteBuf b){return new Request(b.readBlockPos(),b.readLong(),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Request p){b.writeBlockPos(p.source);b.writeLong(p.token);b.writeVarInt(p.radius);}
        };
        public Type<Request> type(){return TYPE;}
    }
    public record Snapshot(BlockPos source,GlobalPos target,long token,ClientboundLevelChunkWithLightPacket chunk) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=new Type<>(Thaumcraft.id("portal_scene_chunk"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=new StreamCodec<>() {
            public Snapshot decode(RegistryFriendlyByteBuf b){return new Snapshot(b.readBlockPos(),GlobalPos.STREAM_CODEC.decode(b),b.readLong(),ClientboundLevelChunkWithLightPacket.STREAM_CODEC.decode(b));}
            public void encode(RegistryFriendlyByteBuf b,Snapshot p){b.writeBlockPos(p.source);GlobalPos.STREAM_CODEC.encode(b,p.target);b.writeLong(p.token);ClientboundLevelChunkWithLightPacket.STREAM_CODEC.encode(b,p.chunk);}
        };
        public Type<Snapshot> type(){return TYPE;}
    }
    public record EntityView(net.minecraft.network.protocol.game.ClientboundAddEntityPacket spawn,
            net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket data,
            net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket equipment,int ticks,int hurt,int death,float bodyYaw,int vehicle) {
        static final StreamCodec<RegistryFriendlyByteBuf,EntityView> CODEC=new StreamCodec<>() {
            public EntityView decode(RegistryFriendlyByteBuf b){
                var spawn=net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.decode(b);
                var data=net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(b);
                var equipment=b.readBoolean()?net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket.STREAM_CODEC.decode(b):null;
                return new EntityView(spawn,data,equipment,b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readFloat(),b.readVarInt());
            }
            public void encode(RegistryFriendlyByteBuf b,EntityView e){
                net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.encode(b,e.spawn);
                net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(b,e.data);
                b.writeBoolean(e.equipment!=null);if(e.equipment!=null)net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket.STREAM_CODEC.encode(b,e.equipment);
                b.writeVarInt(e.ticks);b.writeVarInt(e.hurt);b.writeVarInt(e.death);b.writeFloat(e.bodyYaw);b.writeVarInt(e.vehicle);
            }
        };
    }
    public record Entities(BlockPos source,GlobalPos target,long token,List<EntityView> entities) implements CustomPacketPayload {
        public static final Type<Entities> TYPE=new Type<>(Thaumcraft.id("portal_scene_entities"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Entities> CODEC=new StreamCodec<>() {
            public Entities decode(RegistryFriendlyByteBuf b){
                var source=b.readBlockPos();var target=GlobalPos.STREAM_CODEC.decode(b);long token=b.readLong();int count=b.readVarInt();
                if(count<0||count>128)throw new IllegalArgumentException("Portal entity limit");
                var entries=new ArrayList<EntityView>(count);for(int i=0;i<count;i++)entries.add(EntityView.CODEC.decode(b));return new Entities(source,target,token,entries);
            }
            public void encode(RegistryFriendlyByteBuf b,Entities p){b.writeBlockPos(p.source);GlobalPos.STREAM_CODEC.encode(b,p.target);b.writeLong(p.token);b.writeVarInt(p.entities.size());for(var entry:p.entities)EntityView.CODEC.encode(b,entry);}
        };
        public Type<Entities> type(){return TYPE;}
    }
    public static List<EntityView> entities(net.minecraft.server.level.ServerLevel level,GlobalPos target,int radius){
        var center=target.pos().getCenter();var result=new ArrayList<EntityView>();
        var visible=level.getEntities((net.minecraft.world.entity.Entity)null,new net.minecraft.world.phys.AABB(target.pos()).inflate(radius*16,64,radius*16),entity->!entity.isRemoved()&&!entity.isSpectator());
        visible.sort(Comparator.comparingDouble(entity->entity.distanceToSqr(center)));
        for(var entity:visible){
            if(result.size()>=128)break;
            var tracker=new net.minecraft.server.level.ServerEntity(level,entity,1,false,null);
            if(!(entity.getAddEntityPacket(tracker) instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket spawn))continue;
            var values=entity.getEntityData().getNonDefaultValues();
            var data=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(entity.getId(),values==null?List.of():values);
            net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket equipment=null;int hurt=0,death=0;float yaw=entity.getYRot();
            if(entity instanceof net.minecraft.world.entity.LivingEntity living){
                var slots=new ArrayList<com.mojang.datafixers.util.Pair<net.minecraft.world.entity.EquipmentSlot,net.minecraft.world.item.ItemStack>>();
                for(var slot:net.minecraft.world.entity.EquipmentSlot.values())slots.add(com.mojang.datafixers.util.Pair.of(slot,living.getItemBySlot(slot).copy()));
                equipment=new net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket(entity.getId(),slots);hurt=living.hurtTime;death=living.deathTime;yaw=living.yBodyRot;
            }
            result.add(new EntityView(spawn,data,equipment,entity.tickCount,hurt,death,yaw,entity.getVehicle()==null?-1:entity.getVehicle().getId()));
        }
        return result;
    }
    public static GlobalPos authorizedTarget(ServerPlayer player,BlockPos source){
        var level=player.level();
        if(!player.isAlive()||player.distanceToSqr(source.getX()+.5,source.getY()+.5,source.getZ()+.5)>64*64||!level.hasChunkAt(source))return null;
        if(!(level.getBlockEntity(source) instanceof MachineBlockEntity seal)||!seal.machineId().equals("arcane_seal")||!seal.enabled()||!seal.visualPortalOpen())return null;
        var target=seal.visualPortalTarget();
        return target!=null&&target.dimension().equals(level.dimension())&&level.getWorldBorder().isWithinBounds(target.pos())?target:null;
    }
    public static void handle(ServerPlayer player,Request request,Consumer<CustomPacketPayload> send){
        var target=authorizedTarget(player,request.source());if(target==null)return;
        long now=player.level().getGameTime();Long previous=LAST_REQUEST.get(player);
        if(previous!=null&&now-previous<20)return;
        LAST_REQUEST.put(player,now);
        int radius=Math.clamp(request.radius(),2,MAX_RADIUS);
        var positions=new ArrayDeque<net.minecraft.world.level.ChunkPos>();int cx=target.pos().getX()>>4,cz=target.pos().getZ()>>4;
        for(int ring=0;ring<=radius;ring++)for(int dx=-ring;dx<=ring;dx++)for(int dz=-ring;dz<=ring;dz++)
            if(Math.max(Math.abs(dx),Math.abs(dz))==ring)positions.add(new net.minecraft.world.level.ChunkPos(cx+dx,cz+dz));
        var jobs=JOBS.computeIfAbsent(player,key->new LinkedHashMap<>());
        jobs.remove(request.source);if(jobs.size()>=4)jobs.remove(jobs.keySet().iterator().next());
        jobs.put(request.source,new Job(request,target,positions,send,now));
    }
    private record Job(Request request,GlobalPos target,ArrayDeque<net.minecraft.world.level.ChunkPos> positions,Consumer<CustomPacketPayload> send,long created){}
    private static final Map<ServerPlayer,LinkedHashMap<BlockPos,Job>> JOBS=new WeakHashMap<>();
    public static void tick(net.minecraft.server.level.ServerLevel level){
        var players=JOBS.entrySet().iterator();
        while(players.hasNext()){
            var entry=players.next();var player=entry.getKey();
            if(player.hasDisconnected()){players.remove();continue;}
            if(player.level()!=level)continue;
            var jobs=entry.getValue().values().iterator();int budget=4;
            while(jobs.hasNext()){
                var job=jobs.next();
                if(level.getGameTime()-job.created>240||!job.target.equals(authorizedTarget(player,job.request.source))){jobs.remove();continue;}
                // Destination chunks load in the background instead of blocking the server tick. Each pending chunk keeps
                // a short loading ticket until it is sent, so it stays resident no longer than a blocking load did.
                var chunks=level.getChunkSource();int loading=0;
                for(var pending=job.positions.iterator();pending.hasNext()&&budget>0;){
                    var pos=pending.next();
                    if(!level.getWorldBorder().isWithinBounds(new BlockPos(pos.x()*16,job.target.pos().getY(),pos.z()*16))){pending.remove();budget--;continue;}
                    var chunk=chunks.getChunkNow(pos.x(),pos.z());
                    if(chunk==null){
                        if(loading++<MAX_LOADING)chunks.addTicketWithRadius(Thaumcraft.PORTAL_VIEW_TICKET,pos,0);
                        continue;
                    }
                    pending.remove();budget--;
                    job.send.accept(new Snapshot(job.request.source,job.target,job.request.token,new ClientboundLevelChunkWithLightPacket(chunk,chunks.getLightEngine(),null,null)));
                }
                if(job.positions.isEmpty()&&level.getGameTime()%10==0)job.send.accept(new Entities(job.request.source,job.target,job.request.token,entities(level,job.target,Math.clamp(job.request.radius,2,MAX_RADIUS))));
            }
            if(entry.getValue().isEmpty())players.remove();
        }
    }

    private PortalScenes(){}
}
