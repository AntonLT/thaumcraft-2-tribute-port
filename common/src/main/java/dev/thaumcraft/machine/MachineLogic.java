package dev.thaumcraft.machine;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.gameplay.GameData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Set;

/** Server-only machine effects; all inventory and vis mutations happen on the server thread. */
public final class MachineLogic {
    private static final Set<String> PASSIVE=Set.of("arcane_bellows","vis_conduit","vis_filter","vis_valve","advanced_vis_valve","vis_pump");
    private static final String[] CRYSTALS={"vis_crystal","vaporous_crystal","aqueous_crystal","earthen_crystal","fiery_crystal","tainted_crystal"};
    private MachineLogic() {}

    public static void tick(ServerLevel level,MachineBlockEntity machine) {
        if(!machine.enabled())return;
        String id=machine.machineId();
        if(id.equals("arcane_bore"))for(int slot=9;slot<18;slot++)if(!machine.getItem(slot).isEmpty()){
            ItemStack recovered=machine.getItem(slot).copy();machine.setItem(slot,ItemStack.EMPTY);eject(level,machine,recovered);
        }
        if(PASSIVE.contains(id))return;
        switch(id) {
            case "crucible","crucible_of_eyes","thaumium_crucible","crucible_of_souls" -> {} // Runs once per game tick.
            case "vis_condenser" -> {} // Runs each game tick through MachineProcesses.
            case "vis_storage_tank","thaumium_reinforced_tank" -> {} // Per-tick suction and stacked distribution.
            case "thaumic_infuser","dark_infuser" -> {}
            case "quaesitum" -> {} // Runs once per game tick.
            case "arcane_furnace" -> {} // Runs each game tick through MachineProcesses.
            case "thaumic_restorer" -> {}
            case "thaumic_duplicator" -> {}
            case "thaumic_crystalizer" -> {}
            case "vis_purifier" -> {} // Per-tick conduit purification.
            case "thaumic_generator" -> {} // Runs once per game tick.
            case "darkness_generator" -> {} // Runs once per game tick.
            case "thaumic_enchanter" -> {} // Runs each game tick through MachineProcesses.
            case "occultic_enchanter" -> {} // Runs once per game tick.
            case "arcane_bore" -> {} // Runs once per game tick.
            case "totem_of_dawn","totem_of_dusk" -> {} // Original block random ticks handle totems.
            case "everfull_urn","brain_in_a_jar" -> {} // Original tiles have ambient behavior only.
            case "brazier_of_souls" -> {} // Runs each game tick through MachineProcesses.
            case "arcane_seal" -> {} // Runs once per game tick.
            case "void_chest","void_interface","traveling_trunk" -> StorageLogic.tick(level,machine);
            default -> throw new IllegalStateException("No machine behavior for "+id);
        }
    }

    public static void effect(ServerLevel level,MachineBlockEntity machine) {
        BlockPos pos=machine.getBlockPos();
        level.sendParticles(ParticleTypes.ENCHANT,pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5,8,0.4,0.25,0.4,0.1);
    }

    public static boolean ejectCrucible(ServerLevel level,MachineBlockEntity machine,net.minecraft.world.entity.player.Player player) {
        boolean moved=false;
        for(ItemEntity item:level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(machine.getBlockPos()))) {
            var velocity=player.position().subtract(item.position()).scale(.2);item.setPickUpDelay(0);item.setDeltaMovement(velocity);
            item.noPhysics=true;item.move(net.minecraft.world.entity.MoverType.SELF,velocity);item.noPhysics=false;moved=true;
        }
        return moved;
    }
    public static void crucible(ServerLevel level,MachineBlockEntity machine) {
        float total=machine.totalVis();
        if(total>machine.capacity()) {
            float overflow=Math.min((total-machine.capacity())/2,1);
            if(machine.pureVis()>=overflow)machine.extractVis(overflow,false);
            if(overflow>=1&&machine.taintedVis()>=1){machine.extractVis(1,true);machine.processes.crucibleWisps++;ArcaneWorldData.get(level).changeAura(level,machine.getBlockPos(),0,1);}
        }
        boolean powering=(machine.machineId().equals("crucible_of_eyes")||machine.machineId().equals("thaumium_crucible"))&&total>=machine.capacity()*.9f;
        if(machine.processes.cruciblePower!=powering){machine.processes.cruciblePower=powering;
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
                var neighbor=machine.getBlockPos().offset(x,y,z);
                level.updateNeighborsAt(neighbor,net.minecraft.world.level.block.Blocks.AIR);
            }}
        if(--machine.processes.crucibleDelay>0)return;
        int bellows=VisNetwork.bellows(level,machine);
        float conversion=switch(machine.machineId()) {case "crucible_of_eyes"->.6f;case "thaumium_crucible"->.7f;case "crucible_of_souls"->.4f;default->.5f;};
        MachineBlockEntity furnace=level.getBlockEntity(machine.getBlockPos().below()) instanceof MachineBlockEntity below&&below.machineId().equals("arcane_furnace")&&below.processes.burnTime>0&&!level.hasNeighborSignal(below.getBlockPos())?below:null;
        int furnaceBellows=furnace==null?0:VisNetwork.bellows(level,furnace);
        if(furnace!=null)conversion=Math.min(1,conversion+.1f+furnaceBellows*.025f+(furnace.processes.boosted?.1f:0));
        if(machine.machineId().equals("crucible_of_souls")) {
            if(Math.round(total+1)>machine.capacity())return;
            machine.processes.crucibleDelay=20-bellows*2;boolean sucked=false;
            for(LivingEntity living:level.getEntitiesOfClass(LivingEntity.class,new AABB(machine.getBlockPos()).inflate(4))) {
                if(living instanceof net.minecraft.world.entity.player.Player||living instanceof net.minecraft.world.entity.TamableAnimal||living instanceof dev.thaumcraft.entity.TravelingTrunk
                        ||net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(living.getType()).is(dev.thaumcraft.content.ModTags.SOUL_IMMUNE)
                        ||living instanceof dev.thaumcraft.entity.SkeletonAlly||living.hurtTime>0||!living.isAlive())continue;
                if(living.getType()==net.minecraft.world.entity.EntityType.SNOW_GOLEM){level.sendParticles(ParticleTypes.POOF,living.getX(),living.getY()+living.getBbHeight()/2,living.getZ(),20,living.getBbWidth(),living.getBbHeight()/2,living.getBbWidth(),.02);living.discard();}
                float value=living.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.UNDEAD)?.5f:1;
                machine.addCrucibleVis(value*conversion,value*(1-conversion));living.hurtServer(level,level.damageSources().generic(),1);
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER,3000));sucked=true;
                var effect=new dev.thaumcraft.network.SoulAbsorption(machine.getBlockPos(),living.getX(),living.getY(),living.getZ(),living.getBbHeight());
                for(var viewer:level.players())if(viewer.distanceToSqr(machine.getBlockPos().getCenter())<4096)dev.thaumcraft.network.SoulAbsorption.sender.accept(viewer,effect);
            }
            if(sucked){machine.showSoulAbsorption();dev.thaumcraft.content.ModSounds.play(level,machine.getBlockPos(),"suck",SoundSource.BLOCKS,.1f,.8f+level.getRandom().nextFloat()*.3f);ArcaneWorldData.get(level).addVibes(level,machine.getBlockPos(),0,1);}
            else machine.advanceSoulFace();
            return;
        }
        machine.processes.crucibleDelay=5;
        var contents=level.getEntitiesOfClass(ItemEntity.class,new AABB(machine.getBlockPos()));
        if(contents.isEmpty())return;
        ItemEntity dropped=contents.get(level.getRandom().nextInt(contents.size()));ItemStack input=dropped.getItem();float value=GameData.vis(input);
        java.util.UUID credited=dropped.getOwner() instanceof net.minecraft.world.entity.player.Player player?player.getUUID():machine.owner();
        if(!input.isDamaged()&&value>0){var offered=input.copyWithCount(1);value=dev.thaumcraft.api.ThaumcraftEvents.CRUCIBLE_DISSOLVING.apply(value,(listener,current)->listener.onDissolving(level,machine.getBlockPos(),credited,offered.copy(),current));}
        if(input.isDamaged()||value<=0) {
            dropped.setDeltaMovement((level.getRandom().nextFloat()-level.getRandom().nextFloat())*.2,.2+level.getRandom().nextFloat()*.3,(level.getRandom().nextFloat()-level.getRandom().nextFloat())*.2);level.playSound(null,dropped.getX(),dropped.getY(),dropped.getZ(),net.minecraft.sounds.SoundEvents.ITEM_PICKUP,SoundSource.BLOCKS,.5f,2+level.getRandom().nextFloat()*.45f);dropped.setPickUpDelay(10);((dev.thaumcraft.mixin.ItemEntityAccess)dropped).thaumcraft$setAge(0);return;
        }
        if(machine.machineId().equals("thaumium_crucible")&&total+value>machine.capacity())return;
        machine.addCrucibleVis(value*conversion,value*(1-conversion));
        float speed=(machine.machineId().equals("crucible")?.25f:machine.machineId().equals("crucible_of_eyes")?.5f:.75f)+bellows*.1f;
        machine.processes.crucibleDelay=10+Math.round(value/5/speed);
        if(furnace!=null)machine.processes.crucibleDelay=(int)(machine.processes.crucibleDelay*(.8f-furnaceBellows*.05f));
        ArcaneWorldData.get(level).addVibes(level,machine.getBlockPos(),0,(int)(value/10));
        level.sendParticles(ParticleTypes.LARGE_SMOKE,dropped.getX(),dropped.getY(),dropped.getZ(),1,0,0,0,0);
        var dissolved=input.copyWithCount(1);float dissolvedValue=value;
        input.shrink(1);if(input.isEmpty())dropped.discard();else dropped.setItem(input);
        dev.thaumcraft.api.ThaumcraftEvents.CRUCIBLE_DISSOLVED.post(listener->listener.onDissolved(level,machine.getBlockPos(),credited,dissolved.copy(),dissolvedValue));
        dev.thaumcraft.content.ModSounds.play(level,machine.getBlockPos(),"bubbling",SoundSource.BLOCKS,.25f,.9f+level.getRandom().nextFloat()*.2f);
    }
    public static void tickVisProcess(ServerLevel level,MachineBlockEntity machine){
        if(machine.machineId().equals("thaumic_infuser")||machine.machineId().equals("dark_infuser"))machine.processes.validateInfusionWork();
        switch(machine.machineId()){
            case "thaumic_infuser"->infusionNormal(level,machine);
            case "dark_infuser"->infusion(level,machine);
            case "thaumic_restorer"->repair(level,machine);
            case "thaumic_duplicator"->duplicate(level,machine);
            case "thaumic_crystalizer"->crystalize(level,machine);
        }
    }
    private record InfusionMatch(GameData.Infusion recipe,int[] used,float cost,ItemStack output,boolean canProcess) {}
    private static void infusionCompleted(ServerLevel level,MachineBlockEntity machine,InfusionMatch found){
        var result=found.output().copy();
        var owner=machine.owner()==null?null:level.getServer().getPlayerList().getPlayer(machine.owner());
        if(owner!=null)dev.thaumcraft.gameplay.ProgressTriggers.INFUSION_COMPLETED.trigger(owner,found.recipe().key());
        dev.thaumcraft.api.ThaumcraftEvents.INFUSION_COMPLETED.post(listener->listener.onCompleted(level,machine.getBlockPos(),machine.owner(),found.recipe().key(),result.copy()));
    }
    private static boolean infusionAllowed(ServerLevel level,MachineBlockEntity machine,GameData.Infusion recipe){
        return dev.thaumcraft.api.ThaumcraftEvents.INFUSION_ALLOWED.allows(listener->listener.allow(level,machine.getBlockPos(),machine.owner(),recipe.key()));
    }
    public static int infusionSlots(MachineBlockEntity machine){return machine.machineId().equals("dark_infuser")?5:6;}
    /** The infusion an infuser runs for these inputs: catalog order, owner research, addon vetoes, and an optional reservation pin. */
    public static GameData.Infusion selectInfusion(ServerLevel level,MachineBlockEntity machine,java.util.List<ItemStack> inventory,MachineBlockEntity.Reservation pin){
        boolean dark=machine.machineId().equals("dark_infuser");
        for(GameData.Infusion recipe:GameData.infusions()) {
            if(recipe.dark()!=dark||pin!=null&&!pin.pins(recipe)||dev.thaumcraft.gameplay.ResearchGate.locked(level,machine.owner(),recipe.key()))continue;
            if(GameData.allocateNormal(recipe,inventory,infusionSlots(machine))!=null&&infusionAllowed(level,machine,recipe))return recipe;
        }
        return null;
    }
    private static InfusionMatch matchNormal(ServerLevel level,MachineBlockEntity machine) {
        var recipe=selectInfusion(level,machine,machine.inventory(),machine.reservation());
        if(recipe==null)return new InfusionMatch(null,null,0,ItemStack.EMPTY,false);
        int[] u=GameData.allocateNormal(recipe,machine.inventory(),6);
        float cost=recipe.cost()*(machine.upgrades(1)>0?.8f:1);
        ItemStack output=recipe.result().create();
        return new InfusionMatch(recipe,u,cost,output,machine.outputAt(9,output,true));
    }
    private static void infusionNormal(ServerLevel level,MachineBlockEntity machine) {
        var process=machine.processes;
        boolean powered=level.hasNeighborSignal(machine.getBlockPos());
        var found=matchNormal(level,machine);
        if(process.pureCost!=0&&process.pureCost!=found.cost()) {
            process.clearWork();
            level.playSound(null,machine.getBlockPos(),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,1,1.6f);
        }
        boolean wasCooking=process.pureWork>0;
        float previouslyAbsorbed=process.infuserAbsorbed;process.infuserAbsorbed=0;
        boolean previouslyProcessing=process.infuserProcessing;
        process.infuserProcessing=found.canProcess()&&process.pureCost>0&&!powered;
        if(process.infuserProcessing) {
            float rate=.5f+.05f*process.boost+(machine.upgrades(0)>0?.5f:0);
            float paid=process.absorb(level,Math.min(rate,process.pureCost-process.pureWork+.01f),false);
            process.pureWork+=paid;process.infuserAbsorbed=paid;process.updateProgress(false);
            if(paid>=.025f&&process.soundDelay==0){process.soundDelay=62;ArcaneWorldData.get(level).addVibes(level,machine.getBlockPos(),0,1);dev.thaumcraft.content.ModSounds.play(level,machine.getBlockPos(),"infuser",SoundSource.BLOCKS,.2f,1);}
        }
        if(wasCooking&&process.pureWork>=process.pureCost) {
            if(found.canProcess()&&machine.outputAt(9,found.output(),false)) {
                for(int i=0;i<found.used().length;i++)if(found.used()[i]>0){
                    var ingredient=Content.entry(machine.getItem(i));
                    if(ingredient!=null&&ingredient.source_class().equals("ItemCrystals")&&ingredient.meta()<5)for(int n=0;n<found.used()[i];n++)if(level.getRandom().nextBoolean()){
                        var depleted=new ItemStack(Content.item("depleted_crystal"));if(!machine.outputAt(10,depleted,false))net.minecraft.world.level.block.Block.popResource(level,machine.getBlockPos().above(),depleted);
                    }
                    machine.consumeInput(i,found.used()[i]);
                }
                machine.sequence++;
                infusionCompleted(level,machine,found);
            }
            process.clearWork();
            found=matchNormal(level,machine);
        }
        if(process.pureWork==0&&found.canProcess()) {
            process.rememberInfusion(found.recipe());process.pureCost=found.cost();process.darkCost=0;process.darkWork=0;process.updateProgress(false);
        } else if(found.recipe()!=null&&process.pureCost==found.cost()&&process.pureCost!=0&&!process.recipeKey.equals(found.recipe().id())) {
            process.rememberInfusion(found.recipe());machine.setChanged();
        }
        if(process.infuserAbsorbed!=previouslyAbsorbed||process.infuserProcessing!=previouslyProcessing)machine.setChanged();
    }
    private static InfusionMatch matchDark(ServerLevel level,MachineBlockEntity machine){
        var recipe=selectInfusion(level,machine,machine.inventory(),machine.reservation());
        if(recipe==null)return new InfusionMatch(null,null,0,ItemStack.EMPTY,false);
        int[] used=GameData.allocateNormal(recipe,machine.inventory(),5);
        var output=recipe.result().create();
        return new InfusionMatch(recipe,used,recipe.cost()*(machine.upgrades(1)>0?.8f:1),output,machine.outputAt(9,output,true));
    }
    private static void infusion(ServerLevel level,MachineBlockEntity machine) {
        var process=machine.processes;
        var found=matchDark(level,machine);
        if((process.pureCost!=0||process.darkCost!=0)&&Math.round(process.pureCost+process.darkCost)!=Math.round(found.cost())){
            process.clearWork();level.playSound(null,machine.getBlockPos(),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,1,1.6f);
        }
        boolean wasCooking=process.pureWork>0||process.darkWork>0;
        if(found.canProcess()&&(process.pureCost>0||process.darkCost>0)&&!level.hasNeighborSignal(machine.getBlockPos())){
            float rate=.25f+.025f*(3+Math.abs(MachineProcesses.moon(level)-4))+(machine.upgrades(0)>0?.25f:0);
            float remaining=process.pureCost-process.pureWork;
            float paidPure=remaining>0?process.absorb(level,Math.min(rate,remaining+.01f),false):0;
            remaining=process.darkCost-process.darkWork;
            float paidTaint=remaining>0?process.absorb(level,Math.min(rate,remaining+.01f),true):0;
            process.pureWork+=paidPure;process.darkWork+=paidTaint;process.updateProgress(false);
            if(paidPure+paidTaint>=.025f&&process.soundDelay==0){process.soundDelay=62;ArcaneWorldData.get(level).addVibes(level,machine.getBlockPos(),0,2);dev.thaumcraft.content.ModSounds.play(level,machine.getBlockPos(),"infuserdark",SoundSource.BLOCKS,.2f,1);}
        }
        if(wasCooking&&process.pureWork>=process.pureCost&&process.darkWork>=process.darkCost){
            if(found.canProcess()&&machine.outputAt(9,found.output(),false)){
                for(int i=0;i<found.used().length;i++)if(found.used()[i]>0)machine.getItem(i).shrink(1);
                machine.sequence++;machine.setChanged();
                infusionCompleted(level,machine,found);
            }
            process.clearWork();found=matchDark(level,machine);
        }
        if(process.pureWork==0&&process.darkWork==0&&found.canProcess()){
            process.pureCost=machine.upgrades(3)>0?found.cost()/2:found.cost()*.6666667f;
            process.darkCost=machine.upgrades(3)>0?found.cost()/2:found.cost()*.33333334f;
            process.rememberInfusion(found.recipe());process.updateProgress(false);
        }else if(found.recipe()!=null&&!process.recipeKey.equals(found.recipe().id())){
            process.rememberInfusion(found.recipe());machine.setChanged();
        }
    }
    private static void repair(ServerLevel level,MachineBlockEntity machine) {
        boolean worked=false,hasInput=false;
        for(int slot=0;slot<6;slot++){
            ItemStack input=machine.getItem(slot);if(input.isEmpty())continue;hasInput=true;
            if(input.isDamageableItem()&&input.isDamaged()){
                float cost=GameData.restorerCost(input)*(machine.upgrades(1)>0?.8f:1)*(input.isEnchanted()?1.5f:1);
                if(machine.pureVis()>cost){machine.spendVis(cost,false);input.setDamageValue(input.getDamageValue()-1);machine.setChanged();worked=true;}
            }
            if(!input.isDamaged()&&machine.output(input,true)){machine.output(input,false);machine.setItem(slot,ItemStack.EMPTY);}
        }
        if(hasInput)VisNetwork.pull(level,machine,Math.max(0,Math.min(5-machine.pureVis(),.5f+.05f*machine.processes.boost+(machine.upgrades(0)>0?.5f:0))),false);
        machine.progress=worked?1:0;machine.workRequired=1;
        if(worked&&machine.processes.soundDelay==0){machine.processes.soundDelay=50;dev.thaumcraft.content.ModSounds.play(level,machine.getBlockPos(),"tinkering",SoundSource.BLOCKS,.5f,1);ArcaneWorldData.get(level).addVibes(level,machine.getBlockPos(),0,1);}
    }
    /**
     * Vis one duplication of {@code template} costs, or 0 when the duplicator refuses it. Original duplication accepts
     * common items with a vis value, except forbidden ones; the efficiency upgrade lowers the cost.
     */
    public static float duplicationCost(ItemStack template,boolean efficient) {
        float value=GameData.vis(template);
        if(template.isEmpty()||value<=0||template.getRarity()!=net.minecraft.world.item.Rarity.COMMON||template.is(dev.thaumcraft.content.ModTags.DUPLICATOR_FORBIDDEN))return 0;
        return template.is(Blocks.COBBLESTONE.asItem())?2:Math.round(value*(efficient?4:5));
    }
    /** One cycle's copies: identity and damage only, never stored contents. */
    public static ItemStack duplicationOutput(ItemStack template,boolean repeat) {
        ItemStack output=new ItemStack(template.getItem(),repeat?1:2);if(template.isDamageableItem())output.setDamageValue(template.getDamageValue());
        return output;
    }
    private static void duplicate(ServerLevel level,MachineBlockEntity machine) {
        ItemStack template=machine.getItem(0);var process=machine.processes;
        // A reserved duplicator runs one cycle for its bridge, then waits for the bridge to collect it.
        if(machine.reservation()!=null&&machine.sequence!=machine.reservation().sequence())return;
        float cost=duplicationCost(template,machine.upgrades(1)>0);
        if(cost<=0){if(!process.recipeKey.isEmpty())process.clearWork();return;}
        ItemStack output=duplicationOutput(template,process.repeat);
        process.recipe(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(template.getItem()).toString(),cost,0);
        if(!machine.output(output,true))return;
        process.pureWork+=process.absorb(level,Math.min(cost-process.pureWork,.5f+.05f*process.boost+(machine.upgrades(0)>0?.5f:0)),false);process.updateProgress(false);
        if(process.pureWork+.0001f<cost)return;
        machine.output(output,false);if(!process.repeat)machine.consumeInput(0,1);process.clearWork();machine.sequence++;
        ArcaneWorldData.get(level).addVibes(level,machine.getBlockPos(),0,Math.max(1,(int)(cost/20)));
        dev.thaumcraft.network.EquipmentEffect.send(level,dev.thaumcraft.network.EquipmentEffect.DUPLICATOR,net.minecraft.world.phys.Vec3.atCenterOf(machine.getBlockPos()));
    }
    private static void crystalize(ServerLevel level,MachineBlockEntity machine) {
        var input=Content.entry(machine.getItem(0));var process=machine.processes;
        if(input==null||!input.source_class().equals("ItemCrystals")){
            if(!process.recipeKey.isEmpty()){
                level.playSound(null,machine.getBlockPos(),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,1,1.6f);
                process.clearWork();machine.setChanged();
            }
            return;
        }
        // Original countdown belongs to the current cycle, not the crystal variant or current upgrade.
        if(process.recipeKey.isEmpty())process.recipe("crystalization",(machine.upgrades(1)>0?25:30)*(input.meta()==6?1:2f/3),0);
        float cost=process.pureCost;
        if(level.hasNeighborSignal(machine.getBlockPos()))return;
        process.pureWork+=process.absorb(level,Math.min(cost-process.pureWork,.025f+.0025f*process.boost+(machine.upgrades(0)>0?.025f:0)),false);process.updateProgress(true);
        if(process.pureWork+.0001f<cost)return;
        if(process.pendingCrystal<0)process.pendingCrystal=crystalByBiome(level,machine.getBlockPos(),machine.upgrades(3)>0?3:0);
        int type=process.pendingCrystal;var output=new ItemStack(Content.item(CRYSTALS[type]));
        if(!machine.outputAt(9+type,output,true))return;
        machine.outputAt(9+type,output,false);machine.consumeInput(0,1);process.clearWork();machine.sequence++;
        ArcaneWorldData.get(level).addVibes(level,machine.getBlockPos(),0,5);
    }
    private static int crystalByBiome(ServerLevel level,BlockPos pos,int dark){
        var biome=level.getBiome(pos);String id=biome.unwrapKey().map(k->k.identifier().getPath()).orElse("");
        boolean plains=id.contains("plains"),mushroom=id.contains("mushroom"),jungle=id.contains("jungle"),mountain=biome.is(net.minecraft.tags.BiomeTags.IS_MOUNTAIN),desert=id.contains("desert"),swamp=id.contains("swamp");
        int[] weights={1,1,1,1,1,dark};
        if(plains||mushroom||jungle)weights[0]++;if(desert||mountain||plains)weights[1]++;
        if(biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN)||biome.is(net.minecraft.tags.BiomeTags.IS_RIVER)||swamp||id.contains("snow")||id.contains("frozen"))weights[2]++;
        if(mountain||jungle||biome.is(net.minecraft.tags.BiomeTags.IS_FOREST)||biome.is(net.minecraft.tags.BiomeTags.IS_TAIGA))weights[3]++;
        if(desert||mountain||biome.is(net.minecraft.tags.BiomeTags.IS_NETHER))weights[4]++;if(dark>0&&(mushroom||swamp))weights[5]++;
        int choice=level.getRandom().nextInt(java.util.Arrays.stream(weights).sum());for(int i=0;i<6;i++){choice-=weights[i];if(choice<0)return i;}return 0;
    }
    public static void generatorEmitted(MachineBlockEntity machine) {
        if(machine.getLevel() instanceof ServerLevel level&&machine.processes.soundDelay==0){machine.processes.soundDelay=70;dev.thaumcraft.content.ModSounds.play(level,machine.getBlockPos(),"elecloop",SoundSource.BLOCKS,.05f,1);ArcaneWorldData.get(level).addVibes(level,machine.getBlockPos(),0,1);}
    }
    public static void generator(ServerLevel level,MachineBlockEntity machine) {
        if(!machine.enabled()||level.hasNeighborSignal(machine.getBlockPos())||level.hasNeighborSignal(machine.getBlockPos().above()))return;
        float moon=(2+Math.abs(MachineProcesses.moon(level)-4))*.2f+(machine.upgrades(0)>0?.2f:0);
        int multiplier=dev.thaumcraft.api.ThaumcraftApi.generatorEnergyMultiplier();
        float cost=.00066666666f*(machine.upgrades(1)>0?.8f:1)*Math.min(75*moon,(machine.energyCapacity()-machine.energy())/(4f*multiplier));
        if(cost>.006666667f&&VisNetwork.takeExactPure(level,machine,cost))machine.generateEnergy(Math.round(cost*150*multiplier)*4);
        dev.thaumcraft.api.IntegrationHooks.pushGenerator(level,machine);
    }
    public static void darkness(ServerLevel level,MachineBlockEntity machine) {
        if(!machine.enabled()){machine.setVisualDarknessMonolith(null);return;}
        boolean seed=machine.getItem(0).is(Items.WHEAT_SEEDS)||machine.getItem(0).is(Items.MELON_SEEDS)||machine.getItem(0).is(Items.PUMPKIN_SEEDS)||machine.getItem(0).is(Items.NETHER_WART);
        BlockPos monolith=seed?machine.processes.darknessMonolith(level):null;
        machine.setVisualDarknessMonolith(monolith);
        machine.workRequired=90000;
        if(monolith==null){if(machine.progress!=0){machine.progress=0;machine.setChanged();}return;}
        int penalty=Math.abs(MachineProcesses.moon(level)-4)*4+level.getMaxLocalRawBrightness(machine.getBlockPos());
        machine.progress+=32-penalty;
        if(machine.progress>=90000) {
            ItemStack result=new ItemStack(Content.item("seed_of_darkness"));
            if(machine.outputAt(9,result,false)){machine.consumeInput(0,1);machine.progress=0;}else machine.progress=89999;
        }
        machine.setChanged();
    }
    public static void bore(ServerLevel level,MachineBlockEntity machine) {
        ItemStack focus=machine.getItem(0);var def=Content.entry(focus);
        int type=def!=null&&focus.isDamageableItem()?def.focusType():-1;
        if(type>=0)machine.processes.boreDelay=type==1?2:4;
        int area=type==0?2:type==3?5:3,range=type==2?80:40,delay=machine.processes.boreDelay;
        // TileBore advances its local timer even without power, fuel or a focus.
        machine.progress++;machine.workRequired=delay+1;
        if(machine.progress>delay)machine.progress=0;
        boolean powered=machine.enabled()&&(level.hasNeighborSignal(machine.getBlockPos())||level.hasNeighborSignal(machine.getBlockPos().above()));
        Direction facing=machine.getBlockState().getValue(MachineBlock.FACING);
        if(machine.energy()>0&&powered&&machine.progress==0&&type>=0) {
            if(mineBoreBlock(level,machine,facing,area,range)) {
                // Modern setters clamp to max damage; the original breaks on the following block.
                if(focus.getDamageValue()>=focus.getMaxDamage())machine.setItem(0,ItemStack.EMPTY);
                else focus.setDamageValue(focus.getDamageValue()+1);
                machine.sequence++;
            }
            machine.extractEnergy(1,false);
            level.playSound(null,machine.getBlockPos(),net.minecraft.sounds.SoundEvents.SLIME_ATTACK,SoundSource.BLOCKS,.3f,.1f+level.getRandom().nextFloat()*.3f);
            dev.thaumcraft.network.BoreEffect.send(level,dev.thaumcraft.network.BoreEffect.PULSE,type,
                    machine.getBlockPos().relative(facing).getCenter(),machine.getBlockPos().relative(facing,5).getCenter());
        }
        machine.processes.clearBoreAttraction();
        if(machine.energy()>0&&powered&&type>=0)suckBoreItems(level,machine,facing,area,range,type);
        // Refuel last, including the tick that exhausted the previous singularity.
        if(machine.energy()==0&&powered&&!machine.getItem(0).isEmpty()&&type>=0&&machine.getItem(1).is(Content.item("arcane_singularity"))) {
            machine.consumeInput(1,1);machine.generateEnergy(250);
        }
    }
    private static void suckBoreItems(ServerLevel level,MachineBlockEntity machine,Direction facing,int area,int range,int focus) {
        boolean conserve=focus==4;
        BlockPos mouthPos=machine.getBlockPos().relative(facing);var mouth=mouthPos.getCenter();
        int radius=area+1;
        var bounds=new AABB(machine.getBlockPos()).expandTowards(facing.getStepX()*(range+1),facing.getStepY()*(range+1),facing.getStepZ()*(range+1))
                .inflate(facing.getAxis()==Direction.Axis.X?0:radius,facing.getAxis()==Direction.Axis.Y?0:radius,facing.getAxis()==Direction.Axis.Z?0:radius);
        for(ItemEntity item:level.getEntitiesOfClass(ItemEntity.class,bounds,e->e.isAlive()&&!e.noPhysics)) {
            var velocity=item.getDeltaMovement().add(mouth.subtract(item.position()).normalize().scale(.3));
            item.setDeltaMovement(Math.clamp(velocity.x,-.35,.35),Math.clamp(velocity.y,-.35,.35),Math.clamp(velocity.z,-.35,.35));
            item.setPickUpDelay(2);item.noPhysics=true;
            ((dev.thaumcraft.mixin.ItemEntityAccess)item).thaumcraft$setBoreAttracted(true);
            machine.processes.boreItems.add(item);
            dev.thaumcraft.network.BoreEffect.send(level,dev.thaumcraft.network.BoreEffect.TRAIL,focus,
                    new net.minecraft.world.phys.Vec3(item.xo,item.yo+.1,item.zo),mouth);
        }
        for(ItemEntity item:level.getEntitiesOfClass(ItemEntity.class,new AABB(mouthPos),ItemEntity::isAlive)) {
            item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);item.noPhysics=false;
            ((dev.thaumcraft.mixin.ItemEntityAccess)item).thaumcraft$setBoreAttracted(false);
            dev.thaumcraft.network.BoreEffect.send(level,dev.thaumcraft.network.BoreEffect.COLLECT,focus,
                    new net.minecraft.world.phys.Vec3(item.xo,item.yo+.1,item.zo),mouth);
            ItemStack drop=item.getItem();float value=conserve?GameData.basicVis(drop):0;
            if(conserve&&machine.energy()+drop.getCount()<=250&&value>0&&value<2){machine.generateEnergy(drop.getCount());item.discard();}
            else eject(level,machine,drop,item);
        }
    }
    private static boolean mineBoreBlock(ServerLevel level,MachineBlockEntity machine,Direction facing,int area,int range) {
        Direction across=facing.getAxis().isVertical()?Direction.EAST:facing.getClockWise();
        Direction vertical=facing.getAxis().isVertical()?Direction.SOUTH:Direction.UP;
        for(int attempt=0;attempt<4;attempt++) {
            int x=level.getRandom().nextInt(area)-level.getRandom().nextInt(area),y=level.getRandom().nextInt(area)-level.getRandom().nextInt(area);
            for(int distance=2;distance<=range;distance++) {
                BlockPos target=machine.getBlockPos().relative(facing,distance).relative(across,x).relative(vertical,y);
                if(level.isOutsideBuildHeight(target)||!level.hasChunkAt(target))break;
                BlockState state=level.getBlockState(target);
                if(state.isAir()||state.is(Blocks.WATER)||state.getDestroySpeed(level,target)<0||state.is(dev.thaumcraft.content.ModTags.BORE_IMMUNE)
                        ||!dev.thaumcraft.api.ThaumcraftEvents.removalAllowed(level,target,state,machine.owner()))continue;
                var drops=Block.getDrops(state,level,target,level.getBlockEntity(target),null,ItemStack.EMPTY);
                // TileBore clears the block directly, without vanilla break debris or residual fluid.
                if(!level.setBlock(target,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL))continue;
                for(ItemStack drop:drops)Block.popResource(level,target,drop);
                level.sendParticles(ParticleTypes.POOF,target.getX()+.5,target.getY()+.5,target.getZ()+.5,1,0,0,0,0);
                level.playSound(null,target,net.minecraft.sounds.SoundEvents.GRAVEL_STEP,SoundSource.BLOCKS,1,1);
                return true;
            }
        }
        return false;
    }
    private static void eject(ServerLevel level,MachineBlockEntity machine,ItemStack stack){
        eject(level,machine,stack,null);
    }
    private static void eject(ServerLevel level,MachineBlockEntity machine,ItemStack stack,ItemEntity existing){
        Direction output=machine.machineId().equals("arcane_bore")?machine.getBlockState().getValue(MachineBlock.FACING).getOpposite():Direction.UP;
        BlockPos destination=machine.getBlockPos().relative(output);ItemStack remaining=stack.copy();
        remaining=dev.thaumcraft.api.IntegrationHooks.insert(level,destination,output.getOpposite(),remaining);
        if(remaining.isEmpty()){if(existing!=null)existing.discard();return;}
        var center=destination.getCenter().subtract(output.getStepX()*.3,output.getStepY()*.3,output.getStepZ()*.3);
        var entity=existing==null?new ItemEntity(level,center.x,center.y,center.z,remaining):existing;
        entity.setItem(remaining);entity.setPos(center);entity.setDeltaMovement(output.getStepX()*.1,output.getStepY()*.1,output.getStepZ()*.1);
        if(existing==null)level.addFreshEntity(entity);
        if(existing!=null)level.sendParticles(ParticleTypes.SMOKE,center.x,center.y,center.z,0,0,.1*level.getRandom().nextFloat(),0,1);
    }
    public static void totem(ServerLevel level,MachineBlockEntity machine) {
        boolean dawn=machine.machineId().equals("totem_of_dawn");
        var pos=machine.getBlockPos();var random=level.getRandom();var data=ArcaneWorldData.get(level);
        data.addVibes(level,pos,dawn?1+random.nextInt(2):0,dawn?0:1+random.nextInt(3));
        if(dawn){
            for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){
                var target=pos.offset(x,y,z);
                if(!level.hasChunkAt(target))continue;
                if(level.getBlockState(target).is(Content.block("taint_spore_pod"))&&random.nextInt(3)!=0)continue;
                if(dev.thaumcraft.content.TaintBlock.purify(level,target))return;
            }
        }else dev.thaumcraft.content.TaintBlock.increase(level,pos,data.aura(level,pos).taint(),random);
    }
}
