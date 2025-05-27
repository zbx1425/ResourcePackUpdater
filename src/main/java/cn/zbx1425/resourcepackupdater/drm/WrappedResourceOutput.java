package cn.zbx1425.resourcepackupdater.drm;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.InputStream;

public class WrappedResourceOutput implements PackResources.ResourceOutput {

    private final PackResources.ResourceOutput original;

    public WrappedResourceOutput(PackResources.ResourceOutput original) {
        this.original = original;
    }

    @Override
    public void accept(ResourceLocation resourceLocation, IoSupplier<InputStream> inputStreamIoSupplier) {
        original.accept(resourceLocation, () -> AssetEncryption.wrapInputStream(inputStreamIoSupplier.get()));
    }
}
