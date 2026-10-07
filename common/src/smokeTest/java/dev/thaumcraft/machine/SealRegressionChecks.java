package dev.thaumcraft.machine;

import com.mojang.authlib.GameProfile;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.entity.ArcaneMote;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.network.SealEffect;
import dev.thaumcraft.world.SealPortals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Real-world checks of seal behaviors that differed from the original TileSeal. */
public final class SealRegressionChecks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Seal regression: "+message);}
    private static ItemStack rune(int type){return new ItemStack(Content.item(Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemRunicEssence")&&e.meta()==type).findFirst().orElseThrow().id()));}
    private static MachineBlockEntity seal(ServerLevel level,BlockPos pos,Direction facing,int... runes){
        clear(level,pos,3);
        level.setBlockAndUpdate(pos.relative(facing.getOpposite()),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos,Content.block("arcane_seal").defaultBlockState().setValue(MachineBlock.FACING,facing));
        var seal=(MachineBlockEntity)level.getBlockEntity(pos);
        for(int i=0;i<runes.length;i++)if(runes[i]>=0)seal.setItem(18+i,rune(runes[i]));
        return seal;
    }
    private static void clear(ServerLevel level,BlockPos pos,int range){
        for(BlockPos p:BlockPos.betweenClosed(pos.offset(-range,-range,-range),pos.offset(range,range,range)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
        level.getEntities((Entity)null,new AABB(pos).inflate(range),e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
    }
    private static void seed(ServerLevel level,int bound){
        for(long seed=0;seed<10000;seed++){level.getRandom().setSeed(seed);if(level.getRandom().nextInt(bound)==0){level.getRandom().setSeed(seed);return;}}
        throw new AssertionError("No deterministic success seed");
    }
    private static ItemEntity item(ServerLevel level,Vec3 pos,ItemStack stack){var item=new ItemEntity(level,pos.x,pos.y,pos.z,stack);item.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(item);return item;}
    public static int run(MinecraftServer server){
        checks=0;var level=server.overworld();BlockPos pos=new BlockPos(720,280,720);level.getChunkAt(pos);
        for(BlockPos chunk:BlockPos.betweenClosed(pos.offset(-32,0,-32),pos.offset(64,0,32)))if((chunk.getX()&15)==0&&(chunk.getZ()&15)==0)level.getChunkAt(chunk);
        clear(level,pos,15);
        var aura=ArcaneWorldData.get(level);var boost=seal(level,pos,Direction.EAST,0);aura.changeBoost(level,pos,-100);aura.addVibes(level,pos,0,-100);
        SealLogic.tick(level,boost);check(boost.sealWorked,"Successful boost records work");
        check(boost.getBlockState().isRandomlyTicking(),"Seal block participates in random ticks");
        boost.getBlockState().randomTick(level,pos,level.getRandom());
        check(aura.aura(level,pos).badVibes()==1&&!boost.sealWorked,"Random tick charges one single-rune bad vibe and clears work");
        boost.getBlockState().randomTick(level,pos,level.getRandom());check(aura.aura(level,pos).badVibes()==1,"Idle random tick does not charge again");
        boost.setItem(19,rune(0));boost.setItem(20,rune(0));boost.progress=0;SealLogic.tick(level,boost);int before=aura.aura(level,pos).badVibes();SealLogic.randomTick(level,boost);
        check(aura.aura(level,pos).badVibes()>before&&aura.aura(level,pos).badVibes()<=before+3,"Three occupied runes charge one through three bad vibes");
        boost.progress=7;boost.setItem(18,ItemStack.EMPTY);SealLogic.tick(level,boost);check(boost.progress==6,"Inactive recipe still counts down its previous delay");

        var sensor=seal(level,pos,Direction.EAST,0,4,4);var target=item(level,pos.east(2).getCenter(),new ItemStack(Items.DIAMOND));
        SealLogic.tick(level,sensor);
        for(Direction face:Direction.values())check(sensor.getBlockState().getDirectSignal(level,pos,face)==15,"Detector strong output on "+face);
        check(level.getBestNeighborSignal(pos.west())==15,"Detector powers its supporting block");
        sensor.progress=40;var restored=(MachineBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,sensor.getBlockState(),sensor.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        check(restored.progress==0&&restored.energy()==15&&!restored.sealWorked,"Reload clears transient delay and work, keeping the saved signal for the first scan to change");
        sensor.progress=0;SealLogic.tick(level,sensor);check(sensor.energy()==15,"Continuous target preserves detector output");target.discard();sensor.progress=0;SealLogic.tick(level,sensor);check(sensor.energy()==0,"Detector clears when last target leaves");

        var player=new ServerPlayer(server,level,new GameProfile(UUID.fromString("4a5a4048-0011-4b0e-9303-c8393c245f47"),"SealChecks"),ClientInformation.createDefault());
        new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false));
        player.setPos(pos.east(2).getCenter());
        var editable=seal(level,pos,Direction.EAST);var held=rune(0);player.setItemInHand(InteractionHand.MAIN_HAND,held);
        var hit=new BlockHitResult(pos.getCenter(),Direction.EAST,pos,false);
        player.gameMode.useItemOn(player,level,held,InteractionHand.MAIN_HAND,hit);
        check(SealLogic.runes(editable)[0]==0&&editable.progress==60,"Actual rune insertion imposes sixty ticks");
        for(int i=0;i<60;i++)SealLogic.tick(level,editable);
        check(!editable.sealWorked&&editable.progress==0,"New rune waits the full sixty ticks before acting");
        editable.setItem(19,rune(0));editable.progress=0;
        var reversal=Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemWandReversal")).findFirst().orElseThrow();held=new ItemStack(Content.item(reversal.id()));player.setItemInHand(InteractionHand.MAIN_HAND,held);
        player.gameMode.useItemOn(player,level,held,InteractionHand.MAIN_HAND,hit);
        check(SealLogic.runes(editable)[1]==-1&&editable.progress==60,"Actual reversal removes the last rune and imposes sixty ticks");

        for(int[] runes:new int[][]{{5,0},{5,0,0}}){
            var nullifier=seal(level,pos,Direction.EAST,runes);var cell=aura.aura(level,pos);aura.changeAura(level,pos,.25f-cell.vis(),2-cell.taint());
            SealLogic.tick(level,nullifier);cell=aura.aura(level,pos);
            check(cell.vis()==0&&cell.taint()==1.75f,"Fractional nullification removes equal amounts for "+Arrays.toString(runes));
            check(!nullifier.sealWorked,"Nullification does not acquire an extra work charge");
        }
        for(Direction facing:Direction.values()){
            var targetPos=pos.relative(facing,2);var chunk=net.minecraft.world.level.ChunkPos.containing(targetPos);
            boolean forced=level.setChunkForced(chunk.x(),chunk.z(),true);
            try{
                // Loaded blocks alone do not make entities in an adjacent chunk queryable.
                server.managedBlock(()->{level.getChunkSource().pollTask();return level.isPositionEntityTicking(targetPos);});
                level.waitForEntities(chunk,0);
                var beam=seal(level,pos,facing,4,1);var mob=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);mob.setPos(targetPos.getCenter());level.addFreshEntity(mob);
                check(level.getEntitiesOfClass(Entity.class,mob.getBoundingBox()).contains(mob),"Beam target is queryable for "+facing);
                SealLogic.tick(level,beam);var motes=level.getEntitiesOfClass(ArcaneMote.class,new AABB(pos).inflate(8));
                Vec3 expected=pos.getCenter().subtract(new Vec3(facing.getStepX(),facing.getStepY(),facing.getStepZ()).scale(.5));
                check(motes.size()==1&&motes.getFirst().position().equals(expected),"Beam starts at supporting face for "+facing);motes.forEach(Entity::discard);mob.discard();
            }finally{if(forced)level.setChunkForced(chunk.x(),chunk.z(),false);}
        }
        var wind=seal(level,pos,Direction.EAST,1,-1,0);target=item(level,pos.east(2).getCenter(),new ItemStack(Items.DIAMOND));
        ArcaneMote.wind(level,pos.getCenter(),pos.east(3).getCenter(),false);var mote=level.getEntitiesOfClass(ArcaneMote.class,new AABB(pos).inflate(8)).getFirst();Vec3 motion=mote.getDeltaMovement();
        SealLogic.tick(level,wind);check(target.getDeltaMovement().x>0,"Third magic rune after a hole does not suppress base wind item targets");check(mote.getDeltaMovement().equals(motion),"Wind does not push modern visual entities");target.discard();mote.discard();
        var gapFire=seal(level,pos,Direction.EAST,4,-1,0);var cow=EntityType.COW.create(level,EntitySpawnReason.COMMAND);cow.setPos(pos.east(2).getCenter());level.addFreshEntity(cow);SealLogic.tick(level,gapFire);
        check(!level.getEntitiesOfClass(ArcaneMote.class,new AABB(pos).inflate(8)).isEmpty(),"Base fire retains animal targets through an empty second slot");cow.discard();
        check(SealLogic.cooldown(new int[]{3,-1,1},0)==20,"Base till ignores an air modifier beyond a hole");
        check(SealLogic.description("5,3,-1").contains("suppresses")&&SealLogic.description("5,3,3").contains("12"),"Undocumented working suppressors have accurate runtime descriptions");

        var hydrate=seal(level,pos,Direction.EAST,2,-1,3);BlockPos outside=pos.east(2).above(4);level.setBlockAndUpdate(outside,Blocks.FARMLAND.defaultBlockState());seed(level,10);SealLogic.tick(level,hydrate);
        check(level.getBlockState(outside).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.MOISTURE)==0,"Base hydration does not expand through an empty rune slot");level.setBlockAndUpdate(outside,Blocks.AIR.defaultBlockState());
        BlockPos farm=pos.east(2).below(2);level.setBlockAndUpdate(farm,Blocks.FARMLAND.defaultBlockState());hydrate.progress=0;seed(level,10);SealLogic.tick(level,hydrate);
        check(level.getBlockState(farm).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.MOISTURE)==7,"Hydration samples chance at the eligible candidate");
        var grow=seal(level,pos,Direction.EAST,2,3);BlockPos cane=pos.east(2).below(2);level.setBlockAndUpdate(cane.below(),Blocks.SAND.defaultBlockState());level.setBlockAndUpdate(cane.below().north(),Blocks.WATER.defaultBlockState());level.setBlockAndUpdate(cane,Blocks.SUGAR_CANE.defaultBlockState());level.setBlockAndUpdate(cane.above(),Blocks.GLASS.defaultBlockState());seed(level,20);SealLogic.tick(level,grow);
        check(level.getBlockState(cane).is(Blocks.SUGAR_CANE)&&level.getBlockState(cane).getValue(SugarCaneBlock.AGE)==1,"Cane age can advance beneath an unrelated block");

        var collector=seal(level,pos,Direction.EAST,1,5,5);BlockPos barrelPos=pos.above(2);level.setBlockAndUpdate(barrelPos,Blocks.BARREL.defaultBlockState());var barrel=(net.minecraft.world.Container)level.getBlockEntity(barrelPos);
        for(int i=0;i<barrel.getContainerSize();i++)barrel.setItem(i,new ItemStack(Items.STONE,64));barrel.setItem(0,new ItemStack(Items.DIAMOND,60));barrel.setItem(1,new ItemStack(Items.DIAMOND,60));
        target=item(level,pos.getCenter(),new ItemStack(Items.DIAMOND,8));SealLogic.tick(level,collector);
        check(target.isAlive()&&barrel.getItem(0).getCount()==60&&barrel.getItem(1).getCount()==60,"Collector does not split a whole stack across two slots");
        barrel.setItem(0,new ItemStack(Items.DIAMOND,56));collector.progress=0;SealLogic.tick(level,collector);
        check(target.isRemoved()&&barrel.getItem(0).getCount()==64,"Collector deposits the complete stack in one fitting slot");

        checkPortals(level,pos.offset(32,0,0));
        checkArrivals(level,player,pos.offset(32,0,16),pos.offset(48,0,16));
        checkReviewFixes(level,pos);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        var effect=new SealEffect(SealEffect.HEAL,pos.getCenter(),pos.east(5).getCenter());SealEffect.CODEC.encode(buffer,effect);check(effect.equals(SealEffect.CODEC.decode(buffer)),"Seal particle packet preserves kind, emission and target");buffer.release();
        clear(level,pos,15);clear(level,pos.offset(32,0,0),4);clear(level,pos.offset(48,0,0),4);clear(level,pos.offset(64,0,0),4);
        return checks;
    }
    /** A player-sized traveler arrives in ordinary two-high rooms behind every seal orientation. */
    private static void checkArrivals(ServerLevel level,ServerPlayer player,BlockPos sourcePos,BlockPos targetPos){
        // The fixture connection has no network channel; NeoForge rejects packets sent to it.
        var sender=SealEffect.sender;SealEffect.sender=(recipient,effect)->{};level.addNewPlayer(player);
        try{
            for(Direction to:List.of(Direction.EAST,Direction.DOWN,Direction.UP)){
                var source=seal(level,sourcePos,Direction.UP,0,1,3);var target=seal(level,targetPos,to,0,1,3);
                SealPortals.updateRunes(level,source);SealPortals.updateRunes(level,target);
                BlockPos room=to==Direction.EAST?targetPos.east():to==Direction.DOWN?targetPos.below():targetPos;
                level.setBlockAndUpdate(room.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(room.above(2),Blocks.STONE.defaultBlockState());
                Vec3 expected=to==Direction.UP?targetPos.getBottomCenter().add(0,1/16d,0):room.getBottomCenter();
                player.teleportTo(sourcePos.getX()+.5,sourcePos.getY()+1/16d,sourcePos.getZ()+.5);
                check(SealPortals.tick(level,source,3)&&player.position().distanceTo(expected)<1e-6,"Player arrives in a two-high room behind a "+to+" seal");
            }
        }finally{
            SealEffect.sender=sender;player.discard();SealPortals.remove(level,sourcePos);SealPortals.remove(level,targetPos);clear(level,sourcePos,3);clear(level,targetPos,3);
        }
    }
    private static void checkReviewFixes(ServerLevel level,BlockPos pos){
        var frame=new net.minecraft.world.entity.decoration.ItemFrame(level,pos.east(2),Direction.EAST);
        var wind=seal(level,pos,Direction.EAST,1);level.addFreshEntity(frame);var pushed=item(level,pos.east(2).getCenter(),new ItemStack(Items.DIAMOND));
        SealLogic.tick(level,wind);check(frame.isAlive()&&pushed.getDeltaMovement().x>0,"Wind pushes items but leaves item frames hanging");frame.discard();pushed.discard();

        var harvester=seal(level,pos,Direction.EAST,3,2);BlockPos flower=pos.east(2),leaves=pos.east(2).north();
        level.setBlockAndUpdate(flower.below(),Blocks.DIRT.defaultBlockState());level.setBlockAndUpdate(flower,Blocks.POPPY.defaultBlockState());
        level.setBlockAndUpdate(leaves,Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true));
        for(int i=0;i<100&&level.getBlockState(flower).is(Blocks.POPPY);i++){harvester.progress=0;SealLogic.tick(level,harvester);}
        check(!level.getBlockState(flower).is(Blocks.POPPY)&&level.getBlockState(leaves).is(Blocks.CHERRY_LEAVES),"Harvester picks small flowers but spares cherry leaves");

        var grower=seal(level,pos,Direction.EAST,2,3);BlockPos stem=pos.east(3);
        for(BlockPos soil:List.of(stem,stem.north(),stem.south(),stem.east(),stem.west()))level.setBlockAndUpdate(soil.below(),Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(stem,Blocks.MELON_STEM.defaultBlockState().setValue(StemBlock.AGE,7));seed(level,75);SealLogic.tick(level,grower);
        var attached=level.getBlockState(stem);
        check(attached.is(Blocks.ATTACHED_MELON_STEM)&&level.getBlockState(stem.relative(attached.getValue(HorizontalDirectionalBlock.FACING))).is(Blocks.MELON),"Seal-grown melon attaches its stem");

        var broken=seal(level,pos,Direction.EAST,0,1,2);level.destroyBlock(pos,true);
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
        check(drops.size()==1&&drops.getFirst().getItem().is(Content.item("arcane_seal_item")),"Broken seal drops the craftable seal and loses its runes");drops.forEach(Entity::discard);

        var anchored=net.minecraft.world.level.ChunkPos.containing(pos.offset(-512,0,0));var neighbour=new net.minecraft.world.level.ChunkPos(anchored.x()+1,anchored.z()+1);
        dev.thaumcraft.world.ChunkAnchors.ticket(level,anchored,1);
        check(!level.shouldTickBlocksAt(neighbour.pack()),"Anchor fixture starts outside ticking range");
        long deadline=System.nanoTime()+30_000_000_000L;
        level.getServer().managedBlock(()->{level.getChunkSource().pollTask();return System.nanoTime()>deadline||level.shouldTickBlocksAt(neighbour.pack());});
        check(level.shouldTickBlocksAt(neighbour.pack()),"A 3x3 anchor keeps its corner chunks block-ticking");
    }
    private static void checkPortals(ServerLevel level,BlockPos sourcePos){
        BlockPos targetPos=sourcePos.east(16);
        // Operations recovered from all 36 orientation pairs: identity, clockwise, counterclockwise, reverse XZ, reverse XYZ.
        int[][] transforms={{4,1,2,1,0,0},{2,4,1,2,1,0},{1,2,3,0,2,1},{2,1,0,3,1,2},{0,2,1,2,3,0},{0,0,2,1,0,3}};
        Vec3[] expected={new Vec3(1,2,3),new Vec3(3,2,-1),new Vec3(-3,2,1),new Vec3(-1,2,-3),new Vec3(-1,-2,-3)};
        for(Direction from:Direction.values())for(Direction to:Direction.values()){
            var source=seal(level,sourcePos,from,0,1,5);var target=seal(level,targetPos,to,0,1,5);SealPortals.updateRunes(level,source);SealPortals.updateRunes(level,target);
            var freight=item(level,sourcePos.getCenter(),new ItemStack(Items.DIAMOND));freight.setDeltaMovement(1,2,3);
            check(SealPortals.tick(level,source,5),"Portal travels for "+from+" to "+to);
            check(freight.getDeltaMovement().equals(expected[transforms[from.get3DDataValue()][to.get3DDataValue()]]),"Original motion for "+from+" to "+to);
            check(target.progress==40,"Destination receives travel cooldown");freight.discard();
        }
        var source=seal(level,sourcePos,Direction.EAST,0,1,5);var target=seal(level,targetPos,Direction.EAST,0,1,5);var third=seal(level,targetPos.east(16),Direction.EAST,0,1,5);
        SealPortals.updateRunes(level,source);SealPortals.updateRunes(level,target);SealPortals.updateRunes(level,third);
        var freight=item(level,sourcePos.getCenter(),new ItemStack(Items.DIAMOND));
        for(int i=0;i<3;i++)SealPortals.cycle(level,source);
        check(SealPortals.tick(level,source,5)&&freight.distanceToSqr(targetPos.getCenter())<8,"Out-of-range window resets to first destination instead of modulo-wrapping");freight.discard();
        freight=item(level,sourcePos.getCenter(),new ItemStack(Items.DIAMOND));target.toggle();
        check(!SealPortals.tick(level,source,5)&&freight.distanceToSqr(sourcePos.getCenter())<1,"Disabled selected portal does not silently redirect to another destination");target.toggle();
        level.setBlockAndUpdate(targetPos.east(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(targetPos.east().below(),Blocks.STONE.defaultBlockState());
        check(!SealPortals.tick(level,source,5)&&freight.isAlive(),"Obstructed portal still refuses unsafe travel");freight.discard();
        SealPortals.remove(level,sourcePos);SealPortals.remove(level,targetPos);SealPortals.remove(level,targetPos.east(16));
    }
}
