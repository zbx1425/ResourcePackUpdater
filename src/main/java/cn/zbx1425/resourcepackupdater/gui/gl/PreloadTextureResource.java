package cn.zbx1425.resourcepackupdater.gui.gl;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

public class PreloadTextureResource extends Resource {

    public PreloadTextureResource(ResourceLocation resourceLocation) {
        super(new DummyPackResources(), () ->
            Objects.requireNonNull(PreloadTextureResource.class.getResourceAsStream("/assets/" + resourceLocation.getNamespace() + "/" + resourceLocation.getPath()))
        );
    }

    public static class DummyPackResources implements PackResources {

        @Nullable
        @Override
        public IoSupplier<InputStream> getRootResource(String... elements) {
            return null;
        }

        @Nullable
        @Override
        public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
            return null;
        }

        @Override
        public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {

        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            return Set.of();
        }

        @Nullable
        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
            return null;
        }

        @Override
        public String packId() {
            return "";
        }

        @Override
        public void close() {

        }
    }
}
