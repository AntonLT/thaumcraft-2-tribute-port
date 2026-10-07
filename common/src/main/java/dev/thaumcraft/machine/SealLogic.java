package dev.thaumcraft.machine;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.gameplay.ArcaneWorldData;
import dev.thaumcraft.world.SealPortals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Ordered combinations follow the original TileSeal dispatch, including undocumented recipes. */
public final class SealLogic {
    private static final Map<String,String> DESCRIPTIONS;
    private static final Map<MachineBlockEntity,Integer> SOUND_DELAYS=new WeakHashMap<>();
    static {
        try(var in=SealLogic.class.getResourceAsStream("/thaumcraft2tp/seals.json")) {
            if(in==null)throw new IllegalStateException("Missing seal definitions");
            DESCRIPTIONS=new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),new TypeToken<Map<String,String>>(){}.getType());
        } catch(Exception e) {throw new ExceptionInInitializerError(e);}
    }
    private SealLogic() {}
    public static String description(String combination){
        // Keep the archived source text intact, but correct omissions/claims contradicted by its dispatch.
        return switch(combination){
            case "5,3,-1" -> "suppresses newly spawned hostile creatures within a directional range of 6 blocks. It requires line-of-sight.";
            case "5,3,3" -> "suppresses newly spawned hostile creatures within a directional range of 12 blocks. It requires line-of-sight.";
            case "4,-1,-1" -> "emits a short-range cone of fire at hostile creatures and animals.";
            case "3,3,-1" -> "tills the earth within the same short range as a single Earth rune.";
            case "4,1,1" -> "emits a long-range homing beam of hot air that damages and pushes creatures away.";
            case "1,4,1" -> "hurls rapid-fire bolts of lightning at creatures within a long range.";
            case "2,-1,-1" -> "hydrates the soil within a short radius.";
            case null -> null;
            default -> DESCRIPTIONS.get(combination);
        };
    }
    public static int[] runes(MachineBlockEntity machine) {
        int[] result={-1,-1,-1};
        for(int i=0;i<3;i++){var entry=Content.entry(machine.getItem(18+i));if(entry!=null&&entry.source_class().equals("ItemRunicEssence"))result[i]=entry.meta();}
        return result;
    }
    public static void tick(ServerLevel level,MachineBlockEntity machine) {
        if(dev.thaumcraft.world.ChunkAnchors.radius(machine)>=0)dev.thaumcraft.world.ChunkAnchors.register(level,machine.getBlockPos());
        if(machine.progress>0){machine.progress--;return;}
        SOUND_DELAYS.computeIfPresent(machine,(seal,remaining)->Math.max(0,remaining-1));
        int[] r=runes(machine);
        if(!machine.enabled()||!active(r)){
            machine.setSensorSignal(0);return;
        }
        boolean scanner=r[0]==0&&r[1]==4;
        if((level.hasNeighborSignal(machine.getBlockPos())||level.hasNeighborSignal(machine.getBlockPos().above()))&&!scanner){machine.setSensorSignal(0);return;}
        if(!scanner)machine.setSensorSignal(0);
        int delay=cooldown(r,r[0]==1&&r[1]==4?level.getRandom().nextInt(3):0);
        machine.workRequired=delay;machine.progress=Math.max(0,delay-1);
        BlockPos pos=machine.getBlockPos();var aura=ArcaneWorldData.get(level);boolean worked=false;
        switch(r[0]) {
            case 0 -> {
                if(r[1]<0||r[1]==0){worked=aura.changeBoost(level,pos,1)>0;if(worked)effect(level,machine,dev.thaumcraft.network.SealEffect.BOOST,pos.getCenter());}
                else if(r[1]==1)worked=SealPortals.tick(level,machine,r[2]);
                else if(scanner){int radius=r[2]==1?9:r[2]<0?3:6;worked=entities(level,machine,radius).stream().anyMatch(e->sensor(e,r[2])&&visible(level,pos,e));machine.setSensorSignal(worked?15:0);if(worked)effect(level,machine,dev.thaumcraft.network.SealEffect.DETECT,pos.getCenter());}
                if(r[1]==3)effect(level,machine,dev.thaumcraft.network.SealEffect.ANCHOR,pos.getCenter());
            }
            case 1 -> {if(r[1]<0||r[1]==1||r[1]==5)worked=wind(level,machine,r);else if(r[1]==4)worked=attack(level,machine,r,1);}
            case 2 -> {if(r[1]<0)worked=farm(level,machine,r,0);else if(r[1]==0)worked=heal(level,machine,r);else if(r[1]==1)worked=attack(level,machine,r,2);else if(r[1]==3)worked=farm(level,machine,r,1);}
            case 3 -> worked=farm(level,machine,r,r[1]==0?2:r[1]==2?3:4);
            case 4 -> worked=attack(level,machine,r,4);
            case 5 -> {
                if(r[1]==0){var cell=aura.aura(level,pos);float amount=Math.min(1,Math.min(cell.vis(),cell.taint()));if(amount>0){aura.changeAura(level,pos,-amount,-amount);aura.addVibes(level,pos,0,1);effect(level,machine,dev.thaumcraft.network.SealEffect.NULLIFY,pos.getCenter());}}
                // tickCount is not saved, so chunk-loaded mobs also look new: spare persistent mobs and active raid waves.
                else if(r[1]==3)for(Entity entity:unsortedEntities(level,machine,r[2]==3?12:6))if(entity instanceof Monster monster&&monster.tickCount<5&&!monster.isPersistenceRequired()
                        &&!(monster instanceof net.minecraft.world.entity.raid.Raider raider&&raider.hasActiveRaid())&&visible(level,pos,monster))monster.discard();
            }
        }
        // Cooldown and work are transient; only a portal's cooldown is drawn (the closing animation).
        machine.sealWorked|=worked;if(r[0]==0&&r[1]==1)machine.markVisualDirty();
    }
    public static void randomTick(ServerLevel level,MachineBlockEntity machine){
        int count=0;for(int rune:runes(machine))if(rune>=0)count++;
        if(machine.sealWorked&&count>0){machine.sealWorked=false;ArcaneWorldData.get(level).addVibes(level,machine.getBlockPos(),0,1+level.getRandom().nextInt(count));}
    }
    public static void runesChanged(MachineBlockEntity machine){
        machine.progress=60;machine.setChanged();
        if(machine.getLevel() instanceof ServerLevel level)SealPortals.updateRunes(level,machine);
    }
    public static Vec3 origin(MachineBlockEntity machine){
        var face=machine.getBlockState().getValue(MachineBlock.FACING);
        return machine.getBlockPos().getCenter().add(-face.getStepX()*.5,-face.getStepY()*.5,-face.getStepZ()*.5);
    }
    private static void effect(ServerLevel level,MachineBlockEntity machine,int kind,Vec3 target){dev.thaumcraft.network.SealEffect.send(level,kind,origin(machine),target);}
    /** Source dispatch is authoritative: several working combinations have no description in seals.txt. */
    public static boolean active(int[] r){
        if(r.length!=3||r[0]<0||r[0]>5||r[1]<-1||r[1]>5||r[2]<-1||r[2]>5)return false;
        if(r[1]==-1)return r[0]<5;
        return switch(r[0]){
            case 0 -> r[1]==0&&(r[2]==-1||r[2]==0)||r[1]==1||r[1]==3&&(r[2]==-1||r[2]==3)||r[1]==4&&r[2]!=2;
            case 1 -> r[1]==1&&(r[2]==-1||r[2]==0||r[2]==1||r[2]==3)||r[1]==4||r[1]==5&&r[2]!=2;
            case 2 -> r[1]==0||r[1]==1||r[1]==3&&(r[2]==-1||r[2]==1||r[2]==3);
            case 3 -> (r[1]==0||r[1]==2||r[1]==3)&&(r[2]==-1||r[2]==1||r[2]==3);
            case 4 -> r[1]==1&&(r[2]==-1||r[2]==1||r[2]==4)||r[1]==4;
            case 5 -> r[1]==0&&(r[2]==-1||r[2]==0)||r[1]==3&&(r[2]==-1||r[2]==3);
            default -> false;
        };
    }
    /** Original TileSeal delays in game ticks. */
    public static int cooldown(int[] r,int jitter) {
        return switch(r[0]) {
            case 0 -> r[1]<0?20:r[1]==0?(r[2]==0?10:15):r[1]==4||r[1]==3?5:1;
            case 1 -> r[1]==4?8+jitter-count(r,1)*2:2;
            case 2 -> r[1]==1?1:r[1]==3?(r[2]==1?15:30):r[1]==0&&r[2]==1?10:20;
            case 3 -> (r[1]==0?40:20)/(r[1]>=0&&r[2]==1?2:1);
            case 4 -> r[1]==1?5:1;
            case 5 -> r[1]==0?(r[2]==0?80:100):1;
            default -> 1;
        };
    }
    private static int count(int[] r,int value){return (r[1]==value?1:0)+(r[2]==value?1:0);}
    public static AABB area(MachineBlockEntity seal,double range) {
        var facing=seal.getBlockState().getValue(MachineBlock.FACING);BlockPos p=seal.getBlockPos();
        double x=facing.getStepX(),y=facing.getStepY(),z=facing.getStepZ();
        return new AABB(p).inflate(x==0?range:0,y==0?range:0,z==0?range:0).expandTowards(x*range*2,y*range*2,z*range*2);
    }
    /**
     * Candidates nearest first. Seals that need line of sight test {@link #visible} inside their loops, once an entity
     * passes the cheaper checks: a stable sort keeps the visible ones in the same order as filtering first would.
     */
    private static List<Entity> entities(ServerLevel level,MachineBlockEntity seal,double range) {
        var result=unsortedEntities(level,seal,range);
        result.sort(Comparator.comparingDouble(e->e.distanceToSqr(seal.getBlockPos().getCenter())));return result;
    }
    private static List<Entity> unsortedEntities(ServerLevel level,MachineBlockEntity seal,double range){
        return level.getEntities((Entity)null,area(seal,range),e->!e.isSpectator()&&e.isAlive()&&!(e instanceof dev.thaumcraft.entity.ArcaneMote)&&!(e instanceof dev.thaumcraft.entity.LightningEffect)&&!(e instanceof dev.thaumcraft.entity.RelicProjectile p&&p.mode()==4));
    }
    private static boolean visible(ServerLevel level,BlockPos pos,Entity entity) {
        return level.clip(new ClipContext(pos.getCenter(),entity.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.SOURCE_ONLY,entity)).getType()==HitResult.Type.MISS;
    }
    private static boolean sensor(Entity entity,int type) {
        return switch(type){case 0->hostile(entity);case 3->entity instanceof Animal;case 4->entity instanceof ItemEntity;case 5->entity instanceof Player;default->hostile(entity)||entity instanceof Animal||entity instanceof Player||entity instanceof ItemEntity;};
    }
    private static boolean hostile(Entity entity){return entity instanceof net.minecraft.world.entity.monster.Enemy;}
    private static boolean protectedAnimal(Entity e){return e instanceof dev.thaumcraft.entity.TravelingTrunk||e instanceof net.minecraft.world.entity.TamableAnimal;}
    private static boolean target(Entity entity,boolean mobs,boolean animals,boolean players) {
        return mobs&&hostile(entity)||animals&&entity instanceof Animal&&!protectedAnimal(entity)||players&&entity instanceof Player;
    }
    private static void sound(ServerLevel level,MachineBlockEntity seal,String name,int interval,float volume,float pitch,float variation){
        if(SOUND_DELAYS.getOrDefault(seal,0)<=0){dev.thaumcraft.content.ModSounds.play(level,seal.getBlockPos(),name,net.minecraft.sounds.SoundSource.BLOCKS,volume,pitch+(variation>0?level.getRandom().nextFloat()*variation:0));SOUND_DELAYS.put(seal,interval);}
    }
    private static boolean wind(ServerLevel level,MachineBlockEntity seal,int[] r) {
        int radius=r[1]<0?3:r[2]<0?5:r[1]==5&&(r[2]==4||r[2]==5)?6:7;
        boolean inward=r[1]==5,used=false;float strength=r[1]<0?.03f:r[2]<0?.06f:r[2]==4?.07f:r[2]==5?.04f:.08f;
        var facing=seal.getBlockState().getValue(MachineBlock.FACING);
        for(Entity entity:unsortedEntities(level,seal,radius)) {
            // Pushing a painting, item frame or lead knot breaks it; 1.2.5 paintings ignored motion.
            if(entity instanceof net.minecraft.world.entity.decoration.BlockAttachedEntity||entity instanceof Player||protectedAnimal(entity)||entity instanceof dev.thaumcraft.entity.CarpetEntity||entity instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow||entity instanceof net.minecraft.world.entity.ExperienceOrb)continue;
            if(r[1]>=0&&((r[2]==3||r[2]==5)&&entity instanceof LivingEntity||r[2]==0&&entity instanceof ItemEntity))continue;
            Vec3 delta=entity.position().subtract(seal.getBlockPos().getCenter().add(-facing.getStepX()*.5,-facing.getStepY()*.5,-facing.getStepZ()*.5)).normalize();
            double power=inward?-strength*2:entity.onGround()?strength*2:strength;
            double vertical=facing.getAxis()==net.minecraft.core.Direction.Axis.Y||inward&&(r[2]==4||r[2]==5)?delta.y*strength*3*(inward?-1:1):0;
            entity.push(delta.x*power,vertical,delta.z*power);entity.hurtMarked=true;used=true;
        }
        if(inward&&r[2]==5)for(Entity entity:unsortedEntities(level,seal,.2))if(entity instanceof ItemEntity item)pickup(level,seal.getBlockPos(),item);
        if(inward&&r[2]==4)for(Entity entity:entities(level,seal,0)){
            if(entity instanceof Player||entity instanceof dev.thaumcraft.entity.TravelingTrunk)continue;
            entity.hurtServer(level,level.damageSources().generic(),1);
            dev.thaumcraft.network.SealEffect.send(level,dev.thaumcraft.network.SealEffect.POOF,entity.position().add(-.5,entity.getEyeHeight()-.5,-.5),entity.position());
            level.playSound(null,seal.getBlockPos(),net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.5f,2+level.getRandom().nextFloat()*.4f);
        }
        if(used){
            sound(level,seal,inward?"suck":"wind",inward?30:25,.1f,strength*3+.9f,.1f);
            Vec3 origin=origin(seal);var random=level.getRandom();Vec3 direction=new Vec3(facing.getStepX()==0?random.nextFloat()-random.nextFloat():facing.getStepX(),facing.getStepY()==0?random.nextFloat()-random.nextFloat():facing.getStepY(),facing.getStepZ()==0?random.nextFloat()-random.nextFloat():facing.getStepZ());
            dev.thaumcraft.entity.ArcaneMote.wind(level,origin,origin.add(direction),inward);
        }return used;
    }
    private static boolean heal(ServerLevel level,MachineBlockEntity seal,int[] r) {
        var targets=entities(level,seal,r[2]<0?3:5);Collections.reverse(targets);boolean used=false;
        for(Entity e:targets)if(e instanceof LivingEntity entity){
            boolean players=r[2]!=3&&r[2]!=5,animals=r[2]!=0&&r[2]!=5,mobs=r[2]!=0&&r[2]!=3;
            boolean selected=mobs&&hostile(entity)||animals&&(entity instanceof Animal||entity instanceof net.minecraft.world.entity.npc.villager.Villager)&&!(entity instanceof dev.thaumcraft.entity.TravelingTrunk)||players&&(entity instanceof Player||protectedAnimal(entity));
            if(!selected||!visible(level,seal.getBlockPos(),entity))continue;if(!(entity instanceof Player)&&entity.hasEffect(MobEffects.HUNGER))break;
            if(entity.getHealth()>=entity.getMaxHealth())continue;
            entity.heal(1);if(r[2]==2)entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION,60,1));
            dev.thaumcraft.content.ModSounds.playAt(level,entity.getX(),entity.getY(),entity.getZ(),"heal",net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            effect(level,seal,dev.thaumcraft.network.SealEffect.HEAL,entity.getBoundingBox().getCenter());used=true;if(r[2]!=4)break;
        }
        return used;
    }
    private static boolean attack(ServerLevel level,MachineBlockEntity seal,int[] r,int element) {
        boolean beam=element==4&&r[1]==1;
        int range=element==1?6+count(r,1)*2:element==2?(r[2]<0?6:r[2]==2?15:10):beam?(r[2]<0?6:9):r[1]<0?4:r[2]<0?5:r[2]==1?8:r[2]==2?6:7;
        boolean used=false;var targets=entities(level,seal,range);if(element==4&&!beam)Collections.reverse(targets);
        for(Entity e:targets)if(e instanceof LivingEntity entity){
            boolean mobs=element==1?count(r,3)==0:r[2]!=3,animals=element==1?count(r,0)==0:r[2]!=0&&!(element==2&&r[2]==2),players=r[2]==5;
            if(element==4&&r[1]<0){mobs=true;animals=true;players=false;}
            if(players){mobs=false;animals=false;}
            if(beam?(entity instanceof Player||protectedAnimal(entity)):!target(entity,mobs,animals,players))continue;
            if(!visible(level,seal.getBlockPos(),entity))continue;
            if(element==1){entity.hurtServer(level,level.damageSources().magic(),3);entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,100,count(r,2)*4));}
            else if(element==2){Vec3 velocity=entity.getDeltaMovement().scale(.05);entity.setDeltaMovement(velocity.add(0,r[2]==1?.06:0,0));if(r[2]==1)entity.setOnGround(false);entity.hurtMarked=true;dev.thaumcraft.entity.ArcaneMote.freeze(level,origin(seal),entity);}
            else {
                Vec3 origin=origin(seal);
                if(beam)dev.thaumcraft.entity.ArcaneMote.beam(level,origin,entity,r[2]<0?1:r[2]==1?2:3,r[2]==1,r[2]==4,r[2]==4?3:2);
                else dev.thaumcraft.entity.ArcaneMote.scorch(level,origin,entity,r[1]<0?1:r[2]<0?2:r[2]==2?6:3,count(r,1)>0,mobs,animals,players);
            }
            if(element==1)dev.thaumcraft.entity.LightningEffect.spawn(level,origin(seal),entity.getBoundingBox().getCenter(),3,1.25f,5);
            used=true;if(element==4||r[2]!=4)break;
        }
        if(used){if(element==1)dev.thaumcraft.content.ModSounds.play(level,seal.getBlockPos(),"shock",net.minecraft.sounds.SoundSource.BLOCKS,.33f,1);else sound(level,seal,element==2?"wind":beam?"beamloop":"fireloop",element==2?25:beam?5:20,element==2?.2f:beam?.6f:.33f,element==2?.9f:1,element==2?.1f:0);}return used;
    }
    private static boolean farm(ServerLevel level,MachineBlockEntity seal,int[] r,int action) {
        int radius=(action==2?6:3)*(r[1]>=0&&r[2]==3?2:1);AABB box=area(seal,radius);var random=level.getRandom();
        for(int x=(int)box.minX;x<(int)box.maxX;x++)for(int y=(int)box.minY;y<(int)box.maxY;y++)for(int z=(int)box.minZ;z<(int)box.maxZ;z++) {
            BlockPos p=new BlockPos(x,y,z);
            if(level.isOutsideBuildHeight(p)||!level.hasChunkAt(p))continue;var state=level.getBlockState(p);boolean used=false;
            if(action==1){
                if(level.getMaxLocalRawBrightness(p.above())<8)continue;
                if(state.getBlock() instanceof StemBlock&&state.getValue(StemBlock.AGE)<7){
                    if(random.nextInt(20)==0){level.setBlockAndUpdate(p,state.setValue(StemBlock.AGE,state.getValue(StemBlock.AGE)+1));used=true;}
                }else if(state.getBlock() instanceof StemBlock&&state.getValue(StemBlock.AGE)==7){
                    if(random.nextInt(75)==0){
                        var products=stemProducts(level,(StemBlock)state.getBlock());Block fruit=products==null?null:products[0];boolean exists=fruit==null;
                        for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)if(fruit!=null&&level.getBlockState(p.relative(direction)).is(fruit))exists=true;
                        if(!exists){
                            // Original random direction order is west, east, north, south.
                            var direction=switch(random.nextInt(4)){case 0->net.minecraft.core.Direction.WEST;case 1->net.minecraft.core.Direction.EAST;case 2->net.minecraft.core.Direction.NORTH;default->net.minecraft.core.Direction.SOUTH;};
                            BlockPos next=p.relative(direction);
                            if(level.isEmptyBlock(next)&&level.getBlockState(next.below()).is(Blocks.FARMLAND)){
                                level.setBlockAndUpdate(next,fruit.defaultBlockState());
                                level.setBlockAndUpdate(p,products[1].defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,direction));p=next;used=true;
                            }
                        }
                    }
                }else if(state.getBlock() instanceof CropBlock crop&&!crop.isMaxAge(state)){
                    if(random.nextInt(20)==0){level.setBlockAndUpdate(p,crop.getStateForAge(crop.getAge(state)+1));used=true;}
                }else if((state.is(Blocks.SUGAR_CANE)||state.is(Blocks.CACTUS))&&state.getValue(state.is(Blocks.CACTUS)?CactusBlock.AGE:SugarCaneBlock.AGE)<15&&!level.getBlockState(p.above()).is(Blocks.SUGAR_CANE)&&!level.getBlockState(p.above()).is(Blocks.CACTUS)){
                    int height=1;while(level.getBlockState(p.below(height)).is(state.getBlock()))height++;
                    if(height<3&&random.nextInt(20)==0){var age=state.is(Blocks.CACTUS)?CactusBlock.AGE:SugarCaneBlock.AGE;level.setBlockAndUpdate(p,state.setValue(age,state.getValue(age)+1));used=true;}
                }else if(dev.thaumcraft.api.IntegrationHooks.hasCropAdapters()&&level.getBlockEntity(p.below())!=null&&random.nextInt(25)==0&&dev.thaumcraft.api.IntegrationHooks.crop(level,p.below(),dev.thaumcraft.api.IntegrationHooks.CropAction.GROW)){
                    used=true;
                }else if(state.is(Blocks.COBBLESTONE)&&random.nextInt(100)==0){level.setBlockAndUpdate(p,Blocks.MOSSY_COBBLESTONE.defaultBlockState());used=true;}
            }else if(action==0){
                if(state.is(Blocks.FARMLAND)&&state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.MOISTURE)<7&&random.nextInt(10)==0){level.setBlockAndUpdate(p,state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.MOISTURE,7));used=true;}
            }else if(action==2){
                if(state.is(Blocks.FARMLAND)&&level.isEmptyBlock(p.above())&&random.nextInt(10)==0){
                    for(BlockPos inventory:inventoryPositions(level,seal.getBlockPos()))if(!dev.thaumcraft.api.IntegrationHooks.extract(level,inventory,receivingFace(seal.getBlockPos(),inventory),stack->stack.is(Items.WHEAT_SEEDS),1).isEmpty()){
                        level.setBlockAndUpdate(p.above(),Blocks.WHEAT.defaultBlockState());dev.thaumcraft.network.SealEffect.send(level,dev.thaumcraft.network.SealEffect.SEED,Vec3.atLowerCornerOf(inventory),Vec3.atLowerCornerOf(inventory));used=true;break;
                    }
                }
            }else if(action==3){
                if(random.nextInt(10)==0){
                    if(state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state)||state.is(Blocks.MELON)||state.is(Blocks.PUMPKIN)||state.is(Blocks.SHORT_GRASS)||state.is(Blocks.FERN)||state.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS)||(state.is(Blocks.SUGAR_CANE)||state.is(Blocks.CACTUS))&&level.getBlockState(p.below()).is(state.getBlock())){level.destroyBlock(p,true);used=true;}
                    else used=dev.thaumcraft.api.IntegrationHooks.crop(level,p.below(),dev.thaumcraft.api.IntegrationHooks.CropAction.HARVEST);
                }
            }else if(action==4&&(state.is(Blocks.DIRT)||state.is(Blocks.GRASS_BLOCK))&&level.isEmptyBlock(p.above())&&random.nextInt(10)==0){level.setBlockAndUpdate(p,Blocks.FARMLAND.defaultBlockState());used=true;}
            if(used){dev.thaumcraft.network.SealEffect.send(level,dev.thaumcraft.network.SealEffect.HYDRATE+action,Vec3.atLowerCornerOf(p),Vec3.atLowerCornerOf(p));return true;}
        }
        return false;
    }
    /** Fruit and attached stem named by the stem's own codec, so modded stems keep their pairing; null if unresolved. */
    private static Block[] stemProducts(ServerLevel level,StemBlock stem){
        var encoded=StemBlock.CODEC.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE,stem).result();
        if(encoded.isEmpty()||!encoded.get().isJsonObject())return null;
        var json=encoded.get().getAsJsonObject();if(!json.has("fruit")||!json.has("attached_stem"))return null;
        var blocks=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK);
        var fruit=blocks.getOptional(net.minecraft.resources.Identifier.parse(json.get("fruit").getAsString()));
        var attached=blocks.getOptional(net.minecraft.resources.Identifier.parse(json.get("attached_stem").getAsString()));
        return fruit.isPresent()&&attached.isPresent()&&attached.get().defaultBlockState().hasProperty(HorizontalDirectionalBlock.FACING)?new Block[]{fruit.get(),attached.get()}:null;
    }
    private static List<BlockPos> inventoryPositions(ServerLevel level,BlockPos pos) {
        List<BlockPos> result=new ArrayList<>();
        for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){BlockPos p=pos.offset(x,y,z);if(!p.equals(pos)&&level.hasChunkAt(p))result.add(p);}
        return result;
    }
    private static net.minecraft.core.Direction receivingFace(BlockPos source,BlockPos target){
        int x=source.getX()-target.getX(),y=source.getY()-target.getY(),z=source.getZ()-target.getZ();
        if(Math.abs(y)>=Math.abs(x)&&Math.abs(y)>=Math.abs(z))return y>=0?net.minecraft.core.Direction.UP:net.minecraft.core.Direction.DOWN;
        return Math.abs(x)>=Math.abs(z)?x>=0?net.minecraft.core.Direction.EAST:net.minecraft.core.Direction.WEST:z>=0?net.minecraft.core.Direction.SOUTH:net.minecraft.core.Direction.NORTH;
    }
    private static void pickup(ServerLevel level,BlockPos pos,ItemEntity item){
        for(BlockPos target:inventoryPositions(level,pos)){
            var face=receivingFace(pos,target);ItemStack offered=item.getItem();
            ItemStack remaining=dev.thaumcraft.api.IntegrationHooks.insertWhole(level,target,face,offered);
            if(remaining.getCount()==offered.getCount())continue;
            if(remaining.isEmpty())item.discard();else item.setItem(remaining);
            level.playSound(null,item.blockPosition(),net.minecraft.sounds.SoundEvents.ITEM_PICKUP,net.minecraft.sounds.SoundSource.BLOCKS,.15f,2+level.getRandom().nextFloat()*.45f);
            dev.thaumcraft.network.SealEffect.send(level,dev.thaumcraft.network.SealEffect.PICKUP,item.position(),target.getCenter());return;
        }
    }
}
