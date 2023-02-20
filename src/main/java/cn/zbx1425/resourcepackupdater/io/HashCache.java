package cn.zbx1425.resourcepackupdater.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class HashCache {

    public HashMap<String, CacheEntry> entries = new HashMap<>();

    private static final MessageDigest sha1Digest;
    private boolean isDirty = false;

    static {
        MessageDigest sha1Digest1;
        try {
            sha1Digest1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ignored) {
            sha1Digest1 = null;
        }
        sha1Digest = sha1Digest1;
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
                entries.put(key, new CacheEntry(hash, mTime));
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

    public byte[] getDigest(String key, File file) throws IOException {
        CacheEntry entry = entries.getOrDefault(key, null);
        if (entry != null) {
            if (entry.mTime == file.lastModified()) {
                return entry.hash;
            }
        }
        byte[] hash = getDigest(file);
        entries.put(key, new CacheEntry(hash, file.lastModified()));
        isDirty = true;
        return hash;
    }

    public static byte[] getDigest(File file) throws IOException {
        InputStream fis = new BufferedInputStream(AssetEncryption.wrapInputStream(new FileInputStream(file)));
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fis.read(buffer);
            if (n > 0) {
                sha1Digest.update(buffer, 0, n);
            }
        }
        fis.close();
        return sha1Digest.digest();
    }

    public static class CacheEntry {

        public byte[] hash;
        public long mTime;

        public CacheEntry(byte[] hash, long mTime) {
            this.hash = hash;
            this.mTime = mTime;
        }
    }
}
