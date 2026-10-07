package dev.thaumcraft.item;

import dev.thaumcraft.content.ModSounds;
import dev.thaumcraft.gameplay.ArcaneEnchantments;
import dev.thaumcraft.network.EquipmentEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import java.lang.ref.WeakReference;
import java.util.*;

/** Original hidden-trader propagation, with each player's selection shared across their wands. */
public final class EqualTrade {
    private static final Map<net.minecraft.server.MinecraftServer,Map<UUID,BlockState>> SELECTED=new WeakHashMap<>();
    private static final Map<ServerLevel,List<Operation>> ACTIVE=new WeakHashMap<>();
    private static final int[] OFFSETS={0,1,-1};
    private EqualTrade(){}
    public static InteractionResult use(ServerLevel level,Player player,UseOnContext context){
        var pos=context.getClickedPos();var state=level.getBlockState(pos);
        if(state.getDestroySpeed(level,pos)<0)return InteractionResult.FAIL;
        var selections=SELECTED.computeIfAbsent(level.getServer(),server->new HashMap<>());
        if(player.isShiftKeyDown()){
            selections.put(player.getUUID(),state);
            player.sendSystemMessage(Component.translatableWithFallback("message.thaumcraft2tp.trade.target", "Target block set to %s", state.getBlock().getName()));
            level.playSound(null,pos,SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.BLOCKS,.5f,1);
            return InteractionResult.SUCCESS;
        }
        var replacement=selections.get(player.getUUID());
        // Migrate selections saved by earlier port builds; a new selection always takes precedence.
        if(replacement==null){
            String saved=ItemState.getString(context.getItemInHand(),"trade_state","");
            if(!saved.isEmpty())try{
                replacement=BlockState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(saved)).result().orElse(null);
                if(replacement!=null)selections.put(player.getUUID(),replacement);
            }catch(com.google.gson.JsonParseException ignored){}
        }
        if(replacement==null||replacement.isAir()||replacement.getBlock().asItem()==net.minecraft.world.item.Items.AIR)return InteractionResult.FAIL;
        if(state.equals(replacement))return InteractionResult.PASS;
        ACTIVE.computeIfAbsent(level,key->new ArrayList<>()).add(new Operation(player,context,state,replacement));
        return InteractionResult.SUCCESS;
    }
    public static void tick(ServerLevel level){
        var operations=ACTIVE.get(level);if(operations==null)return;
        operations.removeIf(operation->!operation.tick(level));
        if(operations.isEmpty())ACTIVE.remove(level);
    }
    private static final class Node {
        final BlockPos pos;int delay;
        Node(BlockPos pos,int delay){this.pos=pos.immutable();this.delay=delay;}
    }
    private static final class Operation {
        final WeakReference<Player> owner;
        final ItemStack tool;
        final InteractionHand hand;
        final Direction face;
        final BlockState original,replacement;
        final int limit,slot;
        final ArrayDeque<Node> nodes=new ArrayDeque<>();
        int replaced;
        Operation(Player player,UseOnContext context,BlockState original,BlockState replacement){
            owner=new WeakReference<>(player);tool=context.getItemInHand();hand=context.getHand();face=context.getClickedFace();
            slot=hand==InteractionHand.MAIN_HAND?player.getInventory().getSelectedSlot():-1;
            this.original=original;this.replacement=replacement;
            limit=16+8*ArcaneEnchantments.level((ServerLevel)player.level(),tool,"potency");
            nodes.add(new Node(context.getClickedPos(),0));
        }
        boolean tick(ServerLevel level){
            var player=owner.get();
            if(player==null||player.isRemoved()||!player.isAlive()||player.level()!=level||(slot<0?player.getOffhandItem():player.getInventory().getItem(slot))!=tool||tool.isEmpty())return false;
            var children=new ArrayList<Node>();int count=nodes.size();
            for(int i=0;i<count&&replaced<limit&&!tool.isEmpty();i++){
                var node=nodes.removeFirst();
                if(node.delay>0){node.delay--;nodes.addLast(node);continue;}
                boolean worked=false;
                search:for(int x:OFFSETS)for(int y:OFFSETS)for(int z:OFFSETS){
                    var pos=node.pos.offset(x,y,z);
                    if(!level.hasChunkAt(pos)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,face,tool))continue;
                    var state=level.getBlockState(pos);
                    if(!state.equals(original)||state.getDestroySpeed(level,pos)<0)continue;
                    if(!dev.thaumcraft.api.ThaumcraftEvents.removalAllowed(level,pos,state,player.getUUID()))continue;
                    if(Arrays.stream(Direction.values()).noneMatch(d->!level.getBlockState(pos.relative(d)).isSolidRender()))continue;
                    // Respect machine ownership and let native block removal preserve container contents.
                    var blockEntity=level.getBlockEntity(pos);
                    if(blockEntity instanceof dev.thaumcraft.machine.MachineBlockEntity machine&&machine.owner()!=null&&!machine.owner().equals(player.getUUID()))continue;
                    // A double slab state is two slab items; payment may span stacks but must be complete before mutation.
                    var material=replacement.getBlock().asItem();
                    int cost=replacement.getOptionalValue(SlabBlock.TYPE).orElse(null)==SlabType.DOUBLE?2:1;
                    if(!player.isCreative()&&player.getInventory().countItem(material)<cost)return false;
                    var drops=Block.getDrops(state,level,pos,blockEntity,player,tool);
                    if(!level.setBlockAndUpdate(pos,replacement))continue;
                    if(level.getBlockEntity(pos) instanceof dev.thaumcraft.machine.MachineBlockEntity machine)machine.setOwner(player.getUUID());
                    if(!player.isCreative())for(int slot=0;slot<player.getInventory().getContainerSize()&&cost>0;slot++){
                        var candidate=player.getInventory().getItem(slot);
                        if(!candidate.is(material))continue;
                        int paid=Math.min(cost,candidate.getCount());candidate.shrink(paid);cost-=paid;
                    }
                    for(var drop:drops)if(!player.addItem(drop))player.drop(drop,false);
                    ArcanaItem.charge(tool,player,hand,1);replaced++;
                    EquipmentEffect.send(level,EquipmentEffect.TRADE,Vec3.atLowerCornerOf(pos));
                    ModSounds.playAt(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,"zap",SoundSource.BLOCKS,.25f,1);
                    children.add(new Node(pos,2));node.delay=4;worked=true;break search;
                }
                if(worked)nodes.addLast(node);
            }
            nodes.addAll(children);
            return !tool.isEmpty()&&replaced<limit&&!nodes.isEmpty();
        }
    }
}
