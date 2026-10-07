package dev.alts.tc2tppatches.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.thaumcraft.content.Content;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Draws goggles from a curio slot with the equipment asset the helmet slot uses. Client only, and only loaded with Curios. */
final class GogglesCurioRenderer implements ICurioRenderer {
    // Helmets inflate by 1.0; a little more keeps goggles worn over one from z-fighting with it.
    private static final CubeDeformation OVER_HELMET=new CubeDeformation(1.1F);
    private final HumanoidModel<HumanoidRenderState> model=new HumanoidModel<>(LayerDefinition.create(
            HumanoidModel.createArmorMeshSet(LayerDefinitions.INNER_ARMOR_DEFORMATION,OVER_HELMET).head(),64,32).bakeRoot());

    static void register(){ICurioRenderer.register(Content.item("goggles_of_revealing"),GogglesCurioRenderer::new);}

    @Override
    public <S extends LivingEntityRenderState,M extends EntityModel<? super S>> void render(ItemStack stack,SlotContext slotContext,PoseStack poseStack,
            SubmitNodeCollector collector,int light,S state,RenderLayerParent<S,M> parent,EntityRendererProvider.Context context,float yRotation,float xRotation) {
        var equippable=stack.get(DataComponents.EQUIPPABLE);
        if(!(state instanceof HumanoidRenderState humanoid)||equippable==null||equippable.assetId().isEmpty())return;
        // The submitted model is posed from the render state, so it follows the head, crouching and swimming.
        context.getEquipmentRenderer().renderLayers(EquipmentClientInfo.LayerType.HUMANOID,equippable.assetId().get(),model,humanoid,stack,poseStack,collector,light,state.outlineColor);
    }
}
