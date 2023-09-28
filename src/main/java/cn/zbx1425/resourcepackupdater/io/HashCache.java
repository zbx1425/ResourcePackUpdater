package cn.zbx1425.resourcepackupdater.io;

import cn.zbx1425.resourcepackupdater.drm.AssetEncryption;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class HashCache {

    public HashMap<String, FileProperty> entries = new HashMap<>();

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
            stream.writeInt(entries.size());
            for (var entry : entries.entrySet()) {
                stream.writeInt(entry.getKey().length());
                stream.writeBytes(entry.getKey());
                stream.writeLong(entry.getValue().mTime);
                stream.writeInt(entry.getValue().hash.length);
                stream.write(entry.getValue().hash);
            }
        }
        isDirty = false;
    }

    public byte[] getDigest(File file) throws IOException {
        String key = basePath.relativize(file.toPath()).toString().replace('\\', '/');
        FileProperty entry = entries.getOrDefault(key, null);
        if (entry != null) {
            if (entry.mTime == file.lastModified()) {
                return entry.hash;
            }
        }
        byte[] hash = calculateDigest(file);
        entries.put(key, new FileProperty(hash, file.lastModified()));
        isDirty = true;
        return hash;
    }

    public byte[] getDigestNoCache(File file) throws IOException {
        String key = basePath.relativize(file.toPath()).toString().replace('\\', '/');
        byte[] hash = calculateDigest(file);
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
