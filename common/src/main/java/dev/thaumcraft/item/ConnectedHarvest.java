package dev.thaumcraft.item;

import dev.thaumcraft.network.EquipmentEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.lang.ref.WeakReference;
import java.util.*;

/** The original hidden chopper/tiller sequence, owned by a server operation and its tool. */
final class ConnectedHarvest {
    private static final Map<ServerLevel,List<Operation>> ACTIVE=new WeakHashMap<>();
    private ConnectedHarvest(){}
    static void start(ServerLevel level,ServerPlayer player,ItemStack tool,InteractionHand hand,BlockPos origin,BlockState state,boolean till){
        ACTIVE.computeIfAbsent(level,key->new ArrayList<>()).add(new Operation(player,tool,hand,origin,state,till));
    }
    static void tick(ServerLevel level){
        var operations=ACTIVE.get(level);if(operations==null)return;
        operations.removeIf(operation->!operation.tick(level));
        if(operations.isEmpty())ACTIVE.remove(level);
    }
    static boolean tillable(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);
        return (state.is(Blocks.DIRT)||state.is(Blocks.GRASS_BLOCK))&&level.getBlockState(pos.above()).isAir();
    }
    static boolean tillBlock(ServerLevel level,BlockPos pos){
        if(!level.setBlockAndUpdate(pos,Blocks.FARMLAND.defaultBlockState()))return false;
        var sound=Blocks.FARMLAND.defaultBlockState().getSoundType();
        level.playSound(null,pos,sound.getStepSound(),SoundSource.BLOCKS,sound.getVolume()/2,sound.getPitch()*.8f);
        EquipmentEffect.send(level,EquipmentEffect.TILL,Vec3.atLowerCornerOf(pos.above()));
        return true;
    }
    private static final class Node {
        final BlockPos pos;int delay;
        Node(BlockPos pos,int delay){this.pos=pos.immutable();this.delay=delay;}
    }
    private static final class Operation {
        final WeakReference<ServerPlayer> owner;
        final ItemStack tool;
        final InteractionHand hand;
        final BlockState state;
        final boolean till;
        final ArrayDeque<Node> nodes=new ArrayDeque<>();
        int tilled;
        Operation(ServerPlayer player,ItemStack tool,InteractionHand hand,BlockPos pos,BlockState state,boolean till){
            owner=new WeakReference<>(player);this.tool=tool;this.hand=hand;this.state=state;this.till=till;nodes.add(new Node(pos,0));
        }
        boolean tick(ServerLevel level){
            var player=owner.get();
            // Never resume with a replacement tool, after logout/death, or in another dimension.
            if(player==null||player.isRemoved()||!player.isAlive()||player.level()!=level||player.getItemInHand(hand)!=tool||tool.isEmpty()||!dev.thaumcraft.PortConfig.areaMining)return false;
            var children=new ArrayList<Node>();
            // Bound work per tick, not the completed operation. Unprocessed nodes remain queued.
            int count=Math.min(nodes.size(),256);
            for(int i=0;i<count&&!tool.isEmpty()&&(!till||tilled<32);i++){
                var node=nodes.removeFirst();
                if(!level.hasChunkAt(node.pos))continue;
                if(node.delay>0){node.delay--;nodes.addLast(node);continue;}
                boolean worked=false;
                search:for(int x=-1;x<=1;x++)for(int y=till?0:-1;y<=(till?0:1);y++)for(int z=-1;z<=1;z++){
                    BlockPos pos=node.pos.offset(x,y,z);
                    if(!level.hasChunkAt(pos)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,tool)||level.getBlockEntity(pos)!=null)continue;
                    if(till){
                        if(!tillable(level,pos))continue;
                        if(!tillBlock(level,pos))continue;
                        ArcanaItem.charge(tool,player,hand,1);tilled++;
                    }else{
                        if(!level.getBlockState(pos).equals(state))continue;
                        var nearby=new AABB(pos).inflate(1);
                        var existing=new HashSet<UUID>();
                        for(var drop:level.getEntitiesOfClass(ItemEntity.class,nearby))existing.add(drop.getUUID());
                        if(!ElementalTools.breakConnected(level,player,pos))continue;
                        // Native tools skip wear on zero-hardness crops; the original helper did not.
                        if(state.getDestroySpeed(level,pos)==0)ArcanaItem.charge(tool,player,hand,1);
                        for(var drop:level.getEntitiesOfClass(ItemEntity.class,nearby,e->!existing.contains(e.getUUID()))){
                            var collected=drop.getItem();
                            if(player.addItem(collected)||collected.isEmpty())drop.discard();
                            else{drop.setItem(collected);drop.setPos(player.position().add(0,1,0));drop.setNoPickUpDelay();}
                        }
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,0,0,0,0);
                        dev.thaumcraft.content.ModSounds.playAt(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,"swing",SoundSource.BLOCKS,.3f,1);
                        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.WOOD_STEP,SoundSource.BLOCKS,.5f,1);
                    }
                    children.add(new Node(pos,2));node.delay=till?6:4;worked=true;break search;
                }
                if(worked)nodes.addLast(node);
            }
            nodes.addAll(children);
            return !tool.isEmpty()&&!nodes.isEmpty()&&(!till||tilled<32);
        }
    }
}
