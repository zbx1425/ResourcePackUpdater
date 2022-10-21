package cn.zbx1425.resourcepackupdater;

import cn.zbx1425.resourcepackupdater.gui.GlProgressScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ResourcePackUpdater implements PreLaunchEntrypoint {

    public static final String MOD_ID = "resourcepackupdater";

    public static final Logger LOGGER = LogManager.getLogger("ResourcePackUpdater");

    public static String MOD_VERSION = "";

    public static final GlProgressScreen GL_PROGRESS_SCREEN = new GlProgressScreen();

    public static final Config CONFIG = new Config();

    @Override
    public void onPreLaunch() {
        MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).get()
                .getMetadata().getVersion().getFriendlyString();
        try {
            CONFIG.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
