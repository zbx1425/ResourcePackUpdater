package cn.zbx1425.resourcepackupdater;

import cn.zbx1425.resourcepackupdater.drm.ServerLockRegistry;
import cn.zbx1425.resourcepackupdater.gui.GlHelper;
import cn.zbx1425.resourcepackupdater.gui.GlProgressScreen;
import cn.zbx1425.resourcepackupdater.io.Dispatcher;
import cn.zbx1425.resourcepackupdater.io.network.DummyTrustManager;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.ExecutorServices;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResourcePackUpdater implements ModInitializer {

    public static final String MOD_ID = "resourcepackupdater";

    public static final Logger LOGGER = LogManager.getLogger("ResourcePackUpdater");

    public static String MOD_VERSION = "";

    public static final GlProgressScreen GL_PROGRESS_SCREEN = new GlProgressScreen();

    public static final Config CONFIG = new Config();

    public static final ResourceLocation SERVER_LOCK_PACKET_ID = new ResourceLocation("zbx_rpu", "server_lock");
    public static final ResourceLocation CLIENT_VERSION_PACKET_ID = new ResourceLocation("zbx_rpu", "client_version");

    public static final JsonParser JSON_PARSER = new JsonParser();
    public static final HttpClient HTTP_CLIENT;

    static {
        // PREVENTS HOST VALIDATION
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        ExecutorService HTTP_CLIENT_EXECUTOR = Executors.newFixedThreadPool(4);
        HTTP_CLIENT = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .executor(HTTP_CLIENT_EXECUTOR)
                .sslContext(DummyTrustManager.UNSAFE_CONTEXT)
                .build();
    }

    @Override
    public void onInitialize() {
        MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).get()
                .getMetadata().getVersion().getFriendlyString();
        try {
            CONFIG.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void dispatchSyncWork() {
        GlHelper.initGlStates();

        Dispatcher syncDispatcher = new Dispatcher();

        ResourcePackUpdater.GL_PROGRESS_SCREEN.resetToSelectSource();
        try {
            while (ResourcePackUpdater.GL_PROGRESS_SCREEN.shouldContinuePausing(true)) {
                Thread.sleep(50);
            }
        } catch (Exception ignored) { }

        ResourcePackUpdater.GL_PROGRESS_SCREEN.reset();
        try {
            boolean syncSuccess = syncDispatcher.runSync(ResourcePackUpdater.CONFIG.getPackBaseDir(),
                    ResourcePackUpdater.CONFIG.activeSource.value, ResourcePackUpdater.GL_PROGRESS_SCREEN);
            if (syncSuccess) {
                ServerLockRegistry.lockAllSyncedPacks = false;
            } else {
                ServerLockRegistry.lockAllSyncedPacks = true;
            }

            Minecraft.getInstance().options.save();
        } catch (Exception ignored) {
            ServerLockRegistry.lockAllSyncedPacks = true;
        }

        try {
            while (ResourcePackUpdater.GL_PROGRESS_SCREEN.shouldContinuePausing(true)) {
                Thread.sleep(50);
            }
        } catch (Exception ignored) { }

        ServerLockRegistry.updateLocalServerLock(ResourcePackUpdater.CONFIG.packBaseDirFile.value);
        GlHelper.resetGlStates();
    }

    public static void modifyPackList() {
        Options options = Minecraft.getInstance().options;
        String expectedEntry = "file/" + ResourcePackUpdater.CONFIG.localPackName;
        options.resourcePacks.remove(expectedEntry);
        if (!options.resourcePacks.contains("vanilla")) {
            options.resourcePacks.add("vanilla");
        }
        if (!options.resourcePacks.contains("Fabric Mods")) {
            options.resourcePacks.add("Fabric Mods");
        }
        /*
        if (!ServerLockRegistry.shouldRefuseProvidingFile(null)) {
            options.resourcePacks.add(expectedEntry);
        }
        */
        options.resourcePacks.add(expectedEntry);
        options.incompatibleResourcePacks.remove(expectedEntry);

        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        repository.reload();
        options.loadSelectedResourcePacks(repository);
    }
}
