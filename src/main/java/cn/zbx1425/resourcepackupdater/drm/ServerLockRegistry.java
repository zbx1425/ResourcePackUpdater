package cn.zbx1425.resourcepackupdater.drm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.compress.utils.FileNameUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServerLockRegistry {

    private static final HashMap<File, FileLockInfo> fileServerLocks = new HashMap<>();

    public static String currentServerLock;
    public static String effectiveServerLock;

    public static final ResourceLocation SERVER_LOCK_PACKET_ID = new ResourceLocation("zbx_rpu", "server_lock");

    public static FileLockInfo getServerLock(File file) {
        if (!FileNameUtils.getExtension(file.getName()).equals("json")) return null;
       return fileServerLocks.computeIfAbsent(file, target -> {
            try {
                JsonObject jsonObj = JsonParser.parseString(Files.readString(target.toPath())).getAsJsonObject();
                if (jsonObj.has("zbx_rpu_server_lock")) {
                    return new FileLockInfo(jsonObj.get("zbx_rpu_server_lock").getAsString(), jsonObj);
                } else {
                    return null;
                }
            } catch (Exception ignored) {
                return null;
            }
        });
    }

    public static void notifyPackUsers() {
        if (!Objects.equals(effectiveServerLock, currentServerLock)) {

            effectiveServerLock = currentServerLock;
        }
    }

    public static class FileLockInfo {
        public String intendedServer;
        public String dummyContent;

        public FileLockInfo(String intendedServer, JsonObject prevContent) {
            this.intendedServer = intendedServer;
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
        }

        public boolean isCurrentlyLocked() {
            return intendedServer.equals(currentServerLock);
        }

        public InputStream getDummyInputStream() {
            return new ByteArrayInputStream(dummyContent.getBytes(StandardCharsets.UTF_8));
        }
    }
}
