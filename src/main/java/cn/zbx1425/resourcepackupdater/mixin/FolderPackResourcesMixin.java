package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.io.AssetEncryption;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.FolderPackResources;
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

@Mixin(FolderPackResources.class)
public abstract class FolderPackResourcesMixin extends AbstractPackResources {

    private FolderPackResourcesMixin(File file) { super(file); }

    @Shadow
    private File getFile(String string) { return null; }

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    void getResource(String resourcePath, CallbackInfoReturnable<InputStream> cir) throws IOException {
        if (file.equals(ResourcePackUpdater.CONFIG.packBaseDirFile)) {
            File file = this.getFile(resourcePath);
            if (file == null) {
                throw new ResourcePackFileNotFoundException(this.file, resourcePath);
            }
            FileInputStream fis = new FileInputStream(file);
            cir.setReturnValue(AssetEncryption.wrapInputStream(fis));
            cir.cancel();
        }
    }
}
