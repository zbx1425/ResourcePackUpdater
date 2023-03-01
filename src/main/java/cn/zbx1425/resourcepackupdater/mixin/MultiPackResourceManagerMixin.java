package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.gui.PreloadTextureResource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin {

    @Inject(at = @At("HEAD"), method = "getResource", cancellable = true)
#if MC_VERSION >= "11900"
    void getResource(ResourceLocation resourceLocation, CallbackInfoReturnable<Optional<Resource>> cir) {
#else
    void getResource(ResourceLocation resourceLocation, CallbackInfoReturnable<Resource> cir) {
#endif
        if (resourceLocation.getNamespace().equals(ResourcePackUpdater.MOD_ID)) {
#if MC_VERSION >= "11900"
            cir.setReturnValue(Optional.of(new PreloadTextureResource(resourceLocation)));
#else
            cir.setReturnValue(new PreloadTextureResource(resourceLocation));
#endif
            cir.cancel();
        }
    }
}
