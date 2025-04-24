package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.drm.AssetEncryption;
import cn.zbx1425.resourcepackupdater.drm.ServerLockRegistry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.DetectedVersion;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mixin(PathPackResources.class)
public abstract class FolderPackResourcesMixin extends AbstractPackResources {

    @Unique
    private Path canonicalRoot;

    @Unique
    private Path getCanonicalRoot() {
        if (canonicalRoot == null) {
            try {
                canonicalRoot = root.toRealPath();
            } catch (IOException e) {
                canonicalRoot = root;
            }
        }
        return canonicalRoot;
    }

    private FolderPackResourcesMixin(String string, boolean bl) { super(string, bl); }

    @Shadow @Final private Path root;

    @Inject(method = "getRootResource", at = @At("HEAD"), cancellable = true)
    public void getRootResource(String[] elements, CallbackInfoReturnable<IoSupplier<InputStream>> cir) {
        if (getCanonicalRoot().equals(ResourcePackUpdater.CONFIG.packBaseDirFile.value)) {
            Path path = FileUtil.resolvePath(this.root, List.of(elements));
            if (Files.exists(path)) {
                if (Arrays.equals(elements, new String[] { "pack.mcmeta" })) {
                    cir.setReturnValue(() -> patchPackMeta(AssetEncryption.wrapInputStream(new FileInputStream(path.toFile()))));
                } else {
                    cir.setReturnValue(() -> AssetEncryption.wrapInputStream(new FileInputStream(path.toFile())));
                }
                cir.cancel();
            } else {
                cir.setReturnValue(null);
                cir.cancel();
            }
        }
    }

    @Inject(method = "getResource(Lnet/minecraft/server/packs/PackType;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/server/packs/resources/IoSupplier;", at = @At("HEAD"), cancellable = true)
    void getResource(PackType packType, ResourceLocation location, CallbackInfoReturnable<IoSupplier<InputStream>> cir) throws IOException {
        if (getCanonicalRoot().equals(ResourcePackUpdater.CONFIG.packBaseDirFile.value)) {
            Path path = this.root.resolve(packType.getDirectory()).resolve(location.getNamespace());
            var decomposeResult = FileUtil.decomposePath(location.getPath()).get();
            if (decomposeResult.left().isEmpty()) {
                cir.setReturnValue(null);
                cir.cancel();
                return;
            }
            Path path2 = FileUtil.resolvePath(path, decomposeResult.left().get());
            if (ServerLockRegistry.shouldRefuseProvidingFile(path2.toString())) {
                cir.setReturnValue(null);
                cir.cancel();
                return;
            }
            if (Files.exists(path2)) {
                cir.setReturnValue(() -> AssetEncryption.wrapInputStream(new FileInputStream(path2.toFile())));
                cir.cancel();
            } else {
                cir.setReturnValue(null);
                cir.cancel();
            }
        }
    }

    @Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
    void getResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput, CallbackInfo ci) {
        if (getCanonicalRoot().equals(ResourcePackUpdater.CONFIG.packBaseDirFile.value)) {
            if (ServerLockRegistry.shouldRefuseProvidingFile(null)) {
                ci.cancel();
            }
        }
    }
    @Inject(method = "getNamespaces", at = @At("HEAD"), cancellable = true)
    void getNamespaces(PackType type, CallbackInfoReturnable<Set<String>> cir) {
        if (getCanonicalRoot().equals(ResourcePackUpdater.CONFIG.packBaseDirFile.value)) {
            if (ServerLockRegistry.shouldRefuseProvidingFile(null)) {
                cir.setReturnValue(Collections.emptySet()); cir.cancel();
            }
        }
    }

    @Unique
    private static InputStream patchPackMeta(InputStream inputStream) throws IOException {
        JsonObject jsonObject = JsonParser.parseString(IOUtils.toString(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
        jsonObject.getAsJsonObject("pack").addProperty("pack_format", DetectedVersion.tryDetectVersion().getPackVersion(PackType.CLIENT_RESOURCES));
        return IOUtils.toInputStream(jsonObject.toString(), StandardCharsets.UTF_8);
    }
}
