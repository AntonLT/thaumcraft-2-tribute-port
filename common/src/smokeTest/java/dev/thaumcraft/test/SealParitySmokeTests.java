package dev.thaumcraft.test;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.machine.MachineBlock;
import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.SealLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Original TileSeal dispatch and target-selection regressions independent of the descriptive text. */
final class SealParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Seal parity: "+message);}
    private static MachineBlockEntity seal(ServerLevel level,BlockPos pos,int... runes){
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos.west(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos,Content.block("arcane_seal").defaultBlockState().setValue(MachineBlock.FACING,Direction.EAST));
        var seal=(MachineBlockEntity)level.getBlockEntity(pos);
        for(int i=0;i<runes.length;i++){int rune=runes[i];var entry=Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemRunicEssence")&&e.meta()==rune).findFirst().orElseThrow();seal.setItem(18+i,new ItemStack(Content.item(entry.id())));}
        return seal;
    }
    static int run(MinecraftServer server){
        checks=0;var level=server.overworld();var pos=new BlockPos(480,280,480);level.getChunkAt(pos);
        check(SealLogic.active(new int[]{0,-1,-1})&&SealLogic.active(new int[]{0,0,-1})&&SealLogic.active(new int[]{0,0,0}),"Magic boost combinations remain active independently of description wording");
        check(SealLogic.active(new int[]{5,3,-1})&&SealLogic.active(new int[]{5,3,3})&&!SealLogic.active(new int[]{5,3,0}),"Spawn suppression follows the original switch, including rejected third runes");
        check(!SealLogic.active(new int[]{0,0,1})&&!SealLogic.active(new int[]{2,2,-1})&&!SealLogic.active(new int[]{4,1,2}),"Unsupported rune branches stay inactive");
        check(SealLogic.cooldown(new int[]{1,4,-1},0)==8&&SealLogic.cooldown(new int[]{1,4,1},0)==6,"Shock timing counts modifier air runes only");
        var aura=ArcaneWorldData.get(level);aura.changeBoost(level,pos,-100);var boost=seal(level,pos,0);
        SealLogic.tick(level,boost);check(aura.aura(level,pos).boost()==1,"Single magic rune boosts even though its description says no effect on the aura rating");
        for(int i=0;i<19;i++)SealLogic.tick(level,boost);check(aura.aura(level,pos).boost()==1,"Single magic boost observes twenty-tick delay");SealLogic.tick(level,boost);check(aura.aura(level,pos).boost()==2,"Single magic boost resumes on the twentieth tick");
        // Power reaches the block above the seal without directly powering the seal block.
        level.setBlockAndUpdate(pos.above().east(),Blocks.REDSTONE_BLOCK.defaultBlockState());boost.progress=0;SealLogic.tick(level,boost);
        check(aura.aura(level,pos).boost()==2,"Redstone around the block above suppresses ordinary seal work");level.setBlockAndUpdate(pos.above().east(),Blocks.AIR.defaultBlockState());
        var suppressor=seal(level,pos,5,3);var fresh=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);var old=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        fresh.setPos(pos.east(2).getBottomCenter());old.setPos(pos.east(4).getBottomCenter());old.tickCount=10;level.addFreshEntity(fresh);level.addFreshEntity(old);SealLogic.tick(level,suppressor);
        check(!fresh.isAlive()&&old.isAlive(),"Dark earth suppresses fresh hostiles while preserving established mobs");old.discard();
        var frost=seal(level,pos,2,1,2);var cow=EntityType.COW.create(level,EntitySpawnReason.COMMAND);var zombie=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        cow.setPos(pos.east(2).getBottomCenter());zombie.setPos(pos.east(4).getBottomCenter());cow.setDeltaMovement(1,0,0);zombie.setDeltaMovement(1,0,0);level.addFreshEntity(cow);level.addFreshEntity(zombie);SealLogic.tick(level,frost);
        check(cow.getDeltaMovement().equals(new Vec3(1,0,0))&&Math.abs(zombie.getDeltaMovement().x-.05)<.0001,"Long-range freeze ignores closer animals and slows the hostile target");cow.discard();zombie.discard();
        var scanner=seal(level,pos,0,4,0);var slime=EntityType.SLIME.create(level,EntitySpawnReason.COMMAND);slime.setPos(pos.east(2).getBottomCenter());level.addFreshEntity(slime);SealLogic.tick(level,scanner);
        check(scanner.energy()==15,"Hostile scanner recognizes Enemy implementations outside Monster, including slimes");slime.discard();
        for(var mote:level.getEntitiesOfClass(dev.thaumcraft.entity.ArcaneMote.class,new net.minecraft.world.phys.AABB(pos).inflate(16)))mote.discard();
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos.west(),Blocks.AIR.defaultBlockState());
        return checks+dev.thaumcraft.machine.SealRegressionChecks.run(server);
    }
}
