package cn.zbx1425.resourcepackupdater.gui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;

#if MC_VERSION >= "11900"
public class PreloadTextureResource extends Resource {
#else
public class PreloadTextureResource implements Resource {
#endif

    private final ResourceLocation resourceLocation;

    public PreloadTextureResource(ResourceLocation resourceLocation) {
#if MC_VERSION >= "11900"
        super(resourceLocation.toDebugFileName(), InputStream::nullInputStream);
#endif
        this.resourceLocation = resourceLocation;
    }

#if MC_VERSION >= "11900"
    @Override
    public InputStream open() {
        return getClass().getResourceAsStream("/assets/" + resourceLocation.getNamespace() + "/" + resourceLocation.getPath());
    }
#else
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
#endif
}
