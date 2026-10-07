package dev.thaumcraft.neoforge;

import dev.thaumcraft.machine.MachineBlockEntity;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** One shared transactional output budget for all faces, preserving the 60 FE/tick limit. */
final class GeneratorEnergy implements EnergyHandler {
    private final MachineBlockEntity machine;
    private final net.minecraft.core.Direction side;
    private final Journal journal;
    GeneratorEnergy(MachineBlockEntity machine,net.minecraft.core.Direction side,Journal journal){this.machine=machine;this.side=side;this.journal=journal;}
    static EnergyHandler of(MachineBlockEntity machine,net.minecraft.core.Direction side) {
        if(!machine.machineId().equals("thaumic_generator"))return null;
        if(machine.platformEnergy==null){
            var handlers=new GeneratorEnergy[7];
            var journal=new Journal(machine);
            for(var direction:net.minecraft.core.Direction.values())handlers[direction.ordinal()]=new GeneratorEnergy(machine,direction,journal);
            handlers[6]=new GeneratorEnergy(machine,null,journal);machine.platformEnergy=handlers;
        }
        return ((GeneratorEnergy[])machine.platformEnergy)[side==null?6:side.ordinal()];
    }
    @Override public long getAmountAsLong(){return machine.energy();}
    @Override public long getCapacityAsLong(){return machine.energyCapacity();}
    @Override public int insert(int amount,TransactionContext transaction){if(amount<0)throw new IllegalArgumentException("Negative energy");return 0;}
    @Override public int extract(int amount,TransactionContext transaction){
        if(amount<0)throw new IllegalArgumentException("Negative energy");
        if(machine.generatorOutput.available(amount)==0)return 0;
        journal.updateSnapshots(transaction);
        int taken=machine.generatorOutput.extract(amount);
        journal.extracted[side==null?6:side.ordinal()]+=taken;
        return taken;
    }
    /** Every face must snapshot the shared budget once per transaction, in one journal. */
    private static final class Journal extends SnapshotJournal<Journal.State> {
        record State(dev.thaumcraft.integration.GeneratorOutput.Snapshot output,int[] extracted) {}
        private final MachineBlockEntity machine;
        private int[] extracted=new int[7];
        Journal(MachineBlockEntity machine){this.machine=machine;}
        @Override protected State createSnapshot(){return new State(machine.generatorOutput.snapshot(),extracted.clone());}
        @Override protected void revertToSnapshot(State snapshot){machine.generatorOutput.restore(snapshot.output());extracted=snapshot.extracted();}
        @Override protected void onRootCommit(State original){
            boolean changed=false;
            for(int i=0;i<extracted.length;i++){
                int amount=extracted[i]-original.extracted()[i];
                if(amount>0){
                    changed=true;
                    if(i<6)machine.generatorOutput.transferred(machine.getBlockPos().relative(net.minecraft.core.Direction.values()[i]),amount);
                }
            }
            if(changed)machine.generatorOutput.committed();
        }
    }
}
