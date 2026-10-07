package dev.thaumcraft.entity;

import dev.thaumcraft.Thaumcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class ModEntities {
    public static final Map<String,EntityType<?>> TYPES=new LinkedHashMap<>();
    public static final Map<EntityType<? extends LivingEntity>,AttributeSupplier> ATTRIBUTES=new LinkedHashMap<>();
    public static EntityType<Zombie> BRAINY_ZOMBIE;
    public static EntityType<ThaumSlime> THAUM_SLIME;
    public static EntityType<WispEntity> WISP;
    public static EntityType<SkeletonAlly> SKELETON_ALLY;
    public static EntityType<TravelingTrunk> TRUNK;
    public static EntityType<CarpetEntity> CARPET;
    public static EntityType<RelicProjectile> RELIC;
    public static EntityType<BoneArrow> BONE_ARROW;
    public static EntityType<ArcaneMote> ARCANE_MOTE;
    public static EntityType<LightningEffect> LIGHTNING;
    private ModEntities() {}
    private static <T extends Entity> EntityType<T> create(String id,EntityType.EntityFactory<T> factory,MobCategory category,float width,float height,BiConsumer<String,EntityType<?>> register) {
        var type=EntityType.Builder.of(factory,category).sized(width,height).clientTrackingRange(8).updateInterval(3).build(ResourceKey.create(Registries.ENTITY_TYPE,Thaumcraft.id(id)));
        TYPES.put(id,type);register.accept(id,type);return type;
    }
    public static void register(BiConsumer<String,EntityType<?>> register) {
        BRAINY_ZOMBIE=create("brainy_zombie",BrainyZombie::new,MobCategory.MONSTER,0.6f,1.8f,register);
        ATTRIBUTES.put(BRAINY_ZOMBIE,Zombie.createAttributes().add(Attributes.MAX_HEALTH,25).add(Attributes.ARMOR,3).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.FOLLOW_RANGE,16).build());
        THAUM_SLIME=create("thaum_slime",ThaumSlime::new,MobCategory.MONSTER,0.2f,0.2f,register);
        ATTRIBUTES.put(THAUM_SLIME,Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE).build());
        WISP=create("wisp",WispEntity::new,MobCategory.MONSTER,0.9f,0.9f,register);
        ATTRIBUTES.put(WISP,WispEntity.attributes().build());
        SKELETON_ALLY=create("skeleton_ally",SkeletonAlly::new,MobCategory.MISC,0.6f,1.8f,register);
        ATTRIBUTES.put(SKELETON_ALLY,Skeleton.createAttributes().add(Attributes.MOVEMENT_SPEED,.275).add(Attributes.FOLLOW_RANGE,16).build());
        TRUNK=create("traveling_trunk",TravelingTrunk::new,MobCategory.CREATURE,0.8f,0.8f,register);
        ATTRIBUTES.put(TRUNK,TravelingTrunk.attributes().build());
        CARPET=create("flying_carpet",CarpetEntity::new,MobCategory.MISC,1.5f,0.6f,register);
        BONE_ARROW=create("bone_arrow",BoneArrow::new,MobCategory.MISC,0.5f,0.5f,register);
        ARCANE_MOTE=EntityType.Builder.of(ArcaneMote::new,MobCategory.MISC).sized(.1f,.1f).noSave().noLootTable().clientTrackingRange(8).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE,Thaumcraft.id("arcane_mote")));
        TYPES.put("arcane_mote",ARCANE_MOTE);register.accept("arcane_mote",ARCANE_MOTE);
        LIGHTNING=EntityType.Builder.of(LightningEffect::new,MobCategory.MISC).sized(.1f,.1f).noSave().noLootTable().clientTrackingRange(8).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE,Thaumcraft.id("arcane_lightning")));
        TYPES.put("arcane_lightning",LIGHTNING);register.accept("arcane_lightning",LIGHTNING);
        RELIC=create("arcane_relic",RelicProjectile::new,MobCategory.MISC,0.3f,0.3f,register);
        for(String id:new String[]{"tainted_cow","tainted_pig","tainted_chicken","tainted_sheep","tainted_villager"}) {
            float width=id.equals("tainted_chicken")?0.5f:id.equals("tainted_villager")?0.6f:0.9f;
            float height=id.equals("tainted_chicken")?0.8f:id.equals("tainted_pig")?0.9f:id.equals("tainted_villager")?1.8f:1.3f;
            EntityType<TaintedCreature> type=create(id,TaintedCreature::new,MobCategory.MONSTER,width,height,register);
            double health=id.equals("tainted_cow")?40:id.equals("tainted_chicken")?8:20;
            double attack=id.equals("tainted_cow")?6:id.equals("tainted_pig")?4:id.equals("tainted_villager")?2:3;
            double armor=id.equals("tainted_sheep")?1:java.util.Set.of("tainted_pig","tainted_chicken").contains(id)?2:0;
            ATTRIBUTES.put(type,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,health).add(Attributes.ARMOR,armor).add(Attributes.ATTACK_DAMAGE,attack).add(Attributes.FOLLOW_RANGE,id.equals("tainted_sheep")?12:16).add(Attributes.MOVEMENT_SPEED,0.25).build());
        }
        var creeper=create("tainted_creeper",TaintedCreeper::new,MobCategory.MONSTER,0.6f,1.7f,register);ATTRIBUTES.put(creeper,Creeper.createAttributes().add(Attributes.MAX_HEALTH,30).build());
        EntityType<TaintedTree> tree=create("tainted_tree",TaintedTree::new,MobCategory.MONSTER,1f,7f,register);ATTRIBUTES.put(tree,Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,75).add(Attributes.ARMOR,1).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.KNOCKBACK_RESISTANCE,1).build());
        var grub=create("grub",Grub::new,MobCategory.MONSTER,0.3f,0.7f,register);ATTRIBUTES.put(grub,Grub.createAttributes().add(Attributes.MAX_HEALTH,8).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.FOLLOW_RANGE,8).build());
    }
}
