package dev.thaumcraft.machine;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persisted process state displayed by the original machine GUIs. Tick units are game ticks. */
public final class MachineProcesses {
    private static final java.util.Set<String> BOOSTED_PROCESSORS=java.util.Set.of("thaumic_crystalizer","thaumic_duplicator","thaumic_restorer");
    private final MachineBlockEntity machine;
    public int burnTime,burnMax,boost,crystalType=-1,pendingCrystal=-1;
    public int infuserBoostDelay=20;
    int boreDelay=4;
    final java.util.List<net.minecraft.world.entity.item.ItemEntity> boreItems=new java.util.ArrayList<>();
    public void clearBoreAttraction(){for(var item:boreItems){item.noPhysics=false;((dev.thaumcraft.mixin.ItemEntityAccess)item).thaumcraft$setBoreAttracted(false);}boreItems.clear();}
    int soundDelay;
    int crucibleDelay;
    boolean cruciblePower;
    public float pureWork,darkWork,pureCost,darkCost;
    public float infuserAbsorbed;
    public boolean infuserProcessing;
    public boolean researchWorked;
    private net.minecraft.core.BlockPos cachedDarknessMonolith;
    private long darknessRetry=Long.MIN_VALUE;
    public String recipeKey="";
    private String infusionDefinition="";
    public void rememberInfusion(dev.thaumcraft.gameplay.GameData.Infusion recipe){recipeKey=recipe.id();infusionDefinition=java.util.Objects.requireNonNullElse(dev.thaumcraft.gameplay.AddonData.infusionSignature(recipeKey),"");}
    public void validateInfusionWork(){
        if(recipeKey.isEmpty())return;
        String current=dev.thaumcraft.gameplay.AddonData.infusionSignature(recipeKey);
        if(!infusionDefinition.isEmpty()&&!infusionDefinition.equals(current)){clearWork();return;}
        if(current!=null&&infusionDefinition.isEmpty()){infusionDefinition=current;machine.setChanged();}
        if(current==null&&dev.thaumcraft.gameplay.GameData.infusions().stream().noneMatch(r->r.result().id().equals(recipeKey)))clearWork();
    }
    public float degradation,speed,condenseProgress;
    public boolean repeat,boosted;
    private int delay,urnCountdown=33;
    private long condenserDeadline;
    public int condenserWisps;
    public int crucibleWisps;
    MachineProcesses(MachineBlockEntity machine){this.machine=machine;}
    public static int moon(ServerLevel level){return level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE,net.minecraft.world.phys.Vec3.ZERO).index();}
    public void tick(ServerLevel level){
        if(soundDelay>0)soundDelay--;
        researchWorked=false;
        if(machine.machineId().equals("thaumic_infuser")) {
            if(machine.enabled())MachineLogic.tickVisProcess(level,machine);
            else if(infuserAbsorbed!=0||infuserProcessing){infuserAbsorbed=0;infuserProcessing=false;machine.setChanged();}
            // The original applies acquisition/decay after this tick's crafting rate.
            if(infuserBoostDelay<=0||infuserBoostDelay==10) {
                if(boost<10&&ArcaneWorldData.get(level).changeBoost(level,machine.getBlockPos(),-1)<0){boost++;machine.setChanged();}
            }
            if(infuserBoostDelay<=0) {
                if(boost>0){boost--;machine.setChanged();}
                infuserBoostDelay=20;
            } else infuserBoostDelay--;
            return;
        }
        if(BOOSTED_PROCESSORS.contains(machine.machineId())){
            if(level.getGameTime()%10==0&&boost<10&&ArcaneWorldData.get(level).changeBoost(level,machine.getBlockPos(),-1)<0){boost++;machine.setChanged();}
            if(level.getGameTime()%20==0&&boost>0){boost--;machine.setChanged();}
        }
        if(!machine.enabled())return;
        switch(machine.machineId()){
            case "everfull_urn" -> {if(--urnCountdown==0){urnCountdown=33;level.playSound(null,machine.getBlockPos(),net.minecraft.sounds.SoundEvents.WATER_AMBIENT,net.minecraft.sounds.SoundSource.BLOCKS,level.getRandom().nextFloat()*.2f+.2f,level.getRandom().nextFloat()+.5f);}}
            case "thaumic_duplicator","thaumic_restorer" -> {if(!level.hasNeighborSignal(machine.getBlockPos()))MachineLogic.tickVisProcess(level,machine);}
            case "thaumic_crystalizer","dark_infuser" -> MachineLogic.tickVisProcess(level,machine);
            case "quaesitum" -> {
                researchWorked=dev.thaumcraft.gameplay.ResearchLogic.tick(level,machine);
                if(researchWorked&&soundDelay==0){
                    var pos=machine.getBlockPos();
                    dev.thaumcraft.content.ModSounds.playAt(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,"scribble",net.minecraft.sounds.SoundSource.BLOCKS,.1f,1);
                    soundDelay=110;
                }
            }
            case "thaumic_enchanter" -> machine.basicEnchanting.tick(level);
            case "occultic_enchanter" -> machine.enchanting.tick();
            case "arcane_furnace" -> furnace(level);
            case "vis_condenser" -> {
                long now=System.currentTimeMillis();
                if(!level.hasNeighborSignal(machine.getBlockPos())&&condenserDeadline<now){condenser(level);condenserDeadline=System.currentTimeMillis()+100L;}
            }
            case "brazier_of_souls" -> {if(!level.hasNeighborSignal(machine.getBlockPos()))brazier(level);}
            default -> {}
        }
    }
    public net.minecraft.core.BlockPos darknessMonolith(ServerLevel level){
        var origin=machine.getBlockPos();long now=level.getGameTime();
        if(cachedDarknessMonolith!=null){
            var pos=cachedDarknessMonolith;
            if(Math.abs(pos.getX()-origin.getX())<=5&&Math.abs(pos.getY()-origin.getY())<=5&&Math.abs(pos.getZ()-origin.getZ())<=5
                    &&level.hasChunkAt(pos)&&level.getBlockState(pos).is(Content.block("eldritch_monolith")))return pos;
            cachedDarknessMonolith=null;
        }else if(now<darknessRetry)return null;
        darknessRetry=now+20;
        for(var pos:net.minecraft.core.BlockPos.betweenClosed(origin.offset(-5,-5,-5),origin.offset(5,5,5))){
            if(level.hasChunkAt(pos)&&level.getBlockState(pos).is(Content.block("eldritch_monolith"))){cachedDarknessMonolith=pos.immutable();return cachedDarknessMonolith;}
        }
        return null;
    }
    private int bellows(ServerLevel level){return VisNetwork.bellows(level,machine);}
    private void nextCook(ServerLevel level,boolean initial){boosted=machine.spendVis(.25f,false);machine.workRequired=Math.max(1,(int)((boosted?100:180)*(1-bellows(level)*(initial?.1f:.2f))));}
    private void consumeFuel(){
        ItemStack fuel=machine.getItem(MachineBlockEntity.FUEL_SLOT);
        var remainder=fuel.get(DataComponents.USE_REMAINDER);
        ItemStack empty=fuel.is(Items.LAVA_BUCKET)?new ItemStack(Items.BUCKET):remainder==null?ItemStack.EMPTY:remainder.convertInto().create();
        fuel.shrink(1);machine.setChanged();
        if(!empty.isEmpty()){if(machine.getItem(MachineBlockEntity.FUEL_SLOT).isEmpty())machine.setItem(MachineBlockEntity.FUEL_SLOT,empty);else machine.outputOrDrop(empty);}
    }
    private void furnace(ServerLevel level){
        VisNetwork.pull(level,machine,Math.max(0,5-machine.pureVis()),false);
        if(level.hasNeighborSignal(machine.getBlockPos()))return;
        ItemStack output=ItemStack.EMPTY;int slot=-1;
        for(int i=0;i<9;i++){
            var input=new SingleRecipeInput(machine.getItem(i));if(input.item().isEmpty())continue;
            var recipe=level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level);
            if(recipe.isPresent()){var result=recipe.get().value().assemble(input);if(machine.output(result,true)){output=result;slot=i;break;}}
        }
        String above=level.getBlockEntity(machine.getBlockPos().above()) instanceof MachineBlockEntity m?m.machineId():"";
        boolean heating=above.contains("crucible");
        if(burnTime>0&&(machine.progress>0||heating)){burnTime--;machine.setChanged();}
        if(burnTime==0&&(slot>=0||heating)){
            burnMax=burnTime=MachineInputs.fuelTicks(machine.getItem(MachineBlockEntity.FUEL_SLOT),level);
            if(burnTime>0){if(machine.spendVis(burnMax/1600f,false))burnMax=burnTime=(int)(burnTime*1.25f);consumeFuel();nextCook(level,true);machine.setChanged();}
        }
        if(burnTime>0&&slot>=0){
            machine.progress++;machine.setChanged();
            if(machine.progress>=machine.workRequired){
                machine.progress=0;nextCook(level,false);machine.output(output,false);
                int bellows=bellows(level);
                while(level.getRandom().nextInt(90)<5+bellows*7&&machine.output(output,true)&&machine.spendVis(.5f,false)){machine.output(output,false);ArcaneWorldData.get(level).addVibes(level,machine.getBlockPos(),0,2);}
                machine.consumeInput(slot,1);
            }
        }else if(machine.progress!=0){machine.progress=0;machine.setChanged();}
    }
    private void condenser(ServerLevel level){
        VisNetwork.equalizeCondensers(level,machine);
        float max=machine.upgrades(0)>0?1.25f:1;
        if(speed<max)speed=Math.min(max,speed+(machine.upgrades(0)>0?.01f:.0005f));
        if(degradation==0)speed=Math.max(0,speed-.005f);
        if(crystalType==5&&machine.upgrades(3)==0){degradation=0;condenseProgress=0;}
        int cost=machine.upgrades(1)>0?20:25;
        if(crystalType>=0)condenseProgress+=speed*(3+Math.abs(moon(level)-4))*.2f;
        if(condenseProgress>=cost&&machine.pureVis()<=machine.capacity()-1&&machine.taintedVis()<=machine.capacity()-1&&crystalType>=0){
            boolean dark=crystalType==5;var data=ArcaneWorldData.get(level);var aura=data.aura(level,machine.getBlockPos());
            if((!dark||machine.upgrades(3)>0)&&(dark?aura.taint():aura.vis())>=1){data.drainAura(level,machine.getBlockPos(),1,dark);machine.insertVis(1,dark);condenseProgress=0;}
        }
        boolean expired=false;
        if(condenseProgress<cost){
            expired=degradation>0;degradation=Math.max(0,degradation-Math.max(.25f,speed));
            // TileCondenser creates a tinkling wisp here; FXWisp makes one 1-in-3 sound check.
            // Keep it tied to actual depletion, rather than repeatedly sampling stale client state.
            if(degradation>0&&(int)(degradation%3)==0){
                condenserWisps++;
                var r=level.getRandom();var p=machine.getBlockPos();
                double x=p.getX()+.5+r.nextFloat()-r.nextFloat(),y=p.getY()+1.5+r.nextFloat()-r.nextFloat(),z=p.getZ()+.5+r.nextFloat()-r.nextFloat();
                if(r.nextInt(3)==0)level.playSound(null,x,y,z,net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.AMBIENT,.02f,.5f*((r.nextFloat()-r.nextFloat())*.6f+2));
            }
        }
        if(expired&&degradation<10){var p=machine.getBlockPos();level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,p.getX()+.5,p.getY()+1.3,p.getZ()+.5,1,0,0,0,0);}
        if(degradation==0){
            if(expired){
                var depleted=new ItemStack(Content.item("depleted_crystal"));var output=machine.getItem(9);
                if(output.isEmpty())machine.setItem(9,depleted);
                else if(output.is(depleted.getItem())){
                    if(output.getCount()<64){output.grow(1);machine.setChanged();}
                    else {var p=machine.getBlockPos();var drop=new net.minecraft.world.entity.item.ItemEntity(level,p.getX()+.5,p.getY()+1,p.getZ()+.5,depleted);drop.setDeltaMovement(drop.getDeltaMovement().x,.2,drop.getDeltaMovement().z);level.addFreshEntity(drop);}
                }
            }
            var crystal=Content.entry(machine.getItem(0));
            if(crystal!=null&&crystal.source_class().equals("ItemCrystals")&&crystal.meta()!=6&&(crystal.meta()!=5||machine.upgrades(3)>0)){
                degradation=4550;crystalType=crystal.meta();machine.consumeInput(0,1);
            }else crystalType=-1;
        }
        machine.progress=(int)condenseProgress;machine.workRequired=cost;machine.setChanged();
    }
    private void brazier(ServerLevel level){
        if(burnTime==0&&machine.getItem(0).is(Content.item("soul_fragment"))){burnMax=burnTime=6000;machine.consumeInput(0,1);}
        if(burnTime<=0)return;
        burnTime--;machine.setChanged();if(delay-->0)return;
        delay=90-(3+Math.abs(moon(level)-4))*10;
        var direction=Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom());var from=machine.getBlockPos().relative(direction,16);
        var data=ArcaneWorldData.get(level);boolean dark=!level.getRandom().nextBoolean();
        var local=data.aura(level,machine.getBlockPos());
        if(!dark&&(local.vis()>=dev.thaumcraft.PortConfig.auraMax||data.aura(level,from).vis()<1))dark=true;
        if(dark&&local.taint()>=dev.thaumcraft.PortConfig.auraMax)return;
        float transferred=data.drainAura(level,from,1,dark);data.changeAura(level,machine.getBlockPos(),dark?0:transferred,dark?transferred:0);
    }
    public int darkProgress(){return darkCost<=0?0:Math.clamp(Math.round(darkWork/darkCost*1000),0,1000);}
    public float absorb(ServerLevel level,float limit,boolean tainted){
        if(limit<=0)return 0;float stored=tainted?machine.taintedVis():machine.pureVis();
        // Network transfers have a .001 minimum; tiny final recipe balances still need to be paid.
        if(stored<limit)VisNetwork.pull(level,machine,Math.max(.001f,limit-stored),tainted);
        return machine.extractVis(limit,tainted);
    }
    public void recipe(String key,float pure,float dark){if(!recipeKey.equals(key)||pureCost!=pure||darkCost!=dark){clearWork();recipeKey=key;pureCost=pure;darkCost=dark;} }
    public void updateProgress(boolean remaining){machine.workRequired=1000;machine.progress=pureCost<=0?0:Math.clamp(Math.round((remaining?pureCost-pureWork:pureWork)/pureCost*1000),0,1000);machine.setChanged();}
    public void clearWork(){recipeKey="";infusionDefinition="";pureWork=darkWork=pureCost=darkCost=0;pendingCrystal=-1;machine.progress=0;machine.setChanged();}
    public void save(ValueOutput out){out.putInt("machine_boost",boost);out.putInt("infuser_boost_delay",infuserBoostDelay);out.putInt("pending_crystal",pendingCrystal);out.putString("work_recipe",recipeKey);out.putString("work_definition",infusionDefinition);out.putFloat("work_pure",pureWork);out.putFloat("work_dark",darkWork);out.putFloat("cost_pure",pureCost);out.putFloat("cost_dark",darkCost);out.putInt("burn_time",burnTime);out.putInt("burn_max",burnMax);out.putBoolean("duplicate_repeat",repeat);out.putBoolean("furnace_boosted",boosted);out.putInt("condenser_crystal",crystalType);out.putFloat("crystal_degradation",degradation);out.putFloat("condenser_speed",speed);out.putFloat("condenser_progress",condenseProgress);}
    public void load(ValueInput in){cachedDarknessMonolith=null;darknessRetry=Long.MIN_VALUE;researchWorked=false;boost=Math.clamp(in.getIntOr("machine_boost",0),0,10);infuserBoostDelay=Math.clamp(in.getIntOr("infuser_boost_delay",20),0,20);if(machine.machineId().equals("thaumic_infuser")){boost=0;infuserBoostDelay=20;}pendingCrystal=Math.clamp(in.getIntOr("pending_crystal",-1),-1,5);recipeKey=in.getStringOr("work_recipe","");infusionDefinition=in.getStringOr("work_definition","");pureWork=finite(in.getFloatOr("work_pure",0),100000);darkWork=finite(in.getFloatOr("work_dark",0),100000);pureCost=finite(in.getFloatOr("cost_pure",0),100000);darkCost=finite(in.getFloatOr("cost_dark",0),100000);burnTime=Math.clamp(in.getIntOr("burn_time",0),0,30000);burnMax=Math.clamp(in.getIntOr("burn_max",0),0,30000);repeat=in.getBooleanOr("duplicate_repeat",false);boosted=in.getBooleanOr("furnace_boosted",false);crystalType=Math.clamp(in.getIntOr("condenser_crystal",-1),-1,5);degradation=finite(in.getFloatOr("crystal_degradation",0),4550);speed=finite(in.getFloatOr("condenser_speed",0),1.25f);condenseProgress=finite(in.getFloatOr("condenser_progress",0),50);}
    private static float finite(float value,float max){return Float.isFinite(value)?Math.clamp(value,0,max):0;}
}
