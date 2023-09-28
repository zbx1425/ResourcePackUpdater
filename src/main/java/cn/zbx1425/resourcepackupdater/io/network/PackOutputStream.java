package cn.zbx1425.resourcepackupdater.io.network;

import cn.zbx1425.resourcepackupdater.drm.AssetEncryption;
import cn.zbx1425.resourcepackupdater.io.HashCache;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class PackOutputStream extends OutputStream {

    private final boolean encrypt;
    private final Path target;
    private final HashCache hashCache;
    private final byte[] expectedSha;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private boolean closed = false;

    public PackOutputStream(Path target, boolean encrypt, HashCache hashCache, byte[] expectedSha) {
        this.encrypt = encrypt;
        this.target = target;
        this.expectedSha = expectedSha;
        this.hashCache = hashCache;
    }

    @Override
    public void write(int b) throws IOException {
        buffer.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        buffer.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        if (encrypt) {
            AssetEncryption.writeEncrypted(buffer.toByteArray(), target.toFile());
        } else {
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target.toFile()))) {
                bos.write(buffer.toByteArray());
            }
        }
        byte[] localSha = hashCache.getDigestNoCache(target.toFile());
        if (!Arrays.equals(localSha, expectedSha)) {
            throw new IOException("SHA1 mismatch: " + Hex.encodeHexString(localSha) + " downloaded, " +
                    Hex.encodeHexString(expectedSha) + " expected");
        }
        super.close();
    }
}
