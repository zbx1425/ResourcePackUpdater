package cn.zbx1425.resourcepackupdater.drm;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServerLockRegistry {

    private static final HashMap<File, FileLockInfo> fileServerLocks = new HashMap<>();

    private static String remoteServerLock;
    private static String packAppliedServerLock;

    private static long serverLockPacketTime;

    public static final ResourceLocation SERVER_LOCK_PACKET_ID = new ResourceLocation("zbx_rpu", "server_lock");

    public static FileLockInfo getServerLock(File file, File rpFolder) {
        if (!FilenameUtils.getExtension(file.getName()).equals("json")) return null;
        return fileServerLocks.computeIfAbsent(file, target -> {
            try {
                JsonObject metaObj = JsonParser.parseString(IOUtils.toString(
                        AssetEncryption.wrapInputStream(new FileInputStream(rpFolder.toPath().resolve("pack.mcmeta").toFile()))
                , StandardCharsets.UTF_8)).getAsJsonObject();
                if (metaObj.has("zbx_rpu_server_lock")) {
                    return new FileLockInfo(metaObj.get("zbx_rpu_server_lock").getAsString(), file);
                } else {
                    return null;
                }
            } catch (Exception ignored) {
                return null;
            }
        });
    }

    public static void onSetServerLock(String serverLock) {
        ResourcePackUpdater.LOGGER.info("Server lock info obtained.");
        remoteServerLock = serverLock;
        serverLockPacketTime = System.currentTimeMillis();
    }

    public static void onAfterSetServerLock() {
        if (System.currentTimeMillis() - serverLockPacketTime > 10 * 1000) {
            // Server lock packet not sent in the past 10s, meaning the remote server doesn't have mod installed.
            ResourcePackUpdater.LOGGER.info("Server does not have lock info.");
            remoteServerLock = null;
            serverLockPacketTime = 0;
        }
        if (!Objects.equals(packAppliedServerLock, remoteServerLock)) {
            packAppliedServerLock = remoteServerLock;
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().reloadResourcePacks());
        }
    }

    public static class FileLockInfo {
        public String intendedServer;
        public String dummyContent;

        public FileLockInfo(String intendedServer, File prevFile) {
            this.intendedServer = intendedServer;

            if (prevFile.getName().equals("sounds.json")) {
                this.dummyContent = "{}";
            } else {
                try {
                    JsonObject prevContent = JsonParser.parseString(
                            IOUtils.toString(AssetEncryption.wrapInputStream(new FileInputStream(prevFile)), StandardCharsets.UTF_8))
                            .getAsJsonObject();
                    JsonObject newContent = new JsonObject();
                    for (Map.Entry<String, JsonElement> entry : prevContent.entrySet()) {
                        if (entry.getValue().isJsonArray()) {
                            newContent.add(entry.getKey(), new JsonArray());
                        } else if (entry.getValue().isJsonObject()) {
                            newContent.add(entry.getKey(), new JsonObject());
                        } else if (entry.getKey().equals("zbx_rpu_server_lock")) {
                            newContent.addProperty("zbx_rpu_server_lock", "LOCKED");
                        } else {
                            newContent.add(entry.getKey(), entry.getValue());
                        }
                    }
                    this.dummyContent = newContent.toString();
                } catch (Exception ignored) {
                    this.dummyContent = "{}";
                }
            }
        }

        public boolean isCurrentlyLocked() {
            return !intendedServer.equals(remoteServerLock);
        }

        public InputStream getDummyInputStream() {
            return new ByteArrayInputStream(dummyContent.getBytes(StandardCharsets.UTF_8));
        }
    }
}
