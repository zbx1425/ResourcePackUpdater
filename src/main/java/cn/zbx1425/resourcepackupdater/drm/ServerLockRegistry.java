package cn.zbx1425.resourcepackupdater.drm;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

public class ServerLockRegistry {

    private static final HashMap<File, String> packLocks = new HashMap<>();

    private static String remoteServerLock;
    private static String packAppliedServerLock;

    private static long serverLockPacketTime;
    private static boolean serverLockPrefetched = false;

    public static final ResourceLocation SERVER_LOCK_PACKET_ID = new ResourceLocation("zbx_rpu", "server_lock");

    public static void prefetchServerLock(File rpFolder) {
        try {
            JsonObject metaObj = JsonParser.parseString(IOUtils.toString(
                    AssetEncryption.wrapInputStream(new FileInputStream(rpFolder.toPath().resolve("pack.mcmeta").toFile()))
                    , StandardCharsets.UTF_8)).getAsJsonObject();
            if (metaObj.has("zbx_rpu_server_lock")) {
                packLocks.put(rpFolder, metaObj.get("zbx_rpu_server_lock").getAsString());
                if (!serverLockPrefetched) {
                    remoteServerLock = metaObj.get("zbx_rpu_server_lock").getAsString();
                    packAppliedServerLock = remoteServerLock;
                    ResourcePackUpdater.LOGGER.info("Server lock info prefetched from local pack.");
                    serverLockPrefetched = true;
                }
            }
        } catch (Exception ignored) {

        }
    }

    public static String getServerLock(File rpFolder) {
        return packLocks.getOrDefault(rpFolder, null);
    }

    public static boolean isPackLocked(File rpFolder, String resourcePath) {
        return getServerLock(rpFolder) != null
                && !getServerLock(rpFolder).equals(remoteServerLock)
                && !Objects.equals(resourcePath, "pack.mcmeta")
                && !Objects.equals(resourcePath, "pack.png");
    }

    public static void onSetServerLock(String serverLock) {
        ResourcePackUpdater.LOGGER.info("Server lock info obtained from remote.");
        remoteServerLock = serverLock;
        serverLockPacketTime = System.currentTimeMillis();
    }

    public static void onAfterSetServerLock() {
        if (System.currentTimeMillis() - serverLockPacketTime > 10 * 1000) {
            // Server lock packet not sent in the past 10s, meaning the remote server doesn't have mod installed.
            ResourcePackUpdater.LOGGER.info("Server lock info not present at remote.");
            remoteServerLock = null;
            serverLockPacketTime = 0;
        }
        if (!Objects.equals(packAppliedServerLock, remoteServerLock)) {
            packAppliedServerLock = remoteServerLock;
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().reloadResourcePacks());
        }
    }
}
