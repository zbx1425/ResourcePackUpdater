package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.drm.AssetEncryption;
import cn.zbx1425.resourcepackupdater.drm.ServerLockRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.FolderPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.ResourcePackFileNotFoundException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

@Mixin(FolderPackResources.class)
public abstract class FolderPackResourcesMixin extends AbstractPackResources {

    private FolderPackResourcesMixin(File file) { super(file); }

    @Shadow
    private File getFile(String string) { return null; }

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    void getResource(String resourcePath, CallbackInfoReturnable<InputStream> cir) throws IOException {
        if (file.equals(ResourcePackUpdater.CONFIG.packBaseDirFile)) {
            File file = this.getFile(resourcePath);
            if (file == null || ServerLockRegistry.shouldRefuseProvidingFile(resourcePath)) {
                throw new ResourcePackFileNotFoundException(this.file, resourcePath);
            }
            FileInputStream fis = new FileInputStream(file);
            cir.setReturnValue(AssetEncryption.wrapInputStream(fis));
            cir.cancel();
        }
    }

    @Inject(method = "hasResource", at = @At("HEAD"), cancellable = true)
    void hasResource(String resourcePath, CallbackInfoReturnable<Boolean> cir) {
        if (file.equals(ResourcePackUpdater.CONFIG.packBaseDirFile)) {
            if (ServerLockRegistry.shouldRefuseProvidingFile(resourcePath)) {
                cir.setReturnValue(false); cir.cancel();
            }
        }
    }
    @Inject(method = "getResources", at = @At("HEAD"), cancellable = true)
    void getResources(PackType type, String namespace, String path, Predicate<ResourceLocation> filter, CallbackInfoReturnable<Collection<ResourceLocation>> cir) {
        if (file.equals(ResourcePackUpdater.CONFIG.packBaseDirFile)) {
            if (ServerLockRegistry.shouldRefuseProvidingFile(null)) {
                cir.setReturnValue(Collections.emptyList()); cir.cancel();
            }
        }
    }
    @Inject(method = "getNamespaces", at = @At("HEAD"), cancellable = true)
    void getNamespaces(PackType type, CallbackInfoReturnable<Set<String>> cir) {
        if (file.equals(ResourcePackUpdater.CONFIG.packBaseDirFile)) {
            if (ServerLockRegistry.shouldRefuseProvidingFile(null)) {
                cir.setReturnValue(Collections.emptySet()); cir.cancel();
            }
        }
    }
}
