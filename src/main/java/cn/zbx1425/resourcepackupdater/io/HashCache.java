package cn.zbx1425.resourcepackupdater.io;

import cn.zbx1425.resourcepackupdater.drm.AssetEncryption;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class HashCache {

    public HashMap<String, FileProperty> entries = new HashMap<>();

    public HashMap<String, FileProperty> entriesToSave = new HashMap<>();

    private final Path basePath;
    private boolean isDirty = false;

    public HashCache(Path basePath) {
        this.basePath = basePath;
    }

    public void load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return;
        entries.clear();
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            final int version = stream.readInt();
            final int entryCount = stream.readInt();
            for (int i = 0; i < entryCount; ++i) {
                final int keyLength = stream.readInt();
                final String key = new String(stream.readNBytes(keyLength));
                final long mTime = stream.readLong();
                final int hashLength = stream.readInt();
                final byte[] hash = stream.readNBytes(hashLength);
                entries.put(key, new FileProperty(hash, mTime));
            }
        }
        isDirty = false;
    }

    public void save(Path file) throws IOException {
        if (!isDirty) return;
        try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            stream.writeInt(1);
            stream.writeInt(entriesToSave.size());
            for (var entry : entriesToSave.entrySet()) {
                stream.writeInt(entry.getKey().length());
                stream.writeBytes(entry.getKey());
                stream.writeLong(entry.getValue().mTime);
                stream.writeInt(entry.getValue().hash.length);
                stream.write(entry.getValue().hash);
            }
        }
        isDirty = false;
    }

    public byte[] getDigest(File file) {
        String key = basePath.relativize(file.toPath()).toString().replace('\\', '/');
        FileProperty entry = entries.getOrDefault(key, null);
        if (entry != null) {
            if (entry.mTime == file.lastModified()) {
                entriesToSave.put(key, entry);
                return entry.hash;
            }
        }
        byte[] hash;
        try {
            hash = calculateDigest(file);
        } catch (IOException ex) {
            hash = new byte[20];
        }
        entry = new FileProperty(hash, file.lastModified());
        entries.put(key, entry);
        entriesToSave.put(key, entry);
        isDirty = true;
        return hash;
    }

    public byte[] getDigestNoCache(File file) {
        String key = basePath.relativize(file.toPath()).toString().replace('\\', '/');
        byte[] hash;
        try {
            hash = calculateDigest(file);
        } catch (IOException ex) {
            hash = new byte[20];
        }
        entries.put(key, new FileProperty(hash, file.lastModified()));
        isDirty = true;
        return hash;
    }

    public static byte[] calculateDigest(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.sha1(AssetEncryption.wrapInputStream(fis));
        }
    }
}
