package cn.zbx1425.resourcepackupdater;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class Config {

    public final ConfigItem<String> remoteConfigUrl = new ConfigItem<>(
            "remoteConfigUrl", JsonElement::getAsString, JsonPrimitive::new, "https://mc.zbx1425.cn/jlp-srp/client_config.json");

    public final ConfigItem<List<SourceProperty>> sourceList = new ConfigItem<>(
        "sources",
        (json) -> {
            List<SourceProperty> list = new ArrayList<>();
            for (JsonElement source : json.getAsJsonArray()) {
                list.add(new SourceProperty((JsonObject)source));
            }
            return list;
        },
        (value) -> {
            JsonArray array = new JsonArray();
            for (SourceProperty source : value) {
                if (source.isBuiltin) continue;
                array.add(source.toJson());
            }
            return array;
        },
        new ArrayList<>()
    );
    public final ConfigItem<SourceProperty> selectedSource = new ConfigItem<SourceProperty>(
        "selectedSource", (json) -> new SourceProperty((JsonObject)json), SourceProperty::toJson, () -> null);
    public final ConfigItem<String> localPackName = new ConfigItem<>(
        "localPackName", JsonElement::getAsString, JsonPrimitive::new, "SyncedPack");
    public final ConfigItem<Boolean> disableBuiltinSources = new ConfigItem<>(
        "disableBuiltinSources", JsonElement::getAsBoolean, JsonPrimitive::new, false);
    public final ConfigItem<Boolean> pauseWhenSuccess = new ConfigItem<>(
        "pauseWhenSuccess", JsonElement::getAsBoolean, JsonPrimitive::new, false);
    public final ConfigItem<File> packBaseDirFile = new ConfigItem<File>(
        "packBaseDirFile", (json) -> new File(json.getAsString()),
            (value) -> new JsonPrimitive(value.toString()), () -> new File(getPackBaseDir()));

    public final ConfigItem<String> serverLockKey = new ConfigItem<>(
        "serverLockKey", JsonElement::getAsString, JsonPrimitive::new, "");
    public final ConfigItem<Boolean> clientEnforceInstall = new ConfigItem<>(
        "clientEnforceInstall", JsonElement::getAsBoolean, JsonPrimitive::new, false);
    public final ConfigItem<String> clientEnforceVersion = new ConfigItem<>(
        "clientEnforceVersion", JsonElement::getAsString, JsonPrimitive::new, "");

    public List<ConfigItem<?>> configItems = List.of(
        remoteConfigUrl, sourceList, selectedSource, localPackName, disableBuiltinSources,
        pauseWhenSuccess, packBaseDirFile, serverLockKey, clientEnforceInstall, clientEnforceVersion
    );

    public void load() throws IOException {
        if (!Files.isRegularFile(getConfigFilePath())) {
            save();
        }

        JsonObject localConfig = (JsonObject)ResourcePackUpdater.JSON_PARSER.parse(Files.readString(getConfigFilePath()));
        remoteConfigUrl.load(localConfig, new JsonObject());
        JsonObject remoteConfig;
        if (remoteConfigUrl.value.isEmpty()) {
            remoteConfig = new JsonObject();
        } else {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder(new URI(remoteConfigUrl.value))
                        .timeout(Duration.ofSeconds(10))
                        .setHeader("User-Agent", "ResourcePackUpdater/" + ResourcePackUpdater.MOD_VERSION + " +https://www.zbx1425.cn")
                        .setHeader("Accept-Encoding", "gzip")
                        .GET()
                        .build();
                HttpResponse<String> httpResponse;
                try {
                    httpResponse = ResourcePackUpdater.HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                } catch (InterruptedException ex) {
                    throw new IOException(ex);
                }
                if (httpResponse.statusCode() != 200) {
                    throw new IOException("HTTP " + httpResponse.statusCode() + " " + httpResponse.body());
                }
                remoteConfig = (JsonObject) ResourcePackUpdater.JSON_PARSER.parse(httpResponse.body());
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }

        for (ConfigItem<?> item : configItems) {
            item.load(localConfig, remoteConfig);
        }

        if (remoteConfigUrl.isFromLocal && remoteConfig.has("remoteConfigUrl")
            && !remoteConfig.get("remoteConfigUrl").getAsString().equals(remoteConfigUrl.value)) {
            remoteConfigUrl.load(remoteConfig, remoteConfig);
            save();
        }

        if (!disableBuiltinSources.value) addBuiltinSources();
        if (!sourceList.value.contains(selectedSource.value)) selectedSource.value = null;
        if (selectedSource.value == null) {
            selectedSource.value = new SourceProperty(
                    "NOT CONFIGURED",
                    "",
                    false, false, true
            );
        }
    }

    public void save() throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("version", 2);

        for (ConfigItem<?> item : configItems) {
            item.save(obj);
        }
        Files.writeString(getConfigFilePath(), new GsonBuilder().setPrettyPrinting().create().toJson(obj));
    }

    private void addBuiltinSources() {
        /*
        sourceList.value.add(0, new SourceProperty(
            "MTR Let's Play (HK, Primary)",
            "https://mc.zbx1425.cn/jlp-srp", true, true, true
        ));
        sourceList.value.add(0, new SourceProperty(
            "MTR Let's Play (CN, Mirror)",
            "https://seu.complexstudio.net/jlp-srp", true, false, true
        ));
        */
    }

    public String getPackBaseDir() {
        String sx = FabricLoader.getInstance().getGameDir().toString();
        return Paths.get(sx, "resourcepacks", localPackName.value).toAbsolutePath().normalize().toString();
    }

    public Path getConfigFilePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(ResourcePackUpdater.MOD_ID + ".json");
    }


    public static class ConfigItem<T> {

        public T value;
        public boolean isFromLocal;

        private final String key;
        private final Function<JsonElement, T> fromCodec;
        private final Function<T, JsonElement> toCodec;
        private final Supplier<T> defaultSupplier;

        public ConfigItem(String key, Function<JsonElement, T> fromCodec, Function<T, JsonElement> toCodec, T defaultValue) {
            this.key = key;
            this.fromCodec = fromCodec;
            this.toCodec = toCodec;
            this.defaultSupplier = () -> defaultValue;
        }

        public ConfigItem(String key, Function<JsonElement, T> fromCodec, Function<T, JsonElement> toCodec, Supplier<T> defaultSupplier) {
            this.key = key;
            this.fromCodec = fromCodec;
            this.toCodec = toCodec;
            this.defaultSupplier = defaultSupplier;
        }

        public void load(JsonObject localObject, JsonObject remoteObject) {
            if (localObject.has("!" + key)) {
                value = fromCodec.apply(localObject.get("!" + key));
                isFromLocal = true;
            } else if (remoteObject.has(key)) {
                value = fromCodec.apply(remoteObject.get(key));
                isFromLocal = false;
            } else if (localObject.has(key)) {
                value = fromCodec.apply(localObject.get(key));
                isFromLocal = true;
            } else {
                value = defaultSupplier.get();
                isFromLocal = false;
            }
        }

        public void save(JsonObject jsonObject) {
            if (isFromLocal && value != null) {
                jsonObject.add(key, toCodec.apply(value));
            }
        }
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
