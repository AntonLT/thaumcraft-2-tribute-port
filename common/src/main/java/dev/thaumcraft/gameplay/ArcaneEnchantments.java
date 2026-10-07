package dev.thaumcraft.gameplay;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.content.Content;
import dev.thaumcraft.machine.MachineBlockEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ArcaneEnchantments {
    private static final Map<String,Integer> RESEARCH=Map.of("vampiric",57,"soulstealer",58,"repair",59,"relic",60,"potency",67);
    private static final java.util.Set<String> STEP_BOOTS=java.util.Set.of("ItemStridingBoots","ItemSevenBoots","ItemStompBoots");
    private static final net.minecraft.world.entity.ai.attributes.AttributeModifier BOOT_STEP=new net.minecraft.world.entity.ai.attributes.AttributeModifier(Thaumcraft.id("arcane_boot_step"),.4,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
    private ArcaneEnchantments() {}
    /** Armor boots take precedence; accessory arcane boots supply movement effects with other footwear. */
    public static ItemStack wornBoots(net.minecraft.world.entity.player.Player player) {
        var feet=player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
        if(arcaneBoots(feet))return feet;
        return dev.thaumcraft.api.IntegrationHooks.worn(player,ArcaneEnchantments::arcaneBoots);
    }
    private static boolean arcaneBoots(ItemStack stack) {
        var entry=Content.entry(stack);return entry!=null&&STEP_BOOTS.contains(entry.source_class());
    }
    private static final java.util.Map<LivingEntity,Mark> MARKS=new java.util.WeakHashMap<>();
    private static final class Mark {
        long soulUntil,relicUntil,boneUntil;int soul,relic;java.util.UUID boneOwner;
    }
    public static void markAttack(ServerLevel level,LivingEntity victim,net.minecraft.world.damagesource.DamageSource source) {
        if(!(source.getEntity() instanceof ServerPlayer player)||source.getDirectEntity()!=player)return;
        int soul=level(level,player.getMainHandItem(),"soulstealer"),relic=level(level,player.getMainHandItem(),"relic");
        if(soul==0&&relic==0)return;
        var mark=MARKS.computeIfAbsent(victim,e->new Mark());
        if(soul>0){mark.soul=soul;mark.soulUntil=level.getGameTime()+40;}
        if(relic>0){mark.relic=relic;mark.relicUntil=level.getGameTime()+40;}
    }
    public static void markBone(LivingEntity victim,net.minecraft.world.entity.Entity owner) {
        if(victim.typeHolder().is(net.minecraft.tags.EntityTypeTags.UNDEAD))return;
        var mark=MARKS.computeIfAbsent(victim,e->new Mark());mark.boneUntil=victim.level().getGameTime()+40;mark.boneOwner=owner==null?null:owner.getUUID();
    }

    public static int level(ServerLevel level,ItemStack stack,String enchantment) {
        if(stack.isEmpty())return 0;
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT,Thaumcraft.id(enchantment))).map(e->EnchantmentHelper.getItemEnchantmentLevel(e,stack)).orElse(0);
    }
    public static boolean available(ServerLevel level,MachineBlockEntity machine,Holder<Enchantment> enchantment) {
        Integer project=researchProject(enchantment);
        if(project==null)return true;
        var data=ArcaneWorldData.researchData(level);return machine.owner()==null?data.globallyKnown(project):data.knows(machine.owner(),project);
    }
    public static boolean available(ServerPlayer player,Holder<Enchantment> enchantment) {
        Integer project=researchProject(enchantment);
        return project==null||ArcaneWorldData.knows(player,project);
    }
    private static Integer researchProject(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().filter(key->key.identifier().getNamespace().equals(Thaumcraft.MOD_ID))
                .map(key->RESEARCH.get(key.identifier().getPath())).orElse(null);
    }
    public static void onHit(ServerLevel level,LivingEntity victim,net.minecraft.world.damagesource.DamageSource source) {
        if(!(source.getEntity() instanceof ServerPlayer player)||source.getDirectEntity()!=player)return;
        int vamp=level(level,player.getMainHandItem(),"vampiric");
        if(vamp>0&&!victim.typeHolder().is(net.minecraft.tags.EntityTypeTags.UNDEAD)&&level.getRandom().nextInt(8)<vamp){
            player.heal(1);
            dev.thaumcraft.content.ModSounds.playAt(level,victim.getX(),victim.getY(),victim.getZ(),"gore",net.minecraft.sounds.SoundSource.PLAYERS,1,.9f+level.getRandom().nextFloat()*.1f);
            for(int i=0;i<10;i++)markEffect(level,victim,dev.thaumcraft.network.EquipmentEffect.VAMPIRIC,1.2f);
        }
    }
    public static void tick(ServerLevel level) {
        for(var player:level.players()) {
            var worn=wornBoots(player);var boots=Content.entry(worn);
            if(boots!=null&&boots.source_class().equals("ItemStompBoots"))dev.thaumcraft.item.ArcanaItem.stompTick(worn,level,player);
            var step=player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
            boolean boosted=boots!=null&&!player.isPassenger()&&STEP_BOOTS.contains(boots.source_class());
            // Reusing one modifier instance lets vanilla skip the attribute resync while it stays applied.
            if(step!=null){if(boosted)step.addOrUpdateTransientModifier(BOOT_STEP);else step.removeModifier(BOOT_STEP.id());}
        }
        var watched=MARKS.entrySet().iterator();
        while(watched.hasNext()) {
            var entry=watched.next();var victim=entry.getKey();var mark=entry.getValue();
            if(victim.level()!=level)continue;
            if(!victim.isAlive()&&mark.boneUntil>=level.getGameTime()&&mark.boneUntil>0) {
                var ally=dev.thaumcraft.entity.ModEntities.SKELETON_ALLY.create(level,net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                if(ally!=null){
                    ally.setPos(victim.position());ally.setLifetime(300);
                    if(level.addFreshEntity(ally)){
                        level.broadcastEntityEvent(ally,(byte)20);
                    }
                }
                watched.remove();
                continue;
            }
            // Original watcher drew one mark particle per tick while the victim stayed alive and marked.
            if(victim.isAlive()) {
                if(mark.soul>0&&mark.soulUntil>=level.getGameTime())markEffect(level,victim,dev.thaumcraft.network.EquipmentEffect.SOUL_MARK,1);
                if(mark.boneUntil>0&&mark.boneUntil>=level.getGameTime())markEffect(level,victim,dev.thaumcraft.network.EquipmentEffect.BONE_MARK,1);
            }
            if(victim.isRemoved()||Math.max(mark.boneUntil,Math.max(mark.soulUntil,mark.relicUntil))<level.getGameTime())watched.remove();
        }
        if(level.getGameTime()%40!=0)return;
        var aura=ArcaneWorldData.get(level);
        for(var player:level.players())for(int slot=0;slot<player.getInventory().getContainerSize();slot++) {
            var stack=player.getInventory().getItem(slot);
            // Never spend a partial remainder on a repair that cannot happen.
            if(stack.isDamaged()&&level(level,stack,"repair")>0&&aura.aura(level,player.blockPosition()).vis()>=.5f&&aura.drainAura(level,player.blockPosition(),.5f,false)==.5f)stack.setDamageValue(stack.getDamageValue()-1);
        }
    }
    /** Server-picked particle origin around the victim, so viewers need no entity size or mark state. */
    private static void markEffect(ServerLevel level,LivingEntity victim,int kind,float height) {
        var r=level.getRandom();
        dev.thaumcraft.network.EquipmentEffect.send(level,kind,new net.minecraft.world.phys.Vec3(victim.getX()+r.nextFloat()-r.nextFloat(),victim.getY()+r.nextFloat()*victim.getBbHeight()*height,victim.getZ()+r.nextFloat()-r.nextFloat()));
    }
    public static void loot(LootContext context,List<ItemStack> loot) {
        if(!(context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof LivingEntity victim)||victim.getMaxHealth()<10)return;
        var level=context.getLevel();var random=context.getRandom();
        var mark=MARKS.get(victim);
        int soul=0,relic=0;
        if(mark!=null) {
            soul=mark.soulUntil>=level.getGameTime()?mark.soul:0;relic=mark.relicUntil>=level.getGameTime()?mark.relic:0;mark.soul=0;mark.relic=0;
        } else if(context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY) instanceof ServerPlayer player) {
            var damage=context.getOptionalParameter(LootContextParams.DAMAGE_SOURCE);
            if(damage!=null&&damage.getDirectEntity()==player){soul=level(level,player.getMainHandItem(),"soulstealer");relic=level(level,player.getMainHandItem(),"relic");}
        }
        if(soul>0&&(victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon||random.nextInt(100)<=victim.getMaxHealth()/2+soul*5))loot.add(new ItemStack(Content.item("soul_fragment"),victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon?11:1));
        if(relic<=0)return;
        relicLoot(random,loot,relic);
        if(victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)for(int i=0;i<6;i++)relicLoot(random,loot,relic+1);
    }
    private static void relicLoot(net.minecraft.util.RandomSource random,List<ItemStack> loot,int relic) {
        var choices=new ArrayList<String>();
        for(int a=0;a<relic*6;a++)for(int b=0;b<2;b++){choices.add(artifact("ItemArtifactLost",b));choices.add(artifact("ItemArtifactForbidden",b));}
        if(relic>1)for(int a=0;a<relic*3;a++)for(int b=2;b<4;b++) {
            choices.add(artifact("ItemArtifactLost",b));choices.add(artifact("ItemArtifactForbidden",b));
            if(a<3){choices.add(artifact("ItemArtifactEldritch",b-2));choices.add(artifact("ItemArtifactTainted",b-2));}
            if(a==0&&relic>2){choices.add(artifact("ItemArtifactEldritch",b));choices.add(artifact("ItemArtifactTainted",b));}
        }
        if(relic>2)for(int a=0;a<relic;a++){choices.add(artifact("ItemArtifactLost",4));choices.add(artifact("ItemArtifactForbidden",4));}
        int result=random.nextInt(choices.size()+200);if(result<choices.size())loot.add(new ItemStack(Content.item(choices.get(result))));
    }
    private static String artifact(String cls,int meta){return Content.ENTRIES.stream().filter(e->e.source_class().equals(cls)&&e.meta()==meta).findFirst().orElseThrow().id();}
}
