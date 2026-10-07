package dev.thaumcraft.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Minecraft.class)
public interface PortalMinecraftAccess {
    @Mutable @Accessor("levelRenderer") void thaumcraft$levelRenderer(LevelRenderer renderer);
}
