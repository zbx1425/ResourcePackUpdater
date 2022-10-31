package cn.zbx1425.resourcepackupdater;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Config {

    public final List<SourceProperty> sourceList = new ArrayList<>();
    public SourceProperty activeSource;
    public String localPackName;
    public boolean disableBuiltinSources;
    public boolean pauseWhenSuccess;

    public Config() {
        setDefaults();
    }

    public void load() throws IOException {
        setDefaults();
        if (!Files.isRegularFile(getConfigFilePath())) {
            save();
        }
        JsonObject obj = (JsonObject)JsonParser.parseString(Files.readString(getConfigFilePath()));
        localPackName = obj.get("localPackName").getAsString();
        activeSource = new SourceProperty(obj.get("activeSource").getAsJsonObject());
        if (obj.get("disableBuiltinSources").getAsBoolean()) {
            sourceList.clear();
        }
        for (JsonElement source : obj.get("sources").getAsJsonArray()) {
            sourceList.add(new SourceProperty((JsonObject)source));
        }
        pauseWhenSuccess = obj.get("pauseWhenSuccess").getAsBoolean();
    }

    public void save() throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("version", 2);
        obj.addProperty("localPackName", localPackName);
        obj.add("activeSource", activeSource.toJson());
        obj.addProperty("disableBuiltinSources", disableBuiltinSources);
        JsonArray customSources = new JsonArray();
        for (SourceProperty source : sourceList) {
            if (source.isBuiltin) continue;
            customSources.add(source.toJson());
        }
        obj.add("sources", customSources);
        obj.addProperty("pauseWhenSuccess", pauseWhenSuccess);
        Files.writeString(getConfigFilePath(), new GsonBuilder().setPrettyPrinting().create().toJson(obj));
    }

    public void setDefaults() {
        addBuiltinSources();
        activeSource = sourceList.get(0);
        localPackName = "SyncedPack";
        disableBuiltinSources = false;
        pauseWhenSuccess = false;
    }

    private void addBuiltinSources() {
        sourceList.clear();
        sourceList.add(new SourceProperty(
            "MTR Let's Play (HK, Primary)",
           "https://mc.zbx1425.cn/jlp-srp", true, true, true
        ));
        sourceList.add(new SourceProperty(
            "MTR Let's Play (CN, Mirror)",
        "https://seu.complexstudio.net/jlp-srp", true, false, true
        ));
    }

    public String getPackBaseDir() {
        String sx = FabricLoader.getInstance().getGameDir().toString();
        String baseDir = Paths.get(sx, "resourcepacks", ResourcePackUpdater.CONFIG.localPackName).toAbsolutePath().toString();
        return baseDir;
    }

    public Path getConfigFilePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(ResourcePackUpdater.MOD_ID + ".json");
    }

    public static class SourceProperty {

        public String name;
        public String baseUrl;
        public boolean hasDirHash;
        public boolean hasArchive;
        public boolean isBuiltin;

        public SourceProperty(String name, String baseUrl, boolean hasDirHash, boolean hasArchive, boolean isBuiltin) {
            this.name = name;
            this.baseUrl = baseUrl;
            this.hasDirHash = hasDirHash;
            this.hasArchive = hasArchive;
            this.isBuiltin = isBuiltin;
        }

        public SourceProperty(JsonObject obj) {
            this.name = obj.get("name").getAsString();
            this.baseUrl = obj.get("baseUrl").getAsString();
            this.hasDirHash = obj.get("hasDirHash").getAsBoolean();
            this.hasArchive = obj.get("hasArchive").getAsBoolean();
            this.isBuiltin = false;
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", name);
            obj.addProperty("baseUrl", baseUrl);
            obj.addProperty("hasDirHash", hasDirHash);
            obj.addProperty("hasArchive", hasArchive);
            return obj;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            SourceProperty that = (SourceProperty) o;

            return new EqualsBuilder().append(hasDirHash, that.hasDirHash).append(hasArchive, that.hasArchive).append(baseUrl, that.baseUrl).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(baseUrl).append(hasDirHash).append(hasArchive).toHashCode();
        }
    }
}
