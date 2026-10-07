package dev.thaumcraft.entity;

import dev.thaumcraft.content.Content;
import dev.thaumcraft.content.ModSounds;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public final class TravelingTrunk extends PathfinderMob implements Container,MenuProvider {
    // Entity-local events: positive vanilla IDs can be intercepted before handleEntityEvent (63 is Sniffer).
    private static final byte EAT_EVENT=-1,ATTACK_EVENT=-2,HOP_EVENT=-3;
    private NonNullList<ItemStack> inventory=NonNullList.withSize(27,ItemStack.EMPTY);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> UPGRADES=net.minecraft.network.syncher.SynchedEntityData.defineId(TravelingTrunk.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> OPEN=net.minecraft.network.syncher.SynchedEntityData.defineId(TravelingTrunk.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private float lid,oldLid,squish,oldSquish;
    private int viewers;
    private int eatDelay;
    private int jumpDelay,angerLevel,attackDelay;
    private boolean hopped;
    private int firstUpgrade=-1,secondUpgrade=-1;
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(UPGRADES,0);builder.define(OPEN,false);}
    public boolean hasUpgrade(int upgrade){return (entityData.get(UPGRADES)&(1<<upgrade))!=0;}
    public int upgradeMask(){return entityData.get(UPGRADES);}
    public float lid(float partial){return net.minecraft.util.Mth.lerp(partial,oldLid,lid);}
    public float squish(float partial){return net.minecraft.util.Mth.lerp(partial,oldSquish,squish);}
    public int upgradeAt(int slot){return slot==0?firstUpgrade:secondUpgrade;}
    @Override public void startOpen(ContainerUser user){if(!level().isClientSide()&&user instanceof Player player&&!player.isSpectator()){viewers++;entityData.set(OPEN,true);playSound(SoundEvents.CHEST_OPEN,.5f,getRandom().nextFloat()*.1f+.9f);}}
    @Override public void stopOpen(ContainerUser user){if(!level().isClientSide()&&user instanceof Player player&&!player.isSpectator()){viewers=Math.max(0,viewers-1);entityData.set(OPEN,viewers>0);playSound(SoundEvents.CHEST_CLOSE,.5f,getRandom().nextFloat()*.1f+.9f);}}
    @Override public void tick(){
        oldLid=lid;oldSquish=squish;
        if(level().isClientSide())updateLid();
        boolean grounded=onGround();super.tick();
        if(onGround()&&!grounded)squish=-.5f;
        squish*=.6f;hopped=false;
    }
    private void updateLid(){
        if(!hopped){
            if(getDeltaMovement().y<0||entityData.get(OPEN))lid+=.015f;
            if(onGround()&&!entityData.get(OPEN))lid-=.1f;
        }
        if(entityData.get(OPEN))lid+=.035f;lid=Math.clamp(lid,0,.5f);
    }
    @Override public void handleEntityEvent(byte event){
        if(event==EAT_EVENT)lid=.15f;
        else if(event==ATTACK_EVENT)lid=Math.min(.5f,lid+.015f);
        else if(event==HOP_EVENT){squish=1;hopped=true;}
        else super.handleEntityEvent(event);
    }
    private void animate(byte event){handleEntityEvent(event);level().broadcastEntityEvent(this,event);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);
        if(key.equals(UPGRADES)){refreshDimensions();getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(hasUpgrade(2)?2:1);}
    }
    @Override protected EntityDimensions getDefaultDimensions(Pose pose){return super.getDefaultDimensions(pose).scale(hasUpgrade(5)?1.125f:1);}
    @Override public boolean fireImmune(){return true;}
    @Override public boolean canBreatheUnderwater(){return true;}
    @Override public boolean causeFallDamage(double distance,float multiplier,DamageSource source){return false;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.WOOD_STEP;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.ITEM_BREAK.value();}
    @Override protected float getSoundVolume(){return .5f;}
    @Override protected float getFlyingSpeed(){return hasUpgrade(0)?.04f:.03f;}
    public boolean installUpgrade(ItemStack stack){
        var definition=Content.entry(stack);if(definition==null||!definition.source_class().equals("ItemUpgrades"))return false;
        int meta=definition.meta();if((meta!=0&&meta!=1&&meta!=2&&meta!=5)||hasUpgrade(meta)||Integer.bitCount(upgradeMask())>=2)return false;
        if(firstUpgrade<0)firstUpgrade=meta;else secondUpgrade=meta;
        entityData.set(UPGRADES,upgradeMask()|1<<meta);
        if(meta==5){var expanded=NonNullList.withSize(36,ItemStack.EMPTY);for(int i=0;i<inventory.size();i++)expanded.set(i,inventory.get(i));inventory=expanded;}
        return true;
    }
    public ItemStack removeLatestUpgrade(ServerLevel level) {
        int removed=secondUpgrade>=0?secondUpgrade:firstUpgrade;
        if(removed<0)return ItemStack.EMPTY;
        if(secondUpgrade>=0)secondUpgrade=-1;else firstUpgrade=-1;
        entityData.set(UPGRADES,upgradeMask()&~(1<<removed));
        if(removed==5) {
            var smaller=NonNullList.withSize(27,ItemStack.EMPTY);
            for(int i=0;i<inventory.size();i++)if(i<27)smaller.set(i,inventory.get(i));else dropOverflow(level,inventory.get(i));
            inventory=smaller;
        }
        for(var entry:Content.ENTRIES)if(entry.source_class().equals("ItemUpgrades")&&entry.meta()==removed)return new ItemStack(Content.item(entry.id()));
        return ItemStack.EMPTY;
    }
    private void dropOverflow(ServerLevel level,ItemStack stack){
        double x=getX()+getRandom().nextFloat()*.8f+.1f,y=getY()+getRandom().nextFloat()*.8f+.1f,z=getZ()+getRandom().nextFloat()*.8f+.1f;
        while(!stack.isEmpty()){
            var drop=new ItemEntity(level,x,y,z,stack.split(getRandom().nextInt(21)+10));
            drop.setDeltaMovement(getRandom().nextGaussian()*.05,getRandom().nextGaussian()*.05+.2,getRandom().nextGaussian()*.05);
            level.addFreshEntity(drop);
        }
    }
    private UUID owner;
    private boolean sitting;
    public boolean staying(){return sitting;}
    public void toggleStay(Player player){if(!stillValid(player))return;playSound(SoundEvents.WOOD_STEP,.3f,sitting?.6f:.8f);sitting=!sitting;player.sendSystemMessage(sitting?Component.translatableWithFallback("message.thaumcraft2tp.trunk.stay", "The trunk will remain here."):Component.translatableWithFallback("message.thaumcraft2tp.trunk.follow", "The trunk will now follow you."));}
    public TravelingTrunk(EntityType<? extends TravelingTrunk> type,Level level){
        super(type,level);setPersistenceRequired();xpReward=7;jumpDelay=getRandom().nextInt(20)+10;
        // Run the recovered hop routine in the control phase, before vanilla applies movement.
        moveControl=new MoveControl(this){@Override public void tick(){tickActions((ServerLevel)level());}};
    }
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.ARMOR,1).add(Attributes.MOVEMENT_SPEED,.1).add(Attributes.ATTACK_DAMAGE,1);}
    public void setOwner(UUID owner){this.owner=owner;}
    @Override public void aiStep() {
        resetFallDistance();
        super.aiStep();
        if(!(level() instanceof ServerLevel level) || !isAlive())return;
        if(getHealth()<=getMaxHealth()/2&&eatDelay==0)for(var food:inventory) {
            var nutrition=food.get(net.minecraft.core.component.DataComponents.FOOD);
            if(nutrition!=null){heal(nutrition.nutrition());food.shrink(1);eatDelay=10+getRandom().nextInt(15);fed(level);break;}
        }
        if(hasUpgrade(1))pullItems(level);
    }
    private void tickActions(ServerLevel level){
        if(angerLevel>0)angerLevel--;
        if(eatDelay>0)eatDelay--;
        if(attackDelay>0)attackDelay--;
        Player player=owner==null?null:level.getPlayerByUUID(owner);
        if(player==null){xxa=zza=0;return;}
        if(!sitting&&(distanceToSqr(player)>400||isInWater()&&distanceToSqr(player)>64&&!player.isInWater())&&teleportToPlayer(level,player))return;
        if((angerLevel==0||getTarget()==null)&&hasUpgrade(2)){
            var nearby=level.getEntitiesOfClass(LivingEntity.class,new AABB(getX(),getY(),getZ(),getX()+1,getY()+1,getZ()+1).inflate(16,4,16));
            if(!nearby.isEmpty()){
                var candidate=nearby.get(getRandom().nextInt(nearby.size()));
                if(candidate instanceof Monster&&hasLineOfSight(candidate)){setTarget(candidate);angerLevel=600;}
            }
        }
        boolean move=false;
        LivingEntity target=getTarget();
        if(angerLevel>0&&target!=null&&target!=player){
            lookAt(target,10,20);move=true;
            if(attackDelay==0&&distanceToSqr(target)<2.25&&target.getBoundingBox().maxY>getBoundingBox().minY&&target.getBoundingBox().minY<getBoundingBox().maxY){
                attackDelay=10+getRandom().nextInt(5);doHurtTarget(level,target);
            }
            if(!target.isAlive()){setTarget(null);angerLevel=5;}
        }
        if(distanceToSqr(player)>25&&angerLevel==0&&!sitting){lookAt(player,10,20);move=true;}
        if(onGround()&&jumpDelay--<=0&&move){
            jumpDelay=(getRandom().nextInt(10)+5)/3;
            getJumpControl().jump();animate(HOP_EVENT);
            setSpeed((float)getAttributeValue(Attributes.MOVEMENT_SPEED));
            xxa=1-getRandom().nextFloat()*2;zza=hasUpgrade(0)?8:6;
            playSound(SoundEvents.CHEST_CLOSE,.1f,getRandom().nextFloat()*.1f+.9f);
        }else if(onGround())xxa=zza=0;
        updateLid();
    }
    private boolean teleportToPlayer(ServerLevel level,Player player){
        BlockPos center=BlockPos.containing(player.getX(),player.getBoundingBox().minY,player.getZ());
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            if(Math.abs(x)<2&&Math.abs(z)<2)continue;
            BlockPos pos=center.offset(x,0,z);
            if(!level.hasChunkAt(pos)||!level.getBlockState(pos.below()).isSolidRender()||level.getBlockState(pos).isSolidRender()||level.getBlockState(pos.above()).isSolidRender())continue;
            var destination=pos.getBottomCenter();
            if(level.noCollision(this,getBoundingBox().move(destination.subtract(position())))){
                setPos(destination);xxa=zza=0;setTarget(null);angerLevel=0;
                playSound(SoundEvents.ENDERMAN_TELEPORT,.5f,1);particles(level,false);return true;
            }
        }
        return false;
    }
    @Override public boolean hurtServer(ServerLevel level,DamageSource source,float damage){
        boolean hurt=super.hurtServer(level,source,damage);
        if(hurt){
            Entity attacker=source.getDirectEntity();
            if(attacker instanceof AbstractArrow arrow)attacker=arrow.getOwner();
            if(attacker instanceof LivingEntity living){setTarget(living);angerLevel=300;}
        }
        return hurt;
    }
    @Override public boolean doHurtTarget(ServerLevel level,Entity target){
        if(target instanceof Player player&&player.getUUID().equals(owner))return false;
        // Preserve the recovered Java's effective damage, including the Rage bonus.
        boolean hurt=target.hurtServer(level,damageSources().mobAttack(this),hasUpgrade(2)?2:1);
        animate(ATTACK_EVENT);playSound(SoundEvents.BLAZE_HURT,.5f,getRandom().nextFloat()*.1f+.9f);return hurt;
    }
    private void fed(ServerLevel level){
        playSound(getHealth()==getMaxHealth()?SoundEvents.PLAYER_BURP:SoundEvents.GENERIC_EAT.value(),.5f,getRandom().nextFloat()*.5f+.5f);
        animate(EAT_EVENT);particles(level,true);
    }
    private void particles(ServerLevel level,boolean hearts){
        for(int i=0;i<(hearts?1:7);i++)level.sendParticles(hearts?ParticleTypes.HEART:ParticleTypes.POOF,
            getX()+getRandom().nextFloat()*getBbWidth()*2-getBbWidth(),getY()+.5+getRandom().nextFloat()*getBbHeight(),getZ()+getRandom().nextFloat()*getBbWidth()*2-getBbWidth(),
            0,getRandom().nextGaussian()*.02,getRandom().nextGaussian()*.02,getRandom().nextGaussian()*.02,1);
    }
    private void pullItems(ServerLevel level){
        var center=new AABB(getX()-.5,getY()-.5,getZ()-.5,getX()+.5,getY()+.5,getZ()+.5);
        for(var drop:level.getEntitiesOfClass(ItemEntity.class,center)){
            ItemStack stack=drop.getItem();if(stack.is(dev.thaumcraft.content.ModTags.TRUNK_FORBIDDEN))continue;int before=stack.getCount();
            // Merge existing stacks before using empty slots; retain all item components.
            for(var current:inventory)if(!current.isEmpty()&&ItemStack.isSameItemSameComponents(current,stack)){
                int amount=Math.min(stack.getCount(),Math.max(0,getMaxStackSize(current)-current.getCount()));current.grow(amount);stack.shrink(amount);
                if(stack.isEmpty())break;
            }
            for(int i=0;i<inventory.size()&&!stack.isEmpty();i++)if(inventory.get(i).isEmpty())inventory.set(i,stack.split(getMaxStackSize(stack)));
            if(stack.getCount()<before){playSound(SoundEvents.GENERIC_EAT.value(),.5f,getRandom().nextFloat()*.5f+.5f);animate(EAT_EVENT);}
            if(stack.isEmpty())drop.discard();else drop.setItem(stack);
        }
        if(inventory.stream().noneMatch(ItemStack::isEmpty))return;
        for(var drop:level.getEntitiesOfClass(ItemEntity.class,center.inflate(2.5))){
            var direction=drop.position().subtract(position());double distance=direction.length();
            if(distance>0)drop.setDeltaMovement(drop.getDeltaMovement().subtract(direction.scale(.2/distance)));
        }
    }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand) {
        if(owner!=null && !owner.equals(player.getUUID()))return InteractionResult.FAIL;
        if(!level().isClientSide()) {
            if(owner==null)owner=player.getUUID();
            var held=player.getItemInHand(hand);
            var food=held.get(net.minecraft.core.component.DataComponents.FOOD);
            if(food!=null&&getHealth()<getMaxHealth()) {heal(food.nutrition());held.consume(1,player);fed((ServerLevel)level());return InteractionResult.SUCCESS;}
            if(held.getItem() instanceof dev.thaumcraft.item.ArcanaItem upgrade&&upgrade.entry().source_class().equals("ItemUpgrades")) {
                if(installUpgrade(held)){held.consume(1,player);playSound(ModSounds.event("upgrade"),.4f,1);}return InteractionResult.SUCCESS;
            }
            if(held.getItem() instanceof dev.thaumcraft.item.ArcanaItem arcana&&arcana.entry().source_class().equals("ItemWandReversal")) {
                var recovered=removeLatestUpgrade((ServerLevel)level());
                if(recovered.isEmpty()) {
                    Containers.dropContents(level(),blockPosition(),this);clearContent();
                    recovered=new ItemStack(Content.item("traveling_trunk"));particles((ServerLevel)level(),false);discard();
                }
                if(getRandom().nextInt(6)!=0){
                    var drop=new ItemEntity(level(),getX(),getY(),getZ(),recovered);
                    drop.setDeltaMovement(player.position().subtract(position()).scale(.1));level().addFreshEntity(drop);playSound(ModSounds.event("zap"),.5f,1);
                }else playSound(SoundEvents.FIRE_EXTINGUISH,.33f,1.3f+getRandom().nextFloat()*.2f);
                dev.thaumcraft.item.ArcanaItem.charge(held,player,hand,1);return InteractionResult.SUCCESS;
            }
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean killedByPlayer) {
        super.dropCustomDeathLoot(level,source,killedByPlayer);Containers.dropContents(level,blockPosition(),this);clearContent();
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);ContainerHelper.saveAllItems(output,inventory);if(owner!=null)output.putString("trunk_owner",owner.toString());output.putBoolean("sitting",sitting);output.putInt("upgrades",upgradeMask());output.putInt("first_upgrade",firstUpgrade);output.putInt("second_upgrade",secondUpgrade);output.putInt("anger",angerLevel);
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);entityData.set(UPGRADES,input.getIntOr("upgrades",0)&39);inventory=NonNullList.withSize(hasUpgrade(5)?36:27,ItemStack.EMPTY);ContainerHelper.loadAllItems(input,inventory);
        firstUpgrade=input.getIntOr("first_upgrade",-1);secondUpgrade=input.getIntOr("second_upgrade",-1);
        if(firstUpgrade<0)for(int upgrade:new int[]{0,1,2,5})if(hasUpgrade(upgrade)){if(firstUpgrade<0)firstUpgrade=upgrade;else secondUpgrade=upgrade;}
        String id=input.getStringOr("trunk_owner","");try{owner=id.isEmpty()?null:UUID.fromString(id);}catch(IllegalArgumentException ignored){owner=null;}sitting=input.getBooleanOr("sitting",false);angerLevel=Math.max(0,input.getIntOr("anger",0));
    }
    @Override public Component getDisplayName(){return getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory playerInventory,Player player){return new dev.thaumcraft.machine.TrunkMenu(id,playerInventory,this,hasUpgrade(5)?4:3);}
    @Override public int getContainerSize(){return inventory.size();}
    @Override public boolean isEmpty(){return inventory.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return inventory.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){return ContainerHelper.removeItem(inventory,slot,count);}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(inventory,slot);}
    @Override public void setItem(int slot,ItemStack stack){inventory.set(slot,stack);stack.limitSize(getMaxStackSize(stack));}
    @Override public void setChanged(){}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return !stack.is(dev.thaumcraft.content.ModTags.TRUNK_FORBIDDEN);}
    @Override public boolean stillValid(Player player){return isAlive() && distanceToSqr(player)<64 && (owner==null || owner.equals(player.getUUID()));}
    @Override public void clearContent(){inventory.clear();}
}
