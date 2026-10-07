package dev.thaumcraft.mixin;

import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface PortalCameraInvoker {
    @Invoker("setupPerspective") void thaumcraft$perspective(float near,float far,float fov,float width,float height);
    @Invoker("prepareCullFrustum") void thaumcraft$frustum(Matrix4fc view,Matrix4f projection,Vec3 position);
}
