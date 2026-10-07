package dev.thaumcraft.test;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.CrystalBlock;
import dev.thaumcraft.entity.ModEntities;
import dev.thaumcraft.entity.TaintedCreeper;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

/** Source-backed world regressions: state transitions, generation ranges and hostile effects. */
public final class WorldParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String description){checks++;if(!value)throw new AssertionError(description);}
    public static int run(MinecraftServer server){
        checks=0;ServerLevel level=server.overworld();
        biomeAura(level);boostDiffusion(level);crystals(level);creeper(level);taint(level);portableHole(level);portableInventory(level);voidLinks(server);eldritch(level);
        return checks;
    }
    private static void biomeAura(ServerLevel level){
        var biomes=level.registryAccess().lookupOrThrow(Registries.BIOME);
        var plains=biomes.getOrThrow(Biomes.PLAINS);var forest=biomes.getOrThrow(Biomes.FOREST);
        var jungle=biomes.getOrThrow(Biomes.JUNGLE);var desert=biomes.getOrThrow(Biomes.DESERT);var swamp=biomes.getOrThrow(Biomes.SWAMP);
        for(int seed=0;seed<40;seed++){
            var ordinary=ArcaneWorldData.initialAura(seed,0,0,plains,false);
            if(ordinary.taint()>7500)continue;
            var wooded=ArcaneWorldData.initialAura(seed,0,0,forest,false);
            var lush=ArcaneWorldData.initialAura(seed,0,0,jungle,false);
            var dry=ArcaneWorldData.initialAura(seed,0,0,desert,false);
            var marsh=ArcaneWorldData.initialAura(seed,0,0,swamp,false);
            check(ordinary.vis()>=3000&&ordinary.vis()<5000,"Ordinary biome aura range");
            check(wooded.vis()>=5000&&wooded.vis()<9000,"Forest biome aura range");
            check(lush.vis()>=7500&&lush.vis()<10500,"Jungle biome aura range");
            check(dry.vis()>=750&&dry.vis()<1875,"Desert low aura range");
            check(marsh.vis()>=5000&&marsh.vis()<7500&&marsh.taint()==(int)((int)(marsh.vis()/3)*1.5f),"Swamp receives high aura and extra taint");
        }
    }
    private static ArcaneWorldData cells(int donorBoost){
        JsonObject cells=new JsonObject();
        cells.add(Long.toString(ChunkPos.pack(0,0)),ArcaneWorldData.Aura.CODEC.encodeStart(JsonOps.INSTANCE,new ArcaneWorldData.Aura(4000,1000,4000,0,0,0)).getOrThrow());
        cells.add(Long.toString(ChunkPos.pack(1,0)),ArcaneWorldData.Aura.CODEC.encodeStart(JsonOps.INSTANCE,new ArcaneWorldData.Aura(4000,1000,4000,0,0,donorBoost)).getOrThrow());
        JsonObject root=new JsonObject();root.add("aura",cells);
        return ArcaneWorldData.CODEC.parse(JsonOps.INSTANCE,root).getOrThrow();
    }
    private static void boostDiffusion(ServerLevel level){
        var data=cells(51);data.tickAura(level);data.tickAura(level);
        check(data.aura(level,BlockPos.ZERO).boost()==1&&data.aura(level,new BlockPos(16,0,0)).boost()==50,"Boost diffuses only from donors above50 and is conserved");
        data=cells(50);data.tickAura(level);data.tickAura(level);
        check(data.aura(level,BlockPos.ZERO).boost()==0&&data.aura(level,new BlockPos(16,0,0)).boost()==50,"Boost50 does not diffuse");
        var encoded=ArcaneWorldData.CODEC.encodeStart(JsonOps.INSTANCE,data).getOrThrow();
        check(ArcaneWorldData.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow().aura(level,new BlockPos(16,0,0)).boost()==50,"Aura boost survives serialization");
    }
    private static void crystals(ServerLevel level){
        BlockPos pos=new BlockPos(720,290,720);level.getChunkAt(pos);level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        var data=ArcaneWorldData.get(level);var before=data.aura(level,pos);
        data.changeAura(level,pos,4000-before.vis(),1000-before.taint());data.drainVibes(level,pos,100,false);data.drainVibes(level,pos,100,true);
        level.setBlockAndUpdate(pos,Content.block("vis_ore").defaultBlockState().setValue(CrystalBlock.AMOUNT,3));
        var random=RandomSource.create(216);
        for(int i=0;i<2000;i++)level.getBlockState(pos).randomTick(level,pos,random);
        check(level.getBlockState(pos).getValue(CrystalBlock.AMOUNT)==3,"Raw aura advantage cannot grow crystals beyond3 without vibes");
        data.addVibes(level,pos,1,0);
        for(int i=0;i<12000&&level.getBlockState(pos).getValue(CrystalBlock.AMOUNT)<5;i++)level.getBlockState(pos).randomTick(level,pos,random);
        check(level.getBlockState(pos).getValue(CrystalBlock.AMOUNT)==5,"Good vibes allow crystal growth to5");
        data.changeAura(level,pos,-4000,0);data.drainVibes(level,pos,100,false);
        for(int i=0;i<20;i++)level.getBlockState(pos).randomTick(level,pos,random);
        check(data.aura(level,pos).goodVibes()>0,"Fully grown crystals restore vibes to depleted aura");
        for(Direction facing:Direction.values()){
            var state=Content.block("vis_ore").defaultBlockState().setValue(CrystalBlock.FACING,facing);
            var bounds=state.getShape(level,pos).bounds();
            check(switch(facing){case UP->bounds.minY==0;case DOWN->bounds.maxY==1;case NORTH->bounds.maxZ==1;case SOUTH->bounds.minZ==0;case WEST->bounds.maxX==1;case EAST->bounds.minX==0;},"Crystal hitbox attaches on "+facing);
        }
        level.removeBlock(pos,false);level.removeBlock(pos.below(),false);
    }
    private static void creeper(ServerLevel level){
        BlockPos pos=new BlockPos(752,290,720);ArcanaParitySmokeTests.prepareEntities(level,pos);
        var entity=ModEntities.TYPES.get("tainted_creeper").create(level,EntitySpawnReason.MOB_SUMMONED);
        check(entity instanceof TaintedCreeper,"Tainted creeper uses its corruption behavior");
        var creeper=(TaintedCreeper)entity;creeper.setNoAi(true);creeper.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);creeper.setNoGravity(true);
        var data=ArcaneWorldData.get(level);data.changeAura(level,pos,0,-data.aura(level,pos).taint());
        level.addFreshEntity(creeper);check(level.getEntity(creeper.getUUID())==creeper,"Creeper fixture is visible to server entity queries");creeper.ignite();
        for(int i=0;i<35&&!creeper.isRemoved();i++)creeper.tick();
        float taint=data.aura(level,pos).taint();
        check(creeper.isRemoved()&&taint>=100&&taint<200,"Tainted creeper fuse adds original100–199taint on explosion");
    }
    private static void taint(ServerLevel level){
        var source=new net.minecraft.world.level.block.Block[]{Blocks.DIRT,Blocks.SAND,Blocks.STONE,Blocks.GRAVEL,Blocks.MYCELIUM,Blocks.COAL_ORE,Blocks.DIAMOND_ORE,Blocks.GOLD_ORE,Blocks.IRON_ORE,Blocks.REDSTONE_ORE,Blocks.LAPIS_ORE,Content.block("cinnabar_ore")};
        int[] materials={0,1,2,3,4,5,6,7,8,9,11,12};
        for(int i=0;i<source.length;i++){
            var corrupted=dev.thaumcraft.content.TaintBlock.taint(source[i].defaultBlockState());
            check(corrupted!=null&&corrupted.getValue(dev.thaumcraft.content.TaintBlock.MATERIAL)==materials[i],"Taint retains original material "+materials[i]);
            check(dev.thaumcraft.content.TaintBlock.restore(corrupted).is(source[i]),"Taint restores original material "+materials[i]);
        }
        check(dev.thaumcraft.content.TaintBlock.restore(dev.thaumcraft.content.TaintBlock.taint(Blocks.PUMPKIN.defaultBlockState())).isAir(),"Inert taint disappears on purification");
        check(dev.thaumcraft.content.TaintBlock.taint(Blocks.COBBLESTONE.defaultBlockState())==null,"Original cobblestone resists taint conversion");
        BlockPos pos=new BlockPos(780,290,720);level.getChunkAt(pos);level.setBlockAndUpdate(pos,Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState());
        check(dev.thaumcraft.content.TaintBlock.taint(level,pos)&&dev.thaumcraft.content.TaintBlock.purify(level,pos)&&level.getBlockState(pos).is(Blocks.DEEPSLATE_DIAMOND_ORE),"Modern deepslate ore retains its source state through taint memory");
        level.setBlockAndUpdate(pos.east(),Content.block("totem_of_dawn").defaultBlockState());
        check(dev.thaumcraft.content.TaintBlock.protectedBySilverwood(level,pos),"Totem of Dawn protects nearby terrain like silverwood");
        level.removeBlock(pos,false);level.removeBlock(pos.east(),false);
    }
    private static void portableHole(ServerLevel level){
        BlockPos start=new BlockPos(800,290,720);for(int i=0;i<=12;i++){
            BlockPos pos=start.east(i);level.getChunkAt(pos);
            level.setBlockAndUpdate(pos,(i<12?Blocks.STONE:Blocks.AIR).defaultBlockState());level.setBlockAndUpdate(pos.below(),(i<12?Blocks.STONE:Blocks.AIR).defaultBlockState());
        }
        var space=dev.thaumcraft.world.TemporarySpace.get(level);
        check(space.open(level,start,Direction.EAST,11)==0&&level.getBlockState(start).is(Blocks.STONE),"Insufficient portable-hole charge leaves the complete wall intact");
        level.setBlockAndUpdate(start.east(5).below(),Blocks.BEDROCK.defaultBlockState());
        check(space.open(level,start,Direction.EAST,32)==0&&level.getBlockState(start).is(Blocks.STONE),"Blocked lower tunnel row rejects the operation before mutation");
        level.setBlockAndUpdate(start.east(5).below(),Blocks.STONE.defaultBlockState());
        check(space.open(level,start,Direction.EAST,12)==12,"Portable hole charges tunnel length beyond8blocks");
        for(int i=0;i<12;i++)check(level.getBlockState(start.east(i)).is(Content.TEMPORARY_SPACE)&&level.getBlockState(start.east(i).below()).is(Content.TEMPORARY_SPACE),"Portable hole opens both rows at length "+i);
        for(int i=0;i<12;i++){level.removeBlock(start.east(i),false);level.removeBlock(start.east(i).below(),false);}
    }
    private static void portableInventory(ServerLevel level){
        BlockPos pos=new BlockPos(824,290,720);ArcanaParitySmokeTests.prepareEntities(level,pos);
        var probe=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        level.addFreshEntity(probe);
        check(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).contains(probe),"Portable inventory fixture can detect spawned item spills");
        probe.discard();
        level.setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        level.removeBlock(pos.east(),false);level.removeBlock(pos.east().below(),false);
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(pos);
        chest.setItem(7,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,17));
        var hole=new dev.thaumcraft.world.TemporarySpace();
        check(hole.open(level,pos,Direction.DOWN,32)==0&&chest.getItem(7).getCount()==17,"Vertical portable hole rejects tile entities without consuming inventory");
        check(hole.open(level,pos,Direction.EAST,32)==1,"Horizontal portable hole accepts inventory blocks as the original caller does");
        check(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).isEmpty(),"Suspending a chest does not spill inventory");
        var encoded=(net.minecraft.nbt.ListTag)dev.thaumcraft.world.TemporarySpace.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,hole).getOrThrow();
        for(var entry:encoded)((net.minecraft.nbt.CompoundTag)entry).putLong("expires",0);
        var restarted=dev.thaumcraft.world.TemporarySpace.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,encoded).getOrThrow();
        restarted.restore(level,pos);restarted.restore(level,pos.below());
        restarted.restore(level,pos);
        var restored=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(pos);
        check(restored!=null&&restored.getItem(7).is(net.minecraft.world.item.Items.DIAMOND)&&restored.getItem(7).getCount()==17,"Portable-hole NBT roundtrip restores exact inventory after restart");
        check(((net.minecraft.nbt.ListTag)dev.thaumcraft.world.TemporarySpace.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,restarted).getOrThrow()).isEmpty(),"Restoration consumes its saved inventory exactly once");
        check(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2)).isEmpty(),"Restoring a chest produces no duplicate item entities");
        restored.clearContent();level.removeBlock(pos,false);level.removeBlock(pos.below(),false);
    }
    private static dev.thaumcraft.machine.MachineBlockEntity machine(ServerLevel level,BlockPos pos,String id){
        level.getChunkAt(pos);level.setBlockAndUpdate(pos,Content.block(id).defaultBlockState());
        return (dev.thaumcraft.machine.MachineBlockEntity)level.getBlockEntity(pos);
    }
    private static void voidLinks(MinecraftServer server){
        var level=server.overworld();BlockPos a=new BlockPos(840,100,720),b=a.east(4);
        machine(level,a.below(),"void_chest");machine(level,b.below(),"void_chest");
        var first=machine(level,a,"void_interface");var second=machine(level,b,"void_interface");
        first.setOwner(new java.util.UUID(0,1));second.setOwner(new java.util.UUID(0,2));first.setChannel(3);second.setChannel(3);
        var networks=dev.thaumcraft.gameplay.VoidNetworks.get(level);networks.register(level,a);networks.register(level,b);
        check(networks.linkedChests(level,first).equals(java.util.List.of(level.getBlockEntity(a.below())))
                &&networks.linkedChests(level,second).equals(java.util.List.of(level.getBlockEntity(b.below()))),"Matching void channels remain isolated across owners");
        second.setOwner(first.owner());check(networks.linkedChests(level,first).size()==2,"Matching owner and channel link void interfaces");
        second.setChannel(4);check(networks.linkedChests(level,first).size()==1,"Different void channels remain isolated");
        var nether=server.getLevel(net.minecraft.world.level.Level.NETHER);
        if(nether!=null){
            var remote=machine(nether,a,"void_interface");machine(nether,a.below(),"void_chest");remote.setChannel(3);networks.register(nether,a);
            check(networks.linkedChests(level,first).size()==1&&networks.linkedChests(nether,remote).equals(java.util.List.of(nether.getBlockEntity(a.below()))),"Ownerless void interfaces expose only their local chest");
            remote.setOwner(first.owner());
            check(networks.linkedChests(level,first).equals(java.util.List.of(level.getBlockEntity(a.below()),nether.getBlockEntity(a.below())))
                    &&networks.linkedChests(nether,remote).equals(java.util.List.of(nether.getBlockEntity(a.below()),level.getBlockEntity(a.below()))),"Matching owner and channel link across dimensions with local chest first");
            networks.remove(nether,a);nether.removeBlock(a,false);nether.removeBlock(a.below(),false);
        }
        networks.remove(level,a);networks.remove(level,b);
        for(var pos:java.util.List.of(a,b)){level.removeBlock(pos,false);level.removeBlock(pos.below(),false);}
    }
    static int eldritch(ServerLevel level){
        int before=checks;
        BlockPos core=new BlockPos(880,100,880);level.getChunkAt(core);level.setBlockAndUpdate(core,Content.block("eldritch_core").defaultBlockState());
        level.setBlockAndUpdate(core.below(20),Content.block("vis_ore").defaultBlockState());
        check(dev.thaumcraft.world.EldritchStructures.openEntrance(level,core),"A real monolith core and natural crystal tile permit entrance generation");
        BlockPos base=new BlockPos(core.getX(),6,core.getZ()),lock=null;int chests=0;
        for(BlockPos pos:BlockPos.betweenClosed(base.offset(-7,0,-7),base.offset(7,7,7))){
            if(level.getBlockState(pos).is(Content.block("eldritch_lock"))&&lock==null)lock=pos.immutable();
            if(level.getBlockState(pos).is(Content.block("void_chest"))){chests++;check(((dev.thaumcraft.machine.MachineBlockEntity)level.getBlockEntity(pos)).getContainerSize()==72,"Generated Eldritch chest retains72slots");}
        }
        check(chests>0&&lock!=null,"Original Eldritch blueprint contains void chests and live locks");
        var direction=level.getBlockState(lock).getValue(dev.thaumcraft.world.EldritchBlock.FACING);
        check(dev.thaumcraft.world.EldritchStructures.unlockRoom(level,lock,direction),"A real Eldritch lock block entity permits its chamber generation");
        check(level.getBlockState(lock.relative(direction,2)).isAir(),"Unlocked Eldritch doorway clears the third depth layer");
        var oldLock=new BlockPos(1040,level.getMinY()+15,880);level.getChunkAt(oldLock);
        level.setBlockAndUpdate(oldLock,Content.block("eldritch_lock").defaultBlockState());
        var observer=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"DoorwayObserver"),net.minecraft.server.level.ClientInformation.createDefault());
        new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),observer,net.minecraft.server.network.CommonListenerCookie.createInitial(observer.getGameProfile(),false));
        observer.setPos(oldLock.getCenter());level.addNewPlayer(observer);
        var poofs=new java.util.ArrayList<net.minecraft.world.phys.Vec3>();var sender=dev.thaumcraft.network.EquipmentEffect.sender;
        dev.thaumcraft.network.EquipmentEffect.sender=(recipient,effect)->{if(recipient==observer&&effect.kind()==dev.thaumcraft.network.EquipmentEffect.VOID_POOF)poofs.add(effect.origin());};
        try{
            check(dev.thaumcraft.world.EldritchStructures.unlockRoom(level,oldLock,Direction.EAST),"existing deep port chamber locks still open at their own elevation");
        }finally{dev.thaumcraft.network.EquipmentEffect.sender=sender;observer.discard();}
        check(poofs.size()==27&&new java.util.HashSet<>(poofs).size()==27&&poofs.stream().allMatch(p->level.getBlockState(BlockPos.containing(p)).isAir()&&BlockPos.containing(p).distManhattan(oldLock.east())<=3),"Opening a lock sends one dark wisp burst per cleared doorway cell");
        check(level.getBlockState(oldLock.east(7).below(3)).is(Content.block("eldritch_structure_stone")),"existing deep chamber receives its adjacent room at the same floor height");
        var lowCore=new BlockPos(1200,level.getMinY()+20,880);level.getChunkAt(lowCore);
        level.setBlockAndUpdate(lowCore,Content.block("eldritch_core").defaultBlockState());
        check(dev.thaumcraft.world.EldritchStructures.openEntrance(level,lowCore),"preexisting low port cores remain solvable");
        return checks-before;
    }

}
