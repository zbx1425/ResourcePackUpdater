package cn.zbx1425.resourcepackupdater.gui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;

public class PreloadTextureResource extends Resource {

    private final ResourceLocation resourceLocation;

    public PreloadTextureResource(ResourceLocation resourceLocation) {
        super(resourceLocation.toDebugFileName(), InputStream::nullInputStream);
        this.resourceLocation = resourceLocation;
    }

    @Override
    public InputStream open() {
        return getClass().getResourceAsStream("/assets/" + resourceLocation.getNamespace() + "/" + resourceLocation.getPath());
    }
}
