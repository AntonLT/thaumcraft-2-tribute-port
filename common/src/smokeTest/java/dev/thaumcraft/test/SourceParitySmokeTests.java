package dev.thaumcraft.test;

import dev.thaumcraft.content.*;
import dev.thaumcraft.entity.*;
import dev.thaumcraft.gameplay.*;
import dev.thaumcraft.machine.*;
import dev.thaumcraft.world.SealPortals;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Regressions for source-level omissions outside the independent subsystem fixtures. */
public final class SourceParitySmokeTests {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError("Source parity: "+message);}
    private static MachineBlockEntity seal(ServerLevel level,BlockPos pos,Direction facing,int... runes){
        level.getChunkAt(pos);level.setBlockAndUpdate(pos.relative(facing.getOpposite()),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos,Content.block("arcane_seal").defaultBlockState().setValue(MachineBlock.FACING,facing));
        var machine=(MachineBlockEntity)level.getBlockEntity(pos);
        for(int i=0;i<runes.length;i++){int rune=runes[i];var entry=Content.ENTRIES.stream().filter(e->e.source_class().equals("ItemRunicEssence")&&e.meta()==rune).findFirst().orElseThrow();machine.setItem(18+i,new ItemStack(Content.item(entry.id())));}
        return machine;
    }
    static int run(MinecraftServer server){
        checks=0;var level=server.overworld();BlockPos pos=new BlockPos(432,280,432);ArcanaParitySmokeTests.prepareEntities(level,pos);
        for(String group:new String[]{"creaking","gore","page","shock","stomp","swing","tool","wind","zap"})
            check(BuiltInRegistries.SOUND_EVENT.getValue(dev.thaumcraft.Thaumcraft.id(group))==ModSounds.event(group),"Original sound variant group registered: "+group);
        check(new ItemStack(Content.item("goggles_of_revealing")).getMaxDamage()==350,"Goggles retain explicit durability instead of inherited armor durability");
        check(new ItemStack(Content.item("wand_of_fire")).getRarity()==Rarity.RARE,"Fire wand original rarity");
        check(new ItemStack(Content.item("vis_crystal")).hasFoil()&&!new ItemStack(Content.item("depleted_crystal")).hasFoil(),"Only charged crystal variants have the original glint");
        check(Content.block("arcane_seal").defaultBlockState().getDestroySpeed(level,pos)==.5f,"Seal hardness");
        check(Content.block("vis_conduit").defaultBlockState().getLightEmission()==5&&Content.block("glowing_nitor").defaultBlockState().getLightEmission()==14,"Original apparatus light values");
        for(String name:new String[]{"ice","venom","repair","vampiric","soulstealer","relic"}){
            var enchant=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getValue(dev.thaumcraft.Thaumcraft.id(name));
            check(enchant.getMaxCost(1)==61&&enchant.getMaxCost(2)==71,"Superclass maximum enchantability for "+name);
        }
        float stick=GameData.vis(new ItemStack(Items.STICK));
        check(stick>0&&GameData.vis(new ItemStack(Items.LADDER))>0,"Registered vanilla crafting ingredients derive missing vis values");
        String smelting=dev.thaumcraft.PortConfig.customSmelting;
        dev.thaumcraft.PortConfig.setCustomSmelting("minecraft:barrier:17");check(GameData.vis(new ItemStack(Items.BARRIER))==17,"Namespaced addon smelting configuration");
        dev.thaumcraft.PortConfig.setCustomSmelting("Blue_Alloy_Ingot:37;Structure_Void:5");var renamed=new ItemStack(Items.BARRIER);renamed.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Blue Alloy Ingot"));
        check(GameData.vis(new ItemStack(Items.STRUCTURE_VOID))==5&&GameData.vis(renamed)==0,"Legacy smelting names match registry paths, never anvil names");dev.thaumcraft.PortConfig.setCustomSmelting(smelting);
        var aura=ArcaneWorldData.get(level);var boost=seal(level,pos,Direction.UP,0,0);aura.changeBoost(level,pos,-100);var before=aura.aura(level,pos);
        SealLogic.tick(level,boost);for(int i=0;i<14;i++)SealLogic.tick(level,boost);
        check(aura.aura(level,pos).boost()==1,"Double magic seal waits fifteen ticks between boosts");SealLogic.tick(level,boost);
        check(aura.aura(level,pos).boost()==2&&aura.aura(level,pos).vis()==before.vis(),"Magic seal adds one boost without invented aura consumption");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());var nullifier=seal(level,pos,Direction.UP,5,0);before=aura.aura(level,pos);SealLogic.tick(level,nullifier);
        check(aura.aura(level,pos).vis()==before.vis()-1&&aura.aura(level,pos).taint()==before.taint()-1&&aura.aura(level,pos).badVibes()==Math.min(100,before.badVibes()+1),"Nullifier exchanges one vis and taint for one bad vibe");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());var sensor=seal(level,pos,Direction.EAST,0,4,4);
        // Stray items nearby must not count as the deliberate front/behind sensor test objects.
        level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(10)).forEach(Entity::discard);
        var behind=new ItemEntity(level,pos.getX()-2,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.DIAMOND));level.addFreshEntity(behind);SealLogic.tick(level,sensor);check(sensor.energy()==0,"Directional sensor ignores objects behind its face");
        var ahead=new ItemEntity(level,pos.getX()+2,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.DIAMOND));level.addFreshEntity(ahead);sensor.progress=0;SealLogic.tick(level,sensor);check(sensor.energy()==15,"Directional item sensor powers for an object in front");behind.discard();ahead.discard();
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);var source=seal(level,pos,Direction.NORTH,0,1,2);var destination=seal(level,pos.east(20),Direction.NORTH,0,1,2);
        source.setOwner(java.util.UUID.randomUUID());destination.setOwner(java.util.UUID.randomUUID());level.setBlockAndUpdate(destination.getBlockPos().north().below(),Blocks.STONE.defaultBlockState());
        SealPortals.tick(level,source,2);SealPortals.tick(level,destination,2);
        var freight=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.DIAMOND,3));level.addFreshEntity(freight);
        check(SealPortals.tick(level,source,2)&&freight.distanceToSqr(destination.getBlockPos().north().getCenter())<1,"Portal moves item entities across public matching rune networks");
        check(freight.getItem().getCount()==3&&destination.progress==40,"Portal preserves freight and blocks immediate return for forty ticks");freight.discard();
        var target=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);target.setPos(pos.south(6).getBottomCenter());level.addFreshEntity(target);
        ArcaneMote.scorch(level,pos.south(2).getCenter(),target,3,true,true,true,false);
        var motes=level.getEntitiesOfClass(ArcaneMote.class,new AABB(pos).inflate(16));check(motes.size()==3&&target.getHealth()==20,"Scorch launches moving flames without immediate ray damage");
        for(int tick=0;tick<20;tick++)for(ArcaneMote mote:motes)if(mote.isAlive())mote.tick();
        check(target.getHealth()<20&&target.isOnFire(),"Moving scorch applies damage and fire on contact");for(var mote:motes)mote.discard();target.discard();
        for(BlockPos p:BlockPos.betweenClosed(pos.offset(-2,-1,-2),pos.offset(22,1,8)))level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
        return checks;
    }
}
