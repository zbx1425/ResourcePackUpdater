package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.gui.GlHelper;
import cn.zbx1425.resourcepackupdater.gui.PreloadTextureResource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.SimpleReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleReloadableResourceManager.class)
public class MultiPackResourceManagerMixin {

    @Inject(at = @At("HEAD"), method = "getResource", cancellable = true)
    void getResource(ResourceLocation resourceLocation, CallbackInfoReturnable<Resource> cir) {
        if (resourceLocation.getNamespace().equals(ResourcePackUpdater.MOD_ID)) {
            cir.setReturnValue(new PreloadTextureResource(resourceLocation));
            cir.cancel();
        }
    }
}
