package dev.thaumcraft.client;

import dev.thaumcraft.Thaumcraft;
import dev.thaumcraft.entity.ModEntities;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;

public final class ClientRegistration {
    @FunctionalInterface public interface Registrar {<T extends Entity> void register(EntityType<? extends T> type,EntityRendererProvider<T> renderer);}
    private ClientRegistration() {}
    private static boolean connected;
    public static void init(){dev.thaumcraft.gameplay.AddonData.clientThread=()->net.minecraft.client.Minecraft.getInstance().isSameThread();
        dev.thaumcraft.gameplay.AddonData.clientRegistries=()->{var connection=net.minecraft.client.Minecraft.getInstance().getConnection();return connection==null?null:connection.registryAccess();};}
    public static void tick() {
        boolean present=net.minecraft.client.Minecraft.getInstance().getConnection()!=null;
        if(connected&&!present){dev.thaumcraft.gameplay.ClientResearch.update(java.util.List.of());dev.thaumcraft.gameplay.AddonData.clearClient();}
        connected=present;
        if(Boolean.getBoolean("thaumcraft.clientSmoke"))try {
            Class.forName("dev.thaumcraft.test.ClientSmokeTests").getMethod("tick").invoke(null);
        } catch(ReflectiveOperationException e){throw new IllegalStateException("Client smoke fixture failed",e);}
    }
    @SuppressWarnings("unchecked") private static <T extends Entity> EntityType<T> type(String id){return (EntityType<T>)ModEntities.TYPES.get(id);}
    public static void entities(Registrar r) {
        dev.thaumcraft.content.VisualEffects.ambient=dev.thaumcraft.client.legacy.LegacyVisuals::ambient;
        dev.thaumcraft.content.VisualEffects.entity=entity->{if(entity instanceof dev.thaumcraft.entity.ArcaneMote mote)ArcaneMoteRenderer.ambient(mote);else dev.thaumcraft.client.legacy.LegacyEntityVisuals.ambient(entity);};
        r.register(ModEntities.BRAINY_ZOMBIE,BrainyZombieRenderer::new);
        r.register(ModEntities.THAUM_SLIME,ThaumSlimeRenderer::new);
        r.register(ModEntities.WISP,context->new LegacyEffectRenderer<>(context,true));
        r.register(ModEntities.BONE_ARROW,context->new ArrowRenderer<dev.thaumcraft.entity.BoneArrow,ArrowRenderState>(context) {
            @Override public ArrowRenderState createRenderState(){return new ArrowRenderState();}
            @Override protected Identifier getTextureLocation(ArrowRenderState state){return Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");}
        });
        r.register(ModEntities.ARCANE_MOTE,ArcaneMoteRenderer::new);
        r.register(ModEntities.LIGHTNING,LightningEffectRenderer::new);
        r.register(ModEntities.RELIC,context->new LegacyEffectRenderer<>(context,false));
        r.register(ModEntities.CARPET,RelicModelRenderer::new);
        r.register(ModEntities.TRUNK,TrunkRenderer::new);
        r.register(ModEntities.SKELETON_ALLY,context->new SkeletonRenderer(context){
            @Override public Identifier getTextureLocation(SkeletonRenderState state){return Thaumcraft.id("textures/legacy/skeleton.png");}
        });
        for(String kind:new String[]{"cow","pig","chicken","sheep","villager","tree"})
            r.register(ClientRegistration.<Monster>type("tainted_"+kind),context->new LegacyMobRenderer(context,kind));
        r.register(ClientRegistration.<Creeper>type("tainted_creeper"),context->new CreeperRenderer(context){
            @Override public Identifier getTextureLocation(CreeperRenderState state){return Thaumcraft.id("textures/legacy/creeper.png");}
        });
        r.register(ClientRegistration.<Silverfish>type("grub"),context->new SilverfishRenderer(context) {
            @Override public Identifier getTextureLocation(LivingEntityRenderState state){return Thaumcraft.id("textures/legacy/grub.png");}
        });
    }
}
