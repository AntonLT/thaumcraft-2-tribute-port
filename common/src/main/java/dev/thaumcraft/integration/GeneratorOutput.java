package dev.thaumcraft.integration;

import dev.thaumcraft.machine.MachineBlockEntity;
import dev.thaumcraft.machine.MachineLogic;

/** Shared output budget for optional adapters and loader capabilities, including transaction rollback. */
public final class GeneratorOutput {
    public record Snapshot(int energy,long tick,int emitted) {}
    private final MachineBlockEntity machine;
    private long tick=Long.MIN_VALUE;
    private int emitted;
    public GeneratorOutput(MachineBlockEntity machine){this.machine=machine;}
    public int available(int requested){
        var level=machine.getLevel();
        if(requested<=0||!machine.machineId().equals("thaumic_generator")||level==null||!machine.enabled()||level.hasNeighborSignal(machine.getBlockPos())||level.hasNeighborSignal(machine.getBlockPos().above()))return 0;
        int used=level.getGameTime()==tick?emitted:0;
        return Math.min(Math.min(requested,machine.energy()),Math.max(0,60*dev.thaumcraft.api.ThaumcraftApi.generatorEnergyMultiplier()-used));
    }
    public int extract(int requested){
        int amount=available(requested);if(amount==0)return 0;
        long now=machine.getLevel().getGameTime();if(tick!=now){tick=now;emitted=0;}
        emitted+=amount;machine.extractEnergy(amount,false);return amount;
    }
    public Snapshot snapshot(){return new Snapshot(machine.energy(),tick,emitted);}
    public void restore(Snapshot snapshot){machine.restoreEnergy(snapshot.energy());tick=snapshot.tick();emitted=snapshot.emitted();}
    public void transferred(net.minecraft.core.BlockPos target,int amount){
        if(amount<=0||!(machine.getLevel() instanceof net.minecraft.server.level.ServerLevel level))return;
        var event=new dev.thaumcraft.network.GeneratorArc(machine.getBlockPos(),target,amount);
        for(var player:level.players())if(player.distanceToSqr(machine.getBlockPos().getCenter())<4096)dev.thaumcraft.network.GeneratorArc.sender.accept(player,event);
    }
    public void committed(){machine.setChanged();MachineLogic.generatorEmitted(machine);}
}
