package cn.zbx1425.resourcepackupdater.gui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

public class PreloadTextureResource implements Resource {

    private ResourceLocation resourceLocation;

    public PreloadTextureResource(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    @Override
    public ResourceLocation getLocation() {
        return resourceLocation;
    }

    @Override
    public InputStream getInputStream() {
        return getClass().getResourceAsStream("/assets/" + resourceLocation.getNamespace()
                + "/" + resourceLocation.getPath());
    }

    @Override
    public boolean hasMetadata() {
        return false;
    }

    @Nullable
    @Override
    public <T> T getMetadata(@NotNull MetadataSectionSerializer<T> metadataSectionSerializer) {
        return null;
    }

    @Override
    public String getSourceName() {
        return resourceLocation.toDebugFileName();
    }

    @Override
    public void close() throws IOException {

    }
}
