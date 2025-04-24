package cn.zbx1425.resourcepackupdater;

import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ResourcePackUpdater implements ModInitializer {

    public static final String MOD_ID = "resourcepackupdater";
    public static final Logger LOGGER = LogManager.getLogger("ResourcePackUpdater");
    public static String MOD_VERSION = "";

    public static final Config CONFIG = new Config();
    public static final JsonParser JSON_PARSER = new JsonParser();

    @Override
    public void onInitialize() {
        MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).get()
                .getMetadata().getVersion().getFriendlyString();
        try {
            CONFIG.load();
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
        }
    }
}
