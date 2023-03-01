package cn.zbx1425.resourcepackupdater.drm;

import org.apache.commons.io.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class AssetEncryption {

    private static final byte[] HEADER_MAGIC = "ZBXNMB10".getBytes(StandardCharsets.UTF_8);

    public static boolean isEncrypted(FileInputStream fis) throws IOException {
        fis.getChannel().position(0);
        boolean result = Arrays.equals(fis.readNBytes(HEADER_MAGIC.length), HEADER_MAGIC);
        if (!result) fis.getChannel().position(0);
        return result;
    }

    public static InputStream wrapInputStream(FileInputStream fis) throws IOException {
        if (isEncrypted(fis)) {
            try (DataInputStream dis = new DataInputStream(fis)) {
                int versionMajor = dis.readInt();
                int versionMinor = dis.readInt();
                byte[] dContent;
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                byte[] key = dis.readNBytes(32);
                SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
                byte[] iv = Arrays.copyOfRange(sha256.digest(key), 0, 16);
                IvParameterSpec aesIv = new IvParameterSpec(iv);

                int len = dis.readInt();
                byte[] eContent = dis.readNBytes(len);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, aesKey, aesIv);
                dContent = cipher.doFinal(eContent);
                return new ByteArrayInputStream(dContent);
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        } else {
            return fis;
        }
    }

    public static void writeEncrypted(byte[] src, File target) throws IOException {
        byte[] eContent, key;
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            key = keyGenerator.generateKey().getEncoded();

            SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
            byte[] iv = Arrays.copyOfRange(sha256.digest(key), 0, 16);
            IvParameterSpec aesIv = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, aesIv);
            eContent = cipher.doFinal(src);
        } catch (Exception ex) {
            throw new IOException(ex);
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target))) {
            DataOutputStream dos = new DataOutputStream(bos);
            dos.write("ZBXNMB10".getBytes(StandardCharsets.UTF_8));
            dos.writeInt(1);
            dos.writeInt(0);
            dos.write(key);
            dos.writeInt(eContent.length);
            dos.write(eContent);
        }
    }

    public static void encryptIfRaw(File target) throws IOException {
        byte[] src;
        try (FileInputStream fis = new FileInputStream(target)) {
            if (isEncrypted(fis)) return;
            src = IOUtils.toByteArray(fis);
        }
        writeEncrypted(src, target);
    }
}
