package dev.thaumcraft.machine;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.VoidNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;
import java.util.List;
import java.util.stream.IntStream;

public final class MachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SIZE=28, FUEL_SLOT=27, INPUTS=9, OUTPUT_START=9, OUTPUT_END=18, UPGRADE_START=18;
    private static final java.util.Set<String> VIS_PROCESSORS=java.util.Set.of("thaumic_infuser","dark_infuser","thaumic_crystalizer","thaumic_duplicator","thaumic_restorer");
    private NonNullList<ItemStack> items=NonNullList.withSize(SIZE,ItemStack.EMPTY);
    private UUID owner;
    private float pureVis,taintedVis;
    private float peakPureVis,peakTaintedVis,displayPureVis,displayTaintedVis;
    private int filterWisps,filterStack;
    private int visSuction,taintSuction;
    int filterStore;
    long visDemandUntil=Long.MIN_VALUE,taintDemandUntil=Long.MIN_VALUE;
    private boolean enabled=true;
    private boolean previousValvePower;
    private int previousValveSetting;
    private int energy;
    public int progress,workRequired=100;
    boolean sealWorked;
    public long sequence;
    private int counter;
    private int channel;
    private boolean visualDirty=true,visualWorking,visualPowered;
    private BlockPos visualDarknessMonolith;
    private float visualInfuserSucked;
    private boolean visualInfuserProcessing;
    private int visualUpgradeMask,visualFocus=-1,visualCrystal=-1;
    private int visualFace=3;
    private boolean visualPortalOpen,visualVoidLinked;
    private net.minecraft.core.GlobalPos visualPortalTarget;
    private net.minecraft.core.Direction visualPortalFacing=net.minecraft.core.Direction.NORTH;
    private final int[] visualRunes={-1,-1,-1};
    private Reservation reservation;
    private List<MachineBlockEntity> voidLinks=List.of();
    private long voidLinksTick=Long.MIN_VALUE;
    private int voidLinksRevision=-1;
    public final OcculticEnchanting enchanting=new OcculticEnchanting(this);
    public final BasicEnchanting basicEnchanting=new BasicEnchanting(this);
    public final MachineProcesses processes=new MachineProcesses(this);
    public final dev.thaumcraft.integration.GeneratorOutput generatorOutput=new dev.thaumcraft.integration.GeneratorOutput(this);

    public final ContainerData data=new ContainerData() {
        @Override public int get(int index) {
            return switch(index) {
                case 0 -> (int)(pureVis*10);case 1 -> (int)(taintedVis*10);case 2 -> (int)(capacity()*10);
                case 3 -> progress;case 4 -> workRequired;case 5 -> enabled?1:0;case 6 -> energy;
                case 7 -> BuiltInRegistries.BLOCK.getId(getBlockState().getBlock());
                case 24 -> processes.burnTime;case 25 -> processes.burnMax;case 26 -> (int)processes.degradation;case 27 -> processes.repeat?1:0;
                case 28,29 -> {var entry=Content.entry(items.get(18+index-28));yield entry!=null&&entry.source_class().equals("ItemUpgrades")?entry.meta()+1:0;}
                case 30 -> level instanceof ServerLevel server?MachineProcesses.moon(server):0;case 31 -> energyCapacity();
                case 32,33,34 -> basicEnchanting.offer(index-32);case 35 -> basicEnchanting.selected()+1;
                case 36 -> processes.boosted?1:0;case 37 -> enchanting.data(96);
                case 125 -> progress>>>16;case 126 -> workRequired>>>16;
                case 124 -> level==null?0:level.getMaxLocalRawBrightness(worldPosition);
                case 38 -> processes.boost;case 39 -> processes.darkProgress();
                case 40,41,42 -> machineId().equals("thaumic_enchanter")?basicEnchanting.cost(index-40):enchanting.data(index-24);
                case 120 -> researchOdds().success();
                case 121 -> researchOdds().failure();
                case 122 -> researchOdds().loss();
                case 123 -> researchOdds().theoryProgress();
                case 127 -> energy>>>16;case 128 -> energyCapacity()>>>16;
                default -> index>=40?enchanting.data(index-24):index<24?enchanting.data(index-8):0;
            };
        }
        @Override public void set(int index,int value) {}
        @Override public int getCount() {return MachineMenu.DATA_COUNT;}
    };

    private static final dev.thaumcraft.gameplay.ResearchLogic.Odds NO_ODDS=new dev.thaumcraft.gameplay.ResearchLogic.Odds(0,0,0,-1);
    private dev.thaumcraft.gameplay.ResearchLogic.Odds researchOdds;
    private long researchOddsTick;
    /**
     * Menu sync reads four odds slots per pass. Only the quaesitum screen shows them, so other machines report none.
     * The value is reused within a game tick until the inventory changes.
     */
    private dev.thaumcraft.gameplay.ResearchLogic.Odds researchOdds() {
        if(!machineId().equals("quaesitum"))return NO_ODDS;
        if(level==null)return dev.thaumcraft.gameplay.ResearchLogic.odds(this);
        if(researchOdds==null||researchOddsTick!=level.getGameTime()){researchOdds=dev.thaumcraft.gameplay.ResearchLogic.odds(this);researchOddsTick=level.getGameTime();}
        return researchOdds;
    }
    public MachineBlockEntity(BlockPos pos,BlockState state) {super(Content.MACHINE_ENTITY,pos,state);items=NonNullList.withSize(localSize(),ItemStack.EMPTY);if(machineId().equals("vis_valve"))enabled=false;}
    public String machineId() {return ((MachineBlock)getBlockState().getBlock()).id();}
    public void setOwner(UUID player) {owner=player;invalidateVoidLinks();setChanged();}
    public UUID owner() {return owner;}
    /**
     * A whole infusion batch accepted for an addon. While present, the batch owns the input and output slots: every
     * access path refuses them and only the pinned recipe definition may run. {@code sequence} marks completion.
     */
    public record Reservation(UUID token,net.minecraft.resources.Identifier recipe,String definition,long sequence) {
        public boolean pins(dev.thaumcraft.gameplay.GameData.Infusion infusion){
            return infusion.key().equals(recipe)&&definition.equals(java.util.Objects.requireNonNullElse(dev.thaumcraft.gameplay.AddonData.infusionSignature(infusion.id()),""));
        }
    }
    public Reservation reservation() {return reservation;}
    public void setReservation(Reservation reservation) {this.reservation=reservation;setChanged();}
    /** Slots a reservation owns; upgrades stay with the machine. */
    public boolean reservedSlot(int slot) {return reservation!=null&&slot<UPGRADE_START;}
    public boolean isVoidStorage() {return machineId().equals("void_chest") || machineId().equals("void_interface");}
    public boolean isStorage() {return isVoidStorage() || machineId().equals("traveling_trunk");}
    public int channel() {return channel;}
    public void setChannel(int channel) {this.channel=Math.clamp(channel,0,5);invalidateVoidLinks();setChanged();}
    private int localSize() {return machineId().equals("void_chest")?72:SIZE;}
    private void invalidateVoidLinks() {if(level instanceof ServerLevel server&&isVoidStorage())VoidNetworks.get(server).invalidate();}
    public List<MachineBlockEntity> linkedVoidChests() {
        if(!(level instanceof ServerLevel server)||!machineId().equals("void_interface"))return List.of();
        var network=VoidNetworks.get(server);
        if(voidLinksRevision!=network.revision()||server.getGameTime()-voidLinksTick>=20||voidLinksTick==Long.MIN_VALUE) {
            voidLinks=network.linkedChests(server,this);voidLinksTick=server.getGameTime();voidLinksRevision=network.revision();
        }
        return voidLinks;
    }
    private MachineBlockEntity voidTarget(int slot) {
        var linked=linkedVoidChests();int page=slot/72;
        if(slot<0||page>=linked.size())return null;
        var target=linked.get(page);
        var targetLevel=target.getLevel();
        return target.isRemoved()||targetLevel==null||!targetLevel.hasChunkAt(target.getBlockPos())||targetLevel.getBlockEntity(target.getBlockPos())!=target?null:target;
    }
    public int voidPages() {return machineId().equals("void_chest")?1:linkedVoidChests().size();}
    public float pureVis() {return pureVis;}
    public float taintedVis() {return taintedVis;}
    public float totalVis() {return pureVis+taintedVis;}
    public float displayPureVis() {return Math.max(pureVis,displayPureVis);}
    public float displayTaintedVis() {return Math.max(taintedVis,displayTaintedVis);}
    public int filterWisps() {return filterWisps;}
    public int filterStack() {return filterStack;}
    void emitFilterWisp(int stack) {filterStack=stack;filterWisps++;setChanged();}
    private boolean displaysConduitVis() {
        return switch(machineId()){case "vis_conduit","vis_filter","vis_valve","advanced_vis_valve" -> true;default -> false;};
    }
    private void updateVisDisplay() {
        if(!displaysConduitVis())return;
        // Chunk updates serialize later, so keep the published snapshot separate from the next interval's peaks.
        float pure=Math.max(pureVis,peakPureVis),tainted=Math.max(taintedVis,peakTaintedVis);
        if(displayPureVis!=pure||displayTaintedVis!=tainted)visualDirty=true;
        displayPureVis=pure;displayTaintedVis=tainted;
        peakPureVis=pureVis;peakTaintedVis=taintedVis;
    }
    public int visSuction() {return visSuction;}
    public int taintSuction() {return taintSuction;}
    public int suction() {return Math.max(visSuction,taintSuction);}
    void setSuction(int pure,int tainted) {
        if(visSuction!=pure||taintSuction!=tainted)visualDirty=true;
        visSuction=pure;taintSuction=tainted;
    }
    public int energy() {return energy;}
    public int energyCapacity(){return machineId().equals("thaumic_generator")?(upgrades(5)>0?40000:20000)*dev.thaumcraft.api.ThaumcraftApi.generatorEnergyMultiplier():30000;}
    public Object platformEnergy;
    public void restoreEnergy(int amount) {energy=Math.clamp(amount,0,energyCapacity());}
    public void setSensorSignal(int signal) {
        signal=Math.clamp(signal,0,15);
        if(machineId().equals("arcane_seal")&&energy!=signal){
            energy=signal;setChanged();
            if(level!=null){
                level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
                level.updateNeighborsAt(worldPosition.relative(getBlockState().getValue(MachineBlock.FACING).getOpposite()),getBlockState().getBlock());
            }
        }
    }
    public int extractEnergy(int amount,boolean simulate) {
        int extracted=Math.min(Math.max(amount,0),energy);
        if(!simulate && extracted>0){energy-=extracted;setChanged();}
        return extracted;
    }
    public void generateEnergy(int amount) {energy=(int)Math.clamp((long)energy+amount,0,energyCapacity());setChanged();}
    public boolean enabled() {
        if(level!=null&&level.isClientSide())return enabled;
        // Only valves are switched by redstone; other machines skip the neighbour scans.
        return enabled && (!machineId().contains("valve") || level==null || !(level.hasNeighborSignal(worldPosition)||level.hasNeighborSignal(worldPosition.above())));
    }
    /** Resend the client snapshot without marking saved state changed. */
    void markVisualDirty(){visualDirty=true;}
    public boolean toggle() {enabled=!enabled;if(!enabled)setSensorSignal(0);setChanged();return enabled;}
    public boolean isTotem(){return machineId().equals("totem_of_dawn")||machineId().equals("totem_of_dusk");}
    public int upgrades(int meta) {
        if(!supportsUpgrade(machineId(),meta))return 0;
        if(level!=null&&level.isClientSide())return (visualUpgradeMask>>meta)&1;
        int count=0;
        for(int i=UPGRADE_START;i<UPGRADE_START+upgradeLimit(machineId());i++) {
            var def=Content.entry(items.get(i));
            if(def!=null && def.legacy().endsWith("itemUpgrades") && def.meta()==meta) count++;
        }
        return Math.min(1,count);
    }
    public boolean installUpgrade(ItemStack stack) {
        for(int i=UPGRADE_START;i<UPGRADE_START+upgradeLimit(machineId());i++) if(items.get(i).isEmpty() && canPlaceItem(i,stack)) {
            items.set(i,stack.copyWithCount(1));setChanged();return true;
        }
        return false;
    }
    public static int upgradeLimit(String id) {
        return switch(id) {
            case "vis_condenser","thaumic_generator" -> 2;
            case "thaumic_infuser","dark_infuser","thaumic_restorer","thaumic_duplicator","thaumic_crystalizer","occultic_enchanter" -> 1;
            default -> 0;
        };
    }
    public static boolean supportsUpgrade(String id,int meta) {
        return switch(id) {
            case "vis_condenser","thaumic_crystalizer","dark_infuser" -> meta==0||meta==1||meta==3;
            case "thaumic_generator" -> meta==0||meta==1||meta==5;
            case "thaumic_infuser","thaumic_restorer","thaumic_duplicator" -> meta==0||meta==1;
            case "occultic_enchanter" -> meta==1||meta==5||meta==6;
            default -> false;
        };
    }
    public float capacity() {
        float base=switch(machineId()) {
            case "crucible" -> 500;case "crucible_of_eyes" -> 600;
            case "thaumium_crucible","crucible_of_souls" -> 750;
            case "arcane_furnace" -> 5;
            case "vis_condenser" -> 10;
            case "vis_storage_tank" -> 500;case "thaumium_reinforced_tank" -> 1000;
            case "vis_conduit","vis_filter","vis_valve","advanced_vis_valve","vis_pump","vis_purifier" -> 4;
            default -> 500;
        };
        return base*(1+upgrades(5)*0.5f);
    }
    public float insertVis(float amount,boolean tainted) {
        if(!Float.isFinite(amount)||amount<=0)return 0;
        float accepted=Math.min(amount,Math.max(0,capacity()-(machineId().equals("vis_condenser")?(tainted?taintedVis:pureVis):totalVis())));
        if(accepted>0){
            int signal=comparatorSignal();
            if(tainted)taintedVis+=accepted;else pureVis+=accepted;
            if(displaysConduitVis()){peakPureVis=Math.max(peakPureVis,pureVis);peakTaintedVis=Math.max(peakTaintedVis,taintedVis);}
            visChanged(signal);
        }
        return accepted;
    }
    void addCrucibleVis(float pure,float taint) {
        if(Float.isFinite(pure)&&Float.isFinite(taint)&&pure>=0&&taint>=0){int signal=comparatorSignal();pureVis+=pure;taintedVis+=taint;visChanged(signal);}
    }
    public float extractVis(float amount,boolean tainted) {
        if(!Float.isFinite(amount)||amount<=0)return 0;
        float extracted=Math.min(amount,tainted?taintedVis:pureVis);
        if(extracted>0){int signal=comparatorSignal();if(tainted)taintedVis-=extracted;else pureVis-=extracted;visChanged(signal);}
        return extracted;
    }
    /** The reading comparators take from this machine. */
    public int comparatorSignal() {return Math.min(15,(int)(15*totalVis()/capacity()));}
    /**
     * Vis moves on most ticks. Unlike {@link #setChanged()}, wake comparators only when their reading changes;
     * they ignore every other notification. The chunk is still marked for saving on each change.
     */
    private void visChanged(int previousSignal) {
        if(refillingVis)return;
        visualDirty=true;
        if(level==null)return;
        level.blockEntityChanged(worldPosition);
        if(comparatorSignal()!=previousSignal)level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
    }
    private boolean refillingVis;
    /**
     * Empties both buffers and inserts the given amounts with the usual rules, as tank and condenser balancing does.
     * Counts as a change only when the amounts end up different, so a settled tank stays clean.
     */
    void refillVis(float pure,float tainted) {
        float previousPure=pureVis,previousTainted=taintedVis;int signal=comparatorSignal();
        refillingVis=true;
        try {extractVis(pureVis,false);extractVis(taintedVis,true);insertVis(pure,false);insertVis(tainted,true);}
        finally {refillingVis=false;}
        if(pureVis!=previousPure||taintedVis!=previousTainted)visChanged(signal);
    }
    public boolean spendVis(float amount,boolean tainted) {
        if(amount<0||!Float.isFinite(amount))return false;
        if((tainted?taintedVis:pureVis)+0.0001f<amount)return false;
        extractVis(amount,tainted);return true;
    }

    public boolean output(ItemStack stack,boolean simulate) {
        int remaining=stack.getCount();
        for(int pass=0;pass<2;pass++) for(int i=OUTPUT_START;i<MachineLayout.get(machineId()).outputEnd();i++) {
            ItemStack current=items.get(i);
            if((pass==0 && !current.isEmpty() && ItemStack.isSameItemSameComponents(current,stack)) || (pass==1 && current.isEmpty())) {
                int added=Math.min(remaining,stack.getMaxStackSize()-current.getCount());
                if(added>0 && !simulate) {
                    if(current.isEmpty())items.set(i,stack.copyWithCount(added));else current.grow(added);
                }
                remaining-=Math.max(0,added);
                if(remaining==0){if(!simulate)setChanged();return true;}
            }
        }
        return false;
    }
    public boolean outputAt(int slot,ItemStack stack,boolean simulate){
        ItemStack current=items.get(slot);
        if(!current.isEmpty()&&!ItemStack.isSameItemSameComponents(current,stack)||current.getCount()+stack.getCount()>stack.getMaxStackSize())return false;
        if(!simulate){if(current.isEmpty())items.set(slot,stack.copy());else current.grow(stack.getCount());setChanged();}return true;
    }
    public void outputOrDrop(ItemStack stack) {
        if(stack.isEmpty())return;
        if(output(stack,true))output(stack,false);
        else if(level!=null)Containers.dropItemStack(level,worldPosition.getX()+0.5,worldPosition.getY()+1,worldPosition.getZ()+0.5,stack.copy());
    }
    public NonNullList<ItemStack> inventory() {return getItems();}
    public void consumeInput(int slot,int count) {
        ItemStack stack=items.get(slot);
        var remainder=stack.getItem().getCraftingRemainder();
        stack.shrink(count);
        if(remainder!=null) for(int i=0;i<count;i++) outputOrDrop(remainder.create());
        setChanged();
    }

    public static void tick(Level level,BlockPos pos,BlockState state,MachineBlockEntity machine) {
        if(!(level instanceof ServerLevel server))return;
        machine.counter++;
        if(machine.machineId().equals("vis_valve")||machine.machineId().equals("advanced_vis_valve")){
            boolean powered=level.hasNeighborSignal(pos)||level.hasNeighborSignal(pos.above());
            int oldChannel=machine.channel;boolean oldEnabled=machine.enabled;
            if(machine.machineId().equals("advanced_vis_valve")){
                if(powered){if(!machine.previousValvePower)machine.previousValveSetting=machine.channel;machine.channel=0;}
                else if(machine.previousValvePower)machine.channel=machine.previousValveSetting;
            }else if(powered)machine.enabled=false;
            else if(machine.previousValvePower)machine.enabled=true;
            if(powered!=machine.previousValvePower||oldChannel!=machine.channel||oldEnabled!=machine.enabled)machine.setChanged();
            machine.previousValvePower=powered;
        }
        VisNetwork.tick(server,machine);
        BlockPos previousDarkness=machine.visualDarknessMonolith;
        machine.processes.tick(server);
        boolean researchVisualChanged=machine.machineId().equals("quaesitum")&&machine.visualWorking!=machine.processes.researchWorked;
        if(researchVisualChanged){
            machine.visualWorking=machine.processes.researchWorked;machine.visualDirty=true;
        }
        if(machine.machineId().equals("arcane_seal"))SealLogic.tick(server,machine);
        if(machine.machineId().equals("thaumic_generator"))MachineLogic.generator(server,machine);
        if(machine.machineId().equals("darkness_generator"))MachineLogic.darkness(server,machine);
        if(machine.machineId().equals("arcane_bore"))MachineLogic.bore(server,machine);
        if(machine.machineId().contains("crucible"))MachineLogic.crucible(server,machine);
        if(machine.counter%5==0){
            machine.updateVisDisplay();
            int oldProgress=machine.progress;float oldVis=machine.totalVis();int oldEnergy=machine.energy;
            MachineLogic.tick(server,machine);
            boolean working=machine.enabled()&&(machine.progress!=oldProgress||machine.totalVis()<oldVis||machine.energy!=oldEnergy);
            if(machine.machineId().equals("brazier_of_souls"))working=machine.processes.burnTime>0&&!level.hasNeighborSignal(pos);
            if(machine.machineId().equals("arcane_furnace"))working=machine.processes.burnTime>0&&(machine.progress>0||level.getBlockEntity(pos.above()) instanceof MachineBlockEntity above&&above.machineId().contains("crucible"))&&!level.hasNeighborSignal(pos);
            if(VIS_PROCESSORS.contains(machine.machineId()))working=machine.progress>0&&machine.enabled()&&!level.hasNeighborSignal(pos);
            if(machine.machineId().equals("thaumic_enchanter"))working=machine.basicEnchanting.selected()>=0;
            if(machine.machineId().equals("quaesitum"))working=machine.processes.researchWorked;
            if(machine.machineId().equals("arcane_bore"))working=machine.enabled()&&machine.energy()>0&&(level.hasNeighborSignal(pos)||level.hasNeighborSignal(pos.above()));
            boolean powered=level.hasNeighborSignal(pos);
            if(machine.machineId().equals("arcane_bore"))powered|=level.hasNeighborSignal(pos.above());
            if(machine.visualWorking!=working||machine.visualPowered!=powered)machine.visualDirty=true;
            machine.visualWorking=working;machine.visualPowered=powered;
            if((machine.machineId().equals("arcane_furnace")||machine.machineId().equals("brazier_of_souls"))&&state.getValue(MachineBlock.LIT)!=working){state=state.setValue(MachineBlock.LIT,working);level.setBlock(pos,state,Block.UPDATE_CLIENTS);}
        }
        // Infuser absorption can start and finish between the shared five-tick visual updates.
        if(machine.visualDirty&&(machine.machineId().equals("thaumic_infuser")||researchVisualChanged||!java.util.Objects.equals(previousDarkness,machine.visualDarknessMonolith)||machine.counter%5==0)){
            // Clients that already hold this exact snapshot gain nothing from another update.
            if(!machine.visualTag().equals(machine.broadcastVisuals))level.sendBlockUpdated(pos,state,state,Block.UPDATE_CLIENTS);
            machine.visualDirty=false;
        }
        if(machine.counter%20==0)level.updateNeighbourForOutputSignal(pos,state.getBlock());
    }

    @Override protected void loadAdditional(ValueInput input) {
        if(input.getBooleanOr("visual_only",false)){
            pureVis=sanitize(input.getFloatOr("pure_vis",0));taintedVis=sanitize(input.getFloatOr("tainted_vis",0));
            displayPureVis=sanitize(input.getFloatOr("display_pure_vis",pureVis));displayTaintedVis=sanitize(input.getFloatOr("display_tainted_vis",taintedVis));
            filterWisps=input.getIntOr("filter_wisps",0);filterStack=input.getIntOr("filter_stack",0);
        setSuction(input.getIntOr("vis_suction",0),input.getIntOr("taint_suction",0));filterStore=input.getIntOr("filter_store",0);
            progress=input.getIntOr("progress",0);workRequired=input.getIntOr("work_required",100);energy=input.getIntOr("energy",0);
            enabled=input.getBooleanOr("enabled",true);visualWorking=input.getBooleanOr("working",false);visualPowered=input.getBooleanOr("powered",false);
            visualUpgradeMask=input.getIntOr("upgrades",0);visualFocus=input.getIntOr("focus",-1);channel=input.getIntOr("channel",0);
            visualCrystal=input.getIntOr("crystal",-1);visualPortalOpen=input.getBooleanOr("portal_open",false);visualVoidLinked=input.getBooleanOr("void_linked",false);
            visualFace=input.getIntOr("face",3);processes.degradation=input.getFloatOr("crystal_degradation",0);processes.speed=input.getFloatOr("condenser_speed",0);processes.condenserWisps=input.getIntOr("condenser_wisps",0);processes.crucibleWisps=input.getIntOr("crucible_wisps",0);visualInfuserSucked=sanitize(input.getFloatOr("infuser_sucked",0));
            visualInfuserProcessing=input.getBooleanOr("infuser_processing",false);
            visualDarknessMonolith=input.read("darkness_monolith",BlockPos.CODEC).orElse(null);
            visualPortalTarget=input.read("portal_target",net.minecraft.core.GlobalPos.CODEC).orElse(null);
            visualPortalFacing=net.minecraft.core.Direction.from3DDataValue(input.getIntOr("portal_facing",2));
            for(int i=0;i<3;i++)visualRunes[i]=input.getIntOr("rune"+i,-1);
            return;
        }
        super.loadAdditional(input);
        items=NonNullList.withSize(localSize(),ItemStack.EMPTY);ContainerHelper.loadAllItems(input,items);
        pureVis=sanitize(input.getFloatOr("pure_vis",0));taintedVis=sanitize(input.getFloatOr("tainted_vis",0));
        setSuction(input.getIntOr("vis_suction",0),input.getIntOr("taint_suction",0));filterStore=input.getIntOr("filter_store",0);
        progress=Math.max(0,input.getIntOr("progress",0));workRequired=Math.max(1,input.getIntOr("work_required",100));
        if(machineId().equals("arcane_bore")){progress=0;processes.boreDelay=4;}
        long savedEnergy=input.getIntOr("energy",0);
        if(machineId().equals("thaumic_generator")&&!input.getBooleanOr("generator_energy_fe",false))savedEnergy*=4;
        energy=(int)Math.clamp(savedEnergy,0,machineId().equals("thaumic_generator")?40000*dev.thaumcraft.api.ThaumcraftApi.generatorEnergyMultiplier():30000);enabled=input.getBooleanOr("enabled",true);
        // Original seal saves runes/orientation/window, not its transient action state. The saved detector signal stays
        // until the first scan so that scan's change notifies redstone that was saved powered.
        if(machineId().equals("arcane_seal")){progress=0;energy=Math.clamp(energy,0,15);sealWorked=false;}
        sequence=input.getLongOr("sequence",0);
        channel=Math.clamp(input.getIntOr("channel",0),0,5);
        previousValveSetting=Math.clamp(input.getIntOr("previous_valve_setting",channel),0,2);
        previousValvePower=input.getBooleanOr("previous_valve_power",false);
        enchanting.load(input);basicEnchanting.load(input);processes.load(input);
        reservation=input.getString("reservation_token").flatMap(token->{
            try {return java.util.Optional.of(new Reservation(UUID.fromString(token),net.minecraft.resources.Identifier.parse(input.getStringOr("reservation_recipe","")),input.getStringOr("reservation_definition",""),input.getLongOr("reservation_sequence",0)));}
            catch(RuntimeException invalid) {return java.util.Optional.<Reservation>empty();}
        }).orElse(null);
        String uuid=input.getStringOr("owner","");
        try {owner=uuid.isEmpty()?null:UUID.fromString(uuid);} catch(IllegalArgumentException ignored) {owner=null;}
    }
    private float sanitize(float value) {return Float.isFinite(value)?Math.clamp(value,0,3000):0;}
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);ContainerHelper.saveAllItems(output,items);
        output.putInt("filter_store",filterStore);
        output.putFloat("pure_vis",pureVis);output.putFloat("tainted_vis",taintedVis);
        output.putInt("progress",progress);output.putInt("work_required",workRequired);output.putInt("energy",energy);
        if(machineId().equals("thaumic_generator"))output.putBoolean("generator_energy_fe",true);
        output.putBoolean("enabled",enabled);output.putLong("sequence",sequence);
        output.putInt("channel",channel);
        if(machineId().equals("advanced_vis_valve")){output.putInt("previous_valve_setting",previousValveSetting);output.putBoolean("previous_valve_power",previousValvePower);}
        enchanting.save(output);basicEnchanting.save(output);processes.save(output);
        if(owner!=null)output.putString("owner",owner.toString());
        if(reservation!=null){
            output.putString("reservation_token",reservation.token().toString());output.putString("reservation_recipe",reservation.recipe().toString());
            output.putString("reservation_definition",reservation.definition());output.putLong("reservation_sequence",reservation.sequence());
        }
    }
    @Override public void setRemoved() {
        processes.clearBoreAttraction();
        super.setRemoved();
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state) {
        processes.clearBoreAttraction();
        if(level instanceof ServerLevel server) {
            // Original upgrades and seal runes are machine state, not inventory drops when the block breaks.
            int installed=machineId().equals("arcane_seal")?3:upgradeLimit(machineId());
            for(int slot=UPGRADE_START;slot<UPGRADE_START+installed;slot++)items.set(slot,ItemStack.EMPTY);
            // A detector powers through its support block; let the support's neighbours see the signal end.
            if(machineId().equals("arcane_seal")&&energy>0){energy=0;level.updateNeighborsAt(pos,state.getBlock());level.updateNeighborsAt(pos.relative(state.getValue(MachineBlock.FACING).getOpposite()),state.getBlock());}
            dev.thaumcraft.world.ChunkAnchors.remove(server,pos);
            int spilled=0;
            if(machineId().contains("crucible")||java.util.Set.of("vis_conduit","vis_filter","vis_storage_tank","vis_valve","vis_purifier","advanced_vis_valve","thaumium_reinforced_tank").contains(machineId()))spilled=(int)taintedVis;
            if(machineId().equals("vis_condenser")&&processes.degradation>0)spilled=(int)(25*(4550-processes.degradation)/4550);
            if(spilled>0){dev.thaumcraft.gameplay.ArcaneWorldData.get(server).changeAura(server,pos,0,spilled);server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,Math.min(spilled,50),.5,.5,.5,.02);}
        }
        if(level instanceof ServerLevel server&&machineId().equals("arcane_seal"))dev.thaumcraft.world.SealPortals.remove(server,pos);
        if(level instanceof ServerLevel server&&isVoidStorage()){VoidNetworks.get(server).remove(server,pos);VoidNetworks.get(server).invalidate();}
        if(level!=null && !machineId().equals("void_interface")){Containers.dropContents(level,pos,this);level.updateNeighbourForOutputSignal(pos,state.getBlock());}
        // BlockEntity also drops Container contents; do not invoke it twice or drain a shared void inventory.
    }
    @Override protected NonNullList<ItemStack> getItems() {
        if(level instanceof ServerLevel server && machineId().equals("void_chest") && VoidNetworks.get(server).migrate(owner,channel,items))super.setChanged();
        return items;
    }
    @Override protected void setItems(NonNullList<ItemStack> items) {this.items=items;}
    @Override public void setChanged() {
        super.setChanged();
        visualDirty=true;researchOdds=null;
        if(level instanceof ServerLevel && machineId().equals("void_interface"))for(var chest:linkedVoidChests())chest.setChanged();
    }
    @Override public int getContainerSize() {return machineId().equals("void_interface")?Math.max(72,voidPages()*72):localSize();}
    public BlockPos visualDarknessMonolith(){return visualDarknessMonolith;}
    public void setVisualDarknessMonolith(BlockPos source){if(!java.util.Objects.equals(visualDarknessMonolith,source)){visualDarknessMonolith=source==null?null:source.immutable();visualDirty=true;}}
    public boolean visualWorking(){return visualWorking;}
    public float visualInfuserSucked(){return visualInfuserSucked;}
    public boolean visualInfuserProcessing(){return visualInfuserProcessing;}
    public int visualFace(){return visualFace;}
    public void showSoulAbsorption(){visualFace=0;visualDirty=true;}
    public void advanceSoulFace(){if(visualFace<3){visualFace++;visualDirty=true;}}
    public boolean visualPowered(){return visualPowered;}
    public boolean visualVoidLinked(){return level!=null&&level.isClientSide()?visualVoidLinked:voidPages()>1;}
    public int visualCrystal(){if(level!=null&&level.isClientSide())return visualCrystal;if(machineId().equals("vis_condenser"))return processes.crystalType;var def=Content.entry(items.get(0));return def!=null&&def.legacy().endsWith("itemCrystals")&&def.meta()!=6?def.meta():-1;}
    public boolean visualPortalOpen(){return visualPortalOpen&&enabled()&&visualRune(0)==0&&visualRune(1)==1;}
    public void setVisualPortalOpen(boolean open){if(visualPortalOpen!=open){visualPortalOpen=open;visualDirty=true;}}
    public net.minecraft.core.GlobalPos visualPortalTarget(){return visualPortalOpen()?visualPortalTarget:null;}
    public net.minecraft.core.Direction visualPortalFacing(){return visualPortalFacing;}
    public void setVisualPortalTarget(net.minecraft.core.GlobalPos target,net.minecraft.core.Direction facing){
        if(!java.util.Objects.equals(visualPortalTarget,target)||visualPortalFacing!=facing){visualPortalTarget=target;visualPortalFacing=facing;visualDirty=true;}
        setVisualPortalOpen(target!=null);
    }
    public int visualFocus(){if(level!=null&&level.isClientSide())return visualFocus;var def=Content.entry(items.get(0));return def==null?-1:def.focusType();}
    public int visualRune(int index){if(level!=null&&level.isClientSide())return visualRunes[index];var def=Content.entry(items.get(18+index));return def!=null&&def.source_class().equals("ItemRunicEssence")?def.meta():-1;}
    /** The snapshot every tracking player received last; null once anyone may hold a different one. */
    private net.minecraft.nbt.CompoundTag broadcastVisuals;
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){
        // Block-change broadcasts reach every player tracking this chunk.
        var tag=visualTag();broadcastVisuals=tag;
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this,(entity,registries)->tag);
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries){
        // Chunk packets for newly tracking players (or other readers) can hold a newer snapshot than the last broadcast.
        broadcastVisuals=null;
        return visualTag();
    }
    private net.minecraft.nbt.CompoundTag visualTag(){
        var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("visual_only",true);
        tag.putInt("vis_suction",visSuction);tag.putInt("taint_suction",taintSuction);
        tag.putFloat("pure_vis",pureVis);tag.putFloat("tainted_vis",taintedVis);tag.putInt("progress",progress);tag.putInt("work_required",workRequired);tag.putInt("energy",energy);
        if(displaysConduitVis()){tag.putFloat("display_pure_vis",displayPureVis());tag.putFloat("display_tainted_vis",displayTaintedVis());}
        if(machineId().equals("vis_filter")){tag.putInt("filter_wisps",filterWisps);tag.putInt("filter_stack",filterStack);}
        tag.putBoolean("enabled",enabled());tag.putBoolean("working",visualWorking);tag.putBoolean("powered",visualPowered);tag.putInt("channel",channel);
        int mask=0;for(int i=0;i<8;i++)if(upgrades(i)>0)mask|=1<<i;tag.putInt("upgrades",mask);tag.putInt("focus",visualFocus());
        tag.putInt("crystal",visualCrystal());tag.putFloat("crystal_degradation",processes.degradation);tag.putFloat("condenser_speed",processes.speed);tag.putInt("condenser_wisps",processes.condenserWisps);tag.putInt("crucible_wisps",processes.crucibleWisps);tag.putFloat("infuser_sucked",processes.infuserAbsorbed);tag.putBoolean("portal_open",visualPortalOpen());
        tag.putBoolean("infuser_processing",processes.infuserProcessing);
        if(visualDarknessMonolith!=null)tag.store("darkness_monolith",BlockPos.CODEC,visualDarknessMonolith);
        tag.putInt("face",visualFace);tag.putBoolean("void_linked",voidPages()>1);
        if(visualPortalTarget!=null)tag.store("portal_target",net.minecraft.core.GlobalPos.CODEC,visualPortalTarget);
        tag.putInt("portal_facing",visualPortalFacing.get3DDataValue());
        for(int i=0;i<3;i++)tag.putInt("rune"+i,visualRune(i));return tag;
    }
    @Override public ItemStack getItem(int slot) {
        if(!machineId().equals("void_interface"))return super.getItem(slot);
        var target=voidTarget(slot);return target==null?ItemStack.EMPTY:target.getItem(slot%72);
    }
    @Override public ItemStack removeItem(int slot,int amount) {
        if(!machineId().equals("void_interface"))return super.removeItem(slot,amount);
        var target=voidTarget(slot);return target==null?ItemStack.EMPTY:target.removeItem(slot%72,amount);
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if(!machineId().equals("void_interface"))return super.removeItemNoUpdate(slot);
        var target=voidTarget(slot);return target==null?ItemStack.EMPTY:target.removeItemNoUpdate(slot%72);
    }
    @Override public void setItem(int slot,ItemStack stack) {
        if(!machineId().equals("void_interface")){super.setItem(slot,stack);return;}
        var target=voidTarget(slot);if(target!=null)target.setItem(slot%72,stack);
    }
    @Override public boolean isEmpty() {
        if(!machineId().equals("void_interface"))return super.isEmpty();
        for(int slot=0;slot<getContainerSize();slot++)if(!getItem(slot).isEmpty())return false;
        return true;
    }
    @Override public void clearContent() {
        if(!machineId().equals("void_interface")){super.clearContent();return;}
        for(var chest:linkedVoidChests())chest.clearContent();
    }
    @Override protected Component getDefaultName() {return getBlockState().getBlock().getName();}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory) {
        if(isVoidStorage())return new VoidMenu(id,inventory,this);
        if(isStorage())return ChestMenu.threeRows(id,inventory,this);
        return new MachineMenu(id,inventory,this,data);
    }
    @Override public int[] getSlotsForFace(Direction face) {
        if(isStorage())return IntStream.range(0,getContainerSize()).toArray();
        boolean vertical=face.getAxis().isVertical();
        return switch(machineId()) {
            case "arcane_furnace" -> face==Direction.DOWN?new int[]{FUEL_SLOT}:face==Direction.UP?IntStream.range(9,18).toArray():IntStream.range(0,9).toArray();
            case "arcane_bore" -> new int[]{vertical?1:0};
            case "vis_condenser" -> new int[]{face==Direction.DOWN?9:0};
            case "darkness_generator" -> new int[]{vertical?0:9};
            case "thaumic_crystalizer" -> vertical?new int[]{0}:IntStream.range(9,15).toArray();
            case "thaumic_duplicator" -> vertical?new int[]{0}:IntStream.range(9,18).toArray();
            case "thaumic_infuser" -> vertical?IntStream.range(0,6).toArray():new int[]{9,10};
            case "dark_infuser" -> vertical?IntStream.range(0,5).toArray():new int[]{9};
            case "thaumic_restorer" -> vertical?IntStream.range(0,6).toArray():IntStream.range(9,15).toArray();
            case "thaumic_enchanter","occultic_enchanter","brazier_of_souls" -> new int[]{0};
            case "quaesitum" -> face==Direction.DOWN?IntStream.range(9,18).toArray():IntStream.range(0,4).toArray();
            default -> new int[0];
        };
    }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face) {return !reservedSlot(slot)&&(isStorage() || slot<INPUTS||slot==FUEL_SLOT)&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face) {
        if(reservedSlot(slot))return false;
        // Storage exposes every slot; building that array for each slot a hopper checks is quadratic.
        if(isStorage())return slot>=0&&slot<getContainerSize();
        for(int exposed:getSlotsForFace(face))if(exposed==slot)return true;
        return false;
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {
        if(machineId().equals("void_interface"))return voidTarget(slot)!=null;
        if(isStorage())return true;
        if(slot<INPUTS||slot==FUEL_SLOT)return MachineInputs.accepts(machineId(),slot,stack,level);
        if(slot>=UPGRADE_START) {
            var entry=Content.entry(stack);
            if(entry==null)return false;
            if(machineId().equals("arcane_seal"))return slot<UPGRADE_START+3&&entry.source_class().equals("ItemRunicEssence");
            if(slot>=UPGRADE_START+upgradeLimit(machineId())||!entry.source_class().equals("ItemUpgrades")||!supportsUpgrade(machineId(),entry.meta()))return false;
            for(int i=UPGRADE_START;i<UPGRADE_START+upgradeLimit(machineId());i++) {
                var installed=Content.entry(items.get(i));
                if(i!=slot&&installed!=null&&installed.source_class().equals("ItemUpgrades")&&installed.meta()==entry.meta())return false;
            }
            return true;
        }
        return false;
    }
}
