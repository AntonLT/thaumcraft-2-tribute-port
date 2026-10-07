package dev.thaumcraft.neoforge;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Runs only with -PsmokeTest, before the common success marker. */
public final class GeneratorEnergySmokeTests {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    public static void run(MinecraftServer server) {
        checks=0;var level=server.overworld();var pos=new BlockPos(96,300,96);level.getChunkAt(pos);
        check(new ItemStack(Content.item("alumentum")).getBurnTime(RecipeType.SMELTING,level.fuelValues())==16000,"NeoForge vanilla furnace uses alumentum burn time");
        check(new ItemStack(Content.item("silverwood_log")).getBurnTime(RecipeType.SMELTING,level.fuelValues())==600,"NeoForge vanilla furnace uses silverwood burn time");
        check(new ItemStack(Content.item("greatwood_log")).getBurnTime(RecipeType.SMELTING,level.fuelValues())==400,"NeoForge vanilla furnace uses greatwood burn time");
        level.setBlockAndUpdate(pos,Content.block("thaumic_generator").defaultBlockState());
        var machine=(MachineBlockEntity)level.getBlockEntity(pos);machine.generateEnergy(1000);
        EnergyHandler energy=level.getCapability(Capabilities.Energy.BLOCK,pos,Direction.NORTH);
        check(energy!=null,"Generator exposes NeoForge energy capability");
        for(Direction side:Direction.values()) {
            var handler=level.getCapability(Capabilities.Energy.BLOCK,pos,side);
            check(handler!=null,"Every generator side exposes energy");
            check(level.getCapability(Capabilities.Energy.BLOCK,pos,side)==handler,"Each side caches its energy wrapper");
            check(handler.getAmountAsLong()==energy.getAmountAsLong(),"All generator sides expose shared storage");
        }
        try(var transaction=Transaction.openRoot()) {
            check(energy.insert(500,transaction)==0,"Generator refuses incoming energy");transaction.commit();
        }
        check(machine.energy()==1000,"Refused insertion leaves stored energy unchanged");
        try(var outer=Transaction.openRoot()) {
            check(energy.extract(8,outer)==8,"Outer extraction returns requested amount");
            try(var inner=Transaction.open(outer)) {
                var otherSide=level.getCapability(Capabilities.Energy.BLOCK,pos,Direction.UP);
                check(otherSide.extract(7,inner)==7,"Nested extraction works through another side");inner.commit();
            }
            check(machine.energy()==985,"Nested commit retains provisional extraction");
        }
        check(machine.energy()==1000,"Aborting outer transaction restores both extractions exactly");
        try(var outer=Transaction.openRoot()) {
            energy.extract(5,outer);
            try(var inner=Transaction.open(outer)){energy.extract(6,inner);}
            check(machine.energy()==995,"Aborting inner transaction restores only inner extraction");outer.commit();
        }
        check(machine.energy()==995&&energy.getAmountAsLong()==995,"Committed extraction persists after transaction closure");
        var saved=machine.saveWithFullMetadata(level.registryAccess());
        check(saved.getIntOr("energy",-1)==995,"Committed energy is serialized for world saves");
        try(var transaction=Transaction.openRoot()) {
            check(energy.extract(5000,transaction)==55,"Extraction is bounded by the remaining shared per-tick budget");
        }
        check(machine.energy()==995,"Rollback after full extraction restores exact remainder");
        try(var transaction=Transaction.openRoot()){check(energy.extract(1000,transaction)==55,"Aborted transfer restores the per-tick allowance");transaction.commit();}
        try(var transaction=Transaction.openRoot()){check(energy.extract(1,transaction)==0,"Committed extractions cannot exceed sixty units per tick");}
        check(machine.energy()==940,"Exactly sixty units emitted during the server tick");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        Thaumcraft.LOG.info("THAUMCRAFT_NEOFORGE_ENERGY_TESTS_PASS checks={}",checks);
    }
}
