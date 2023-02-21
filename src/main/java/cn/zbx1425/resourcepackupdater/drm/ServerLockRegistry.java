package cn.zbx1425.resourcepackupdater.drm;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.mappings.Text;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ServerLockRegistry {

    public static boolean lockAllSyncedPacks = true;

    private static String localServerLock;

    private static String remoteServerLock;
    private static String packAppliedServerLock;

    private static long serverLockPacketTime;
    private static boolean serverLockPrefetched = false;

    public static void prefetchServerLock(File rpFolder) {
        try {
            JsonObject metaObj = JsonParser.parseString(IOUtils.toString(
                    AssetEncryption.wrapInputStream(new FileInputStream(rpFolder.toPath().resolve("pack.mcmeta").toFile()))
                    , StandardCharsets.UTF_8)).getAsJsonObject();
            if (metaObj.has("zbx_rpu_server_lock")) {
                localServerLock = metaObj.get("zbx_rpu_server_lock").getAsString();
                if (!serverLockPrefetched) {
                    remoteServerLock = localServerLock;
                    packAppliedServerLock = remoteServerLock;
                    ResourcePackUpdater.LOGGER.info("Server lock info prefetched from local pack.");
                    serverLockPrefetched = true;
                }
            }
        } catch (Exception ignored) {

        }
    }

    public static boolean shouldRefuseProvidingFile(String resourcePath) {
        if (Objects.equals(resourcePath, "pack.mcmeta") || Objects.equals(resourcePath, "pack.png")) return false;
        if (lockAllSyncedPacks) return true;
        return !Objects.equals(localServerLock, remoteServerLock);
    }

    public static void onSetServerLock(String serverLock) {
        ResourcePackUpdater.LOGGER.info("Server lock info obtained from remote.");
        remoteServerLock = serverLock;
        serverLockPacketTime = System.currentTimeMillis();
        if (lockAllSyncedPacks) {
            Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToast.SystemToastIds.PACK_LOAD_FAILURE,
                Text.literal("伺服器資源包不完整而未被采用"), Text.literal("您可按 F3+T 重試下載。如有錯誤請聯絡管理人員。")
            ));
            Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToast.SystemToastIds.PACK_LOAD_FAILURE,
                Text.literal("Resource Pack Incomplete"), Text.literal("Press F3+T to download again. Ask the staff when error.")
            ));
        }
    }

    public static void onAfterSetServerLock() {
        if (Minecraft.getInstance().isSameThread()) return;
        if (System.currentTimeMillis() - serverLockPacketTime > 10 * 1000) {
            // Server lock packet not sent in the past 10s, meaning the remote server doesn't have mod installed.
            ResourcePackUpdater.LOGGER.info("Server lock info not present at remote.");
            remoteServerLock = null;
            serverLockPacketTime = 0;
        } else {
            ResourcePackUpdater.LOGGER.info("Server lock info is valid.");
        }
        if (!Objects.equals(packAppliedServerLock, remoteServerLock)) {
            packAppliedServerLock = remoteServerLock;
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().reloadResourcePacks());
        }
    }
}
