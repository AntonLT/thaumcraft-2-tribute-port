package dev.thaumcraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.SealLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;

import java.util.*;

public final class SealPortals extends SavedData {
    public record Node(GlobalPos location,String owner,int channel,int window) {
        public Node(GlobalPos location,String owner,int channel){this(location,owner,channel,0);}
        static final Codec<Node> CODEC=RecordCodecBuilder.create(i->i.group(GlobalPos.CODEC.fieldOf("location").forGetter(Node::location),Codec.STRING.fieldOf("owner").forGetter(Node::owner),Codec.INT.fieldOf("channel").forGetter(Node::channel),Codec.INT.optionalFieldOf("window",0).forGetter(Node::window)).apply(i,Node::new));
    }
    private static final Codec<SealPortals> CODEC=Node.CODEC.listOf().xmap(SealPortals::new,d->d.nodes);
    private static final SavedDataType<SealPortals> TYPE=new SavedDataType<>(Thaumcraft.id("seal_portals"),SealPortals::new,CODEC,null);
    private final List<Node> nodes;
    public SealPortals() {this(List.of());}
    private SealPortals(List<Node> nodes) {this.nodes=new ArrayList<>(nodes);}
    private static SealPortals get(ServerLevel level) {return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);}
    private static boolean traveler(net.minecraft.world.entity.Entity entity){
        return entity.isAlive()&&!entity.isSpectator()&&!(entity instanceof dev.thaumcraft.entity.ArcaneMote)&&!(entity instanceof dev.thaumcraft.entity.LightningEffect)
                &&!(entity instanceof dev.thaumcraft.entity.RelicProjectile relic&&relic.mode()==4);
    }
    /** Advances the previewed destination; false when the seal is not a portal. */
    public static boolean cycle(ServerLevel level,MachineBlockEntity seal) {
        if(!Arrays.equals(Arrays.copyOf(SealLogic.runes(seal),2),new int[]{0,1}))return false;
        var data=get(level);GlobalPos location=GlobalPos.of(level.dimension(),seal.getBlockPos());
        for(int i=0;i<data.nodes.size();i++){Node node=data.nodes.get(i);if(node.location().equals(location)){
            data.nodes.set(i,new Node(location,node.owner(),node.channel(),node.window()+1));data.setDirty();
            dev.thaumcraft.content.ModSounds.play(level,seal.getBlockPos(),"pclose",net.minecraft.sounds.SoundSource.BLOCKS,.2f,1+level.getRandom().nextFloat()*.2f);
            return true;
        }}
        return true;
    }
    public static void updateRunes(ServerLevel level,MachineBlockEntity seal){
        var data=get(level);GlobalPos location=GlobalPos.of(level.dimension(),seal.getBlockPos());
        int index=-1,window=0;for(int i=0;i<data.nodes.size();i++)if(data.nodes.get(i).location().equals(location)){index=i;window=data.nodes.get(i).window();break;}
        int[] runes=SealLogic.runes(seal);
        if(runes[0]==0&&runes[1]==1){
            Node node=new Node(location,seal.owner()==null?"":seal.owner().toString(),runes[2],window);
            if(index<0)data.nodes.add(node);else data.nodes.set(index,node);
        }else if(index>=0)data.nodes.remove(index);
        data.setDirty();
    }
    public static boolean tick(ServerLevel level,MachineBlockEntity seal,int channel) {
        var data=get(level);GlobalPos location=GlobalPos.of(level.dimension(),seal.getBlockPos());
        Node source=data.nodes.stream().filter(n->n.location().equals(location)).findFirst().orElse(null);
        if(source==null||source.channel()!=channel){
            source=new Node(location,seal.owner()==null?"":seal.owner().toString(),Math.clamp(channel,-1,5));
            data.nodes.removeIf(n->n.location().equals(location));data.nodes.add(source);data.setDirty();
        }
        // The original rune networks are public and dimension-local.
        var candidates=data.nodes.stream().filter(n->!n.location().equals(location)&&n.location().dimension().equals(level.dimension())&&n.channel()==channel).toList();
        if(source.window()!=0&&(source.window()<0||source.window()>=candidates.size())){
            Node reset=new Node(source.location(),source.owner(),source.channel(),0);
            data.nodes.set(data.nodes.indexOf(source),reset);source=reset;data.setDirty();
        }
        Node selected=null;MachineBlockEntity target=null;
        if(!candidates.isEmpty()){
            Node node=candidates.get(source.window());
            BlockPos pos=node.location().pos();
            if(level.getWorldBorder().isWithinBounds(pos)&&!level.isOutsideBuildHeight(pos)){
                level.getChunkAt(pos);
                if(level.getBlockEntity(pos) instanceof MachineBlockEntity other&&other.machineId().equals("arcane_seal")&&Arrays.equals(SealLogic.runes(other),new int[]{0,1,channel})){
                    if(other.enabled()){selected=node;target=other;}
                }else {data.nodes.remove(node);data.setDirty();}
            }
        }
        boolean wasOpen=seal.visualPortalOpen();
        boolean near=!level.getEntities((net.minecraft.world.entity.Entity)null,SealLogic.area(seal,1),SealPortals::traveler).isEmpty();
        if(selected==null||!near){
            seal.setVisualPortalTarget(null,net.minecraft.core.Direction.NORTH);
            if(wasOpen&&!near&&nearPlayer(level,seal,2))dev.thaumcraft.content.ModSounds.play(level,seal.getBlockPos(),"pclose",net.minecraft.sounds.SoundSource.BLOCKS,.4f,1+level.getRandom().nextFloat()*.2f);
            return false;
        }
        var facing=target.getBlockState().getValue(dev.thaumcraft.machine.MachineBlock.FACING);
        seal.setVisualPortalTarget(selected.location(),facing);
        if(!wasOpen&&nearPlayer(level,seal,1))dev.thaumcraft.content.ModSounds.play(level,seal.getBlockPos(),"popen",net.minecraft.sounds.SoundSource.BLOCKS,.4f,1+level.getRandom().nextFloat()*.2f);
        var entities=level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(seal.getBlockPos()),SealPortals::traveler);
        if(entities.isEmpty())return false;
        var entity=entities.getFirst();var root=entity.getRootVehicle();
        var travelers=new ArrayList<net.minecraft.world.entity.Entity>();travelers.add(root);root.getIndirectPassengers().forEach(travelers::add);
        var feet=landing(level,target,travelers);if(feet==null)return false;
        var from=seal.getBlockState().getValue(dev.thaumcraft.machine.MachineBlock.FACING);
        float yaw=facing.getAxis().isHorizontal()?facing.toYRot():root.getYRot();
        for(var traveler:travelers)dev.thaumcraft.network.SealEffect.send(level,dev.thaumcraft.network.SealEffect.POOF,traveler.position().add(-.5,-.5,-.5),traveler.position());
        // Teleporting the root carries its passengers as passengers; players keep their mount. Known movement is the client's real velocity.
        var motion=rotateMotion(root.getKnownMovement(),from,facing);
        if(root.teleport(new net.minecraft.world.level.portal.TeleportTransition(level,feet,motion,yaw,root.getXRot(),net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING))==null)return false;
        root.hurtMarked=true;
        for(var traveler:travelers){traveler.resetFallDistance();dev.thaumcraft.network.SealEffect.send(level,dev.thaumcraft.network.SealEffect.POOF,traveler.position().add(-.5,-.5,-.5),traveler.position());}
        target.progress=40;target.setChanged();
        int bad=entity instanceof net.minecraft.world.entity.item.ItemEntity?1:4;
        var aura=dev.thaumcraft.gameplay.ArcaneWorldData.get(level);aura.addVibes(level,seal.getBlockPos(),0,bad);aura.addVibes(level,target.getBlockPos(),0,bad);
        level.playSound(null,seal.getBlockPos(),net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        level.playSound(null,target.getBlockPos(),net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        return true;
    }
    /**
     * Feet position in front of a destination seal where every traveler fits, or null. The original landed half a block up
     * and let entities clip into ceilings; standing on the landing surface keeps ordinary two-high rooms usable.
     * Floor seals fall back to the seal's own block, where the original's arrivals ended up after dropping.
     */
    private static net.minecraft.world.phys.Vec3 landing(ServerLevel level,MachineBlockEntity seal,List<net.minecraft.world.entity.Entity> travelers){
        var facing=seal.getBlockState().getValue(dev.thaumcraft.machine.MachineBlock.FACING);
        BlockPos pos=seal.getBlockPos().relative(facing);
        if(facing.getAxis().isHorizontal()&&level.isEmptyBlock(pos.below()))pos=pos.below();
        for(BlockPos candidate:facing==net.minecraft.core.Direction.UP?List.of(pos,seal.getBlockPos()):List.of(pos)){
            var shape=level.getBlockState(candidate).getCollisionShape(level,candidate);
            double top=shape.isEmpty()?0:shape.max(net.minecraft.core.Direction.Axis.Y);
            if(top>.5)continue;
            var feet=new net.minecraft.world.phys.Vec3(candidate.getX()+.5,candidate.getY()+top,candidate.getZ()+.5);
            if(travelers.stream().allMatch(traveler->fits(level,traveler,feet)))return feet;
        }
        return null;
    }
    private static boolean fits(ServerLevel level,net.minecraft.world.entity.Entity traveler,net.minecraft.world.phys.Vec3 feet){
        var box=traveler.getBoundingBox().move(feet.x-traveler.getX(),feet.y-traveler.getY(),feet.z-traveler.getZ());
        return !level.isOutsideBuildHeight(BlockPos.containing(box.minX,box.minY,box.minZ))&&!level.isOutsideBuildHeight(BlockPos.containing(box.maxX,box.maxY,box.maxZ))
                &&level.getWorldBorder().isWithinBounds(box)&&level.noCollision(traveler,box);
    }
    private static boolean nearPlayer(ServerLevel level,MachineBlockEntity seal,int range){return !level.getEntitiesOfClass(ServerPlayer.class,SealLogic.area(seal,range),p->!p.isSpectator()).isEmpty();}
    private static net.minecraft.world.phys.Vec3 rotateMotion(net.minecraft.world.phys.Vec3 motion,net.minecraft.core.Direction from,net.minecraft.core.Direction to){
        int source=from.get3DDataValue(),target=to.get3DDataValue(),diff=source-target,sum=source+target;
        double x=motion.x,y=motion.y,z=motion.z;
        if(diff==-3||diff==2||diff==-1&&sum!=5&&sum!=9){x=motion.z;z=-motion.x;}
        else if(diff==-2||diff==3||diff==1&&sum!=5&&sum!=9){x=-motion.z;z=motion.x;}
        else if(diff==0){x=-x;z=-z;if(source<=1)y=-y;}
        return new net.minecraft.world.phys.Vec3(x,y,z);
    }
    /** The bracelet joins the same-dimension network selected by the third seal rune. */
    public static boolean travelByBracelet(ServerPlayer player,int rune) {
        ServerLevel level=player.level();var data=get(level);
        var candidates=data.nodes.stream().filter(n->n.channel()==rune&&n.location().dimension().equals(level.dimension())).toList();
        if(candidates.isEmpty())return false;
        BlockPos pos=candidates.get(level.getRandom().nextInt(candidates.size())).location().pos();
        if(!level.getWorldBorder().isWithinBounds(pos)||level.isOutsideBuildHeight(pos))return false;
        level.getChunkAt(pos);
        if(!(level.getBlockEntity(pos) instanceof MachineBlockEntity seal)||!seal.machineId().equals("arcane_seal"))return false;
        var facing=seal.getBlockState().getValue(dev.thaumcraft.machine.MachineBlock.FACING);
        var feet=landing(level,seal,List.of(player));if(feet==null)return false;
        var source=player.position();var motion=player.getKnownMovement();var fallDistance=player.fallDistance;
        float yaw=facing.getAxis().isHorizontal()?facing.toYRot():0;
        braceletEffect(level,source);
        if(!player.teleportTo(level,feet.x,feet.y,feet.z,Set.of(),yaw,player.getXRot(),true))return false;
        player.setDeltaMovement(0,motion.y,0);player.hurtMarked=true;player.fallDistance=fallDistance;
        seal.progress=40;seal.setChanged();
        var aura=dev.thaumcraft.gameplay.ArcaneWorldData.get(level);
        aura.addVibes(level,BlockPos.containing(source),0,25);aura.addVibes(level,pos,0,25);
        braceletEffect(level,player.position());
        return true;
    }
    private static void braceletEffect(ServerLevel level,net.minecraft.world.phys.Vec3 point){
        level.playSound(null,point.x,point.y,point.z,net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        dev.thaumcraft.network.SealEffect.send(level,dev.thaumcraft.network.SealEffect.POOF,point.add(-.5,-.5,-.5),point);
    }

    public static void remove(ServerLevel level,BlockPos pos) {var data=get(level);if(data.nodes.removeIf(n->n.location().equals(GlobalPos.of(level.dimension(),pos))))data.setDirty();}
}
