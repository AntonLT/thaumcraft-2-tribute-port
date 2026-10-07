package dev.thaumcraft.test;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.GameData;
import dev.thaumcraft.machine.MachineBlock;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Literal TileBore expectations, exercised through the production tick. */
public final class BoreParitySmokeTests {
    private static int checks;
    private static final java.util.Set<net.minecraft.world.level.ChunkPos> loaded=new java.util.HashSet<>();
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Bore parity: "+message);}
    static int run(MinecraftServer server){
        checks=0;loaded.clear();ServerLevel level=server.overworld();BlockPos pos=new BlockPos(752,280,752);
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try{
            for(int focus=0;focus<5;focus++)for(int kind=0;kind<3;kind++)for(Direction facing:Direction.values()){
                var effect=new dev.thaumcraft.network.BoreEffect(kind,focus,pos.relative(facing).getCenter(),pos.relative(facing,5).getCenter());
                buffer.clear();dev.thaumcraft.network.BoreEffect.CODEC.encode(buffer,effect);
                check(effect.equals(dev.thaumcraft.network.BoreEffect.CODEC.decode(buffer)),"Effect packet preserves focus, kind and directed endpoints");
            }
        }finally{buffer.release();}
        level.getChunkAt(pos);
        level.setBlockAndUpdate(pos,Content.block("arcane_bore").defaultBlockState().setValue(MachineBlock.FACING,Direction.EAST));
        var bore=(MachineBlockEntity)level.getBlockEntity(pos);
        for(Direction side:Direction.values()){
            BlockPos leverPos=pos.relative(side);
            var lever=Blocks.LEVER.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LeverBlock.FACE,side==Direction.UP?net.minecraft.world.level.block.state.properties.AttachFace.FLOOR:side==Direction.DOWN?net.minecraft.world.level.block.state.properties.AttachFace.CEILING:net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                    .setValue(net.minecraft.world.level.block.LeverBlock.FACING,side.getAxis().isVertical()?Direction.NORTH:side);
            check(lever.canSurvive(level,leverPos),"Lever attaches to bore face "+side);
            level.setBlockAndUpdate(leverPos,lever.setValue(net.minecraft.world.level.block.LeverBlock.POWERED,true));
            bore.setItem(0,new ItemStack(Content.item("arcane_focus")));bore.setItem(1,new ItemStack(Content.item("arcane_singularity")));bore.restoreEnergy(0);
            MachineLogic.bore(level,bore);
            check(bore.energy()==250,"Attached lever powers the bore from "+side);
            level.setBlockAndUpdate(leverPos,Blocks.AIR.defaultBlockState());
        }
        bore.setItem(0,ItemStack.EMPTY);bore.restoreEnergy(0);
        level.setBlockAndUpdate(pos.below(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        String[] foci={"arcane_focus","arcane_focus_air","arcane_focus_water","arcane_focus_earth","arcane_focus_fire"};
        for(int type=0;type<foci.length;type++){
            var focus=new ItemStack(Content.item(foci[type]));int maximum=type==0?2500:2000,interval=type==1?3:5;
            check(focus.getMaxDamage()==maximum,"Original durability for "+foci[type]);
            bore.setItem(0,focus);check(bore.visualFocus()==type,"Distinct displayed focus type "+type);
            focus.setDamageValue(maximum-1);bore.restoreEnergy(100);bore.progress=interval-1;
            wall(level,pos,Blocks.STONE.defaultBlockState());MachineLogic.bore(level,bore);
            check(!bore.getItem(0).isEmpty()&&focus.getDamageValue()==maximum,"Focus survives at exactly maximum damage "+type);
            check(bore.energy()==99&&bore.workRequired==interval,"One fuel unit per original cycle "+type);
            bore.progress=interval-1;wall(level,pos,Blocks.STONE.defaultBlockState());MachineLogic.bore(level,bore);
            check(bore.getItem(0).isEmpty(),"Focus breaks on the maximum-plus-one successful block "+type);
            clearItems(level,pos);
        }
        wall(level,pos,Blocks.AIR.defaultBlockState());
        var focus=new ItemStack(Content.item("arcane_focus"));bore.setItem(0,focus);
        bore.restoreEnergy(0);bore.progress=0;bore.setItem(1,new ItemStack(Content.item("arcane_singularity"),2));
        level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());
        for(int i=0;i<4;i++)MachineLogic.bore(level,bore);
        check(bore.progress==4&&bore.energy()==0&&bore.getItem(1).getCount()==2,"Idle timer advances without burning fuel");
        level.setBlockAndUpdate(pos.below(),Blocks.REDSTONE_BLOCK.defaultBlockState());
        MachineLogic.bore(level,bore);
        check(bore.progress==0&&bore.energy()==250&&bore.getItem(1).getCount()==1,"Initial refuel runs after the idle timer wraps");
        for(int i=0;i<4;i++)MachineLogic.bore(level,bore);
        check(bore.energy()==250,"Basic focus waits five ticks per cycle");
        MachineLogic.bore(level,bore);
        check(bore.energy()==249&&focus.getDamageValue()==0,"Empty mining cycle spends fuel without wearing the focus");
        bore.restoreEnergy(1);bore.progress=4;
        var atMouth=item(level,pos.east().getCenter(),new ItemStack(Items.COBBLESTONE));
        MachineLogic.bore(level,bore);
        check(bore.energy()==250&&bore.getItem(1).isEmpty(),"Last fuel unit refills in the same tick");
        check(atMouth.isAlive()&&atMouth.position().equals(pos.east().getCenter())&&!atMouth.noPhysics,"Exhaustion skips collection before refueling");
        MachineLogic.bore(level,bore);
        check(atMouth.isAlive()&&atMouth.getX()<pos.getX()&&!atMouth.noPhysics,"Following tick ejects the same item behind the bore");
        clearItems(level,pos);

        bore.setItem(0,new ItemStack(Content.item("arcane_focus_fire")));bore.restoreEnergy(246);bore.progress=4;
        var fuel=item(level,pos.east().getCenter(),new ItemStack(Items.COBBLESTONE,5));
        MachineLogic.bore(level,bore);
        check(!fuel.isAlive()&&bore.energy()==250,"Mining spends fuel before Fire focus tests whole-stack capacity");
        fuel=item(level,pos.east().getCenter(),new ItemStack(Items.COBBLESTONE,2));bore.restoreEnergy(249);bore.progress=0;
        MachineLogic.bore(level,bore);
        check(fuel.isAlive()&&fuel.getItem().getCount()==2&&fuel.getX()<pos.getX()&&bore.energy()==249,"Oversized fuel stack is ejected intact");
        var derived=new ItemStack(Items.COBBLESTONE_SLAB);float derivedValue=GameData.vis(derived);
        check(derivedValue>0&&derivedValue<2&&GameData.basicVis(derived)==0,"Basic lookup excludes even a cached recipe-derived low value");
        fuel=item(level,pos.east().getCenter(),derived);bore.restoreEnergy(100);bore.progress=0;MachineLogic.bore(level,bore);
        check(fuel.isAlive()&&fuel.getX()<pos.getX()&&bore.energy()==100,"Fire focus ejects recipe-derived fuel");
        fuel=item(level,pos.east().getCenter(),new ItemStack(Items.END_STONE));bore.progress=0;MachineLogic.bore(level,bore);
        check(fuel.isAlive()&&bore.energy()==100,"Value exactly two is not conserved");
        clearItems(level,pos);

        bore.setItem(0,new ItemStack(Content.item("arcane_focus")));bore.progress=4;
        wall(level,pos,Blocks.BEDROCK.defaultBlockState());
        wall(level,pos.east(),Blocks.WATER.defaultBlockState());
        wall(level,pos.east(2),Blocks.DIAMOND_ORE.defaultBlockState());
        MachineLogic.bore(level,bore);
        check(bore.getItem(0).getDamageValue()==1&&level.getBlockState(pos.east(2)).is(Blocks.BEDROCK)&&level.getBlockState(pos.east(3)).is(Blocks.WATER),"Drilling skips water and bedrock to mine beyond them");
        check(level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(8)).stream().anyMatch(e->e.getItem().is(Items.DIAMOND)&&e.getItem().getCount()==1),"Tool-free mining produces ordinary ore drops without fortune or silk touch");
        wall(level,pos,Blocks.AIR.defaultBlockState());wall(level,pos.east(),Blocks.AIR.defaultBlockState());wall(level,pos.east(2),Blocks.AIR.defaultBlockState());
        clearItems(level,pos);

        // TileBore replaces its target with air, including waterlogged blocks.
        var wetSlab=Blocks.OAK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true);
        wall(level,pos,wetSlab);bore.progress=4;MachineLogic.bore(level,bore);
        int cleared=0;
        for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)if(level.getBlockState(pos.offset(2,y,z)).isAir())cleared++;
        check(cleared==1,"Mined waterlogged block becomes air rather than leaving source water");
        var slabDrops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(8),e->e.getItem().is(Items.OAK_SLAB));
        check(slabDrops.size()==1&&level.getBlockState(slabDrops.getFirst().blockPosition()).isAir(),"Block loot spawns inside the cleared block");
        wall(level,pos,Blocks.AIR.defaultBlockState());clearItems(level,pos);

        level.setBlockAndUpdate(pos.west(),Blocks.CHEST.defaultBlockState());
        var chest=(net.minecraft.world.Container)level.getBlockEntity(pos.west());
        for(int slot=0;slot<chest.getContainerSize();slot++)chest.setItem(slot,new ItemStack(Items.STONE,64));
        chest.setItem(0,new ItemStack(Items.STONE,62));
        var overflow=item(level,pos.east().getCenter(),new ItemStack(Items.STONE,3));bore.progress=0;MachineLogic.bore(level,bore);
        check(chest.getItem(0).getCount()==64&&overflow.isAlive()&&overflow.getItem().getCount()==1&&overflow.getX()<pos.getX(),"Rear inventory fills before the same item entity ejects its remainder");
        chest.clearContent();level.setBlockAndUpdate(pos.west(),Blocks.AIR.defaultBlockState());clearItems(level,pos);

        // Rear output reaches a full double-chest half's free partner through the combined inventory.
        var pairState=Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING,Direction.EAST);
        level.setBlockAndUpdate(pos.west(),pairState.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT));
        level.setBlockAndUpdate(pos.west().south(),pairState.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.RIGHT));
        var attached=(net.minecraft.world.Container)level.getBlockEntity(pos.west());var paired=(net.minecraft.world.Container)level.getBlockEntity(pos.west().south());
        for(int slot=0;slot<attached.getContainerSize();slot++)attached.setItem(slot,new ItemStack(Items.STONE,64));
        var spill=item(level,pos.east().getCenter(),new ItemStack(Items.STONE,3));bore.progress=0;MachineLogic.bore(level,bore);
        check(!spill.isAlive()&&paired.countItem(Items.STONE)==3,"Rear output fills the free half of a double chest");
        attached.clearContent();paired.clearContent();
        level.setBlockAndUpdate(pos.west(),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos.west().south(),Blocks.AIR.defaultBlockState());clearItems(level,pos);

        // Original suction includes the bore's own block and extends only along its facing axis.
        bore.setItem(0,new ItemStack(Content.item("arcane_focus")));
        for(Direction facing:Direction.values()){
            level.setBlockAndUpdate(pos,bore.getBlockState().setValue(MachineBlock.FACING,facing));
            Vec3 step=Vec3.atLowerCornerOf(facing.getUnitVec3i());
            var base=item(level,pos.getCenter().subtract(step.scale(.25)),new ItemStack(Items.DIRT));
            var reach=item(level,pos.getCenter().add(step.scale(41)),new ItemStack(Items.DIRT));
            var beyond=item(level,pos.getCenter().add(step.scale(43)),new ItemStack(Items.DIRT));
            bore.progress=0;bore.restoreEnergy(100);MachineLogic.bore(level,bore);
            check(base.noPhysics&&reach.noPhysics&&!beyond.noPhysics,"Directional suction bounds for "+facing+": "+base.noPhysics+","+reach.noPhysics+","+beyond.noPhysics+" at "+bore.getBlockState().getValue(MachineBlock.FACING));
            clearItems(level,pos);bore.processes.clearBoreAttraction();
        }
        level.setBlockAndUpdate(pos,bore.getBlockState().setValue(MachineBlock.FACING,Direction.EAST));
        var corner=item(level,new Vec3(pos.getX()+1.05,pos.getY()-.2,pos.getZ()-.1),new ItemStack(Items.DIRT));
        check(corner.getBoundingBox().intersects(new AABB(pos.east()))&&corner.position().distanceToSqr(pos.east().getCenter())>1,"Mouth fixture distinguishes box intersection from sphere distance");
        bore.progress=0;MachineLogic.bore(level,bore);
        check(corner.isAlive()&&corner.getX()<pos.getX(),"Mouth collects intersecting item bounds at its corner");
        clearItems(level,pos);

        var attracted=item(level,pos.east(5).getCenter(),new ItemStack(Items.DIRT));
        bore.progress=0;MachineLogic.bore(level,bore);attracted.tick();
        check(attracted.noPhysics,"Item tick retains bore wall traversal");
        // Invoke block effects without noPhysics, just as native items do outside solid blocks.
        attracted.noPhysics=false;attracted.applyEffectsFromBlocks(attracted.position(),attracted.position());
        check(attracted.getRemainingFireTicks()==-50,"Attracted item receives the original 50-tick ignition delay");
        check(attracted.hurtServer(level,level.damageSources().inFire(),1),"Ignition delay is not blanket damage immunity");
        level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());MachineLogic.bore(level,bore);
        check(!attracted.noPhysics,"Loss of power releases attraction");
        attracted.applyEffectsFromBlocks(attracted.position(),attracted.position());
        check(attracted.getRemainingFireTicks()==0,"Released item regains native ignition delay");
        level.setBlockAndUpdate(pos.below(),Blocks.REDSTONE_BLOCK.defaultBlockState());bore.progress=0;MachineLogic.bore(level,bore);
        check(attracted.noPhysics,"Power restores attraction");bore.setRemoved();
        attracted.applyEffectsFromBlocks(attracted.position(),attracted.position());
        check(!attracted.noPhysics&&attracted.getRemainingFireTicks()==0,"Chunk unload releases items in neighboring loaded chunks");
        bore.clearRemoved();
        clearItems(level,pos);

        // Reload retains paid fuel and worn focus, but not the original transient mining timer.
        bore.restoreEnergy(123);focus.setDamageValue(17);bore.setItem(0,focus);bore.progress=3;
        var saved=bore.saveWithFullMetadata(level.registryAccess());
        var restored=(MachineBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,bore.getBlockState(),saved,level.registryAccess());
        check(restored!=null&&restored.energy()==123&&restored.getItem(0).getDamageValue()==17&&restored.progress==0,"Reload preserves fuel and focus while resetting the mining timer");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());
        loaded.forEach(chunk->level.setChunkForced(chunk.x(),chunk.z(),false));
        return checks;
    }
    private static void wall(ServerLevel level,BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
        for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)level.setBlockAndUpdate(pos.offset(2,y,z),state);
    }
    private static ItemEntity item(ServerLevel level,Vec3 at,ItemStack stack){
        var block=BlockPos.containing(at);var chunk=new net.minecraft.world.level.ChunkPos(block.getX()>>4,block.getZ()>>4);
        if(loaded.add(chunk)){level.setChunkForced(chunk.x(),chunk.z(),true);level.getChunk(chunk.x(),chunk.z());level.getChunkSource().tick(()->true,false);level.waitForEntities(chunk,0);}
        var item=new ItemEntity(level,at.x,at.y,at.z,stack);
        item.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(item);return item;
    }
    private static void clearItems(ServerLevel level,BlockPos pos){level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(85)).forEach(ItemEntity::discard);}
}
