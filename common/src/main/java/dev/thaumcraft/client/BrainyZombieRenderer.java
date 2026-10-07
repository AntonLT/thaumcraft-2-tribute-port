package dev.thaumcraft.client;

import dev.thaumcraft.Thaumcraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.Zombie;

/** Original 64 by 32 biped UVs, retaining modern equipment rendering. */
public final class BrainyZombieRenderer extends AbstractZombieRenderer<Zombie,ZombieRenderState,ZombieModel<ZombieRenderState>> {
    private static ZombieModel<ZombieRenderState> model(){return new ZombieModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32).bakeRoot());}
    public BrainyZombieRenderer(EntityRendererProvider.Context context){
        super(context,model(),model(),ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR,context.getModelSet(),ZombieModel::new),ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR,context.getModelSet(),ZombieModel::new));
    }
    @Override public ZombieRenderState createRenderState(){return new ZombieRenderState();}
    @Override public Identifier getTextureLocation(ZombieRenderState state){return Thaumcraft.id("textures/legacy/bzombie.png");}
}
