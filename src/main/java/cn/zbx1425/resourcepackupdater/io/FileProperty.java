package cn.zbx1425.resourcepackupdater.io;

import com.google.gson.JsonObject;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class FileProperty {

    public final byte[] hash;
    public final long mTime;
    public final long size;

    public FileProperty(JsonObject obj) {
        byte[] hash1;
        try {
            hash1 = Hex.decodeHex(obj.get("sha1").getAsString().toCharArray());
        } catch (DecoderException e) {
            hash1 = null;
        }
        hash = hash1;

        mTime = obj.has("mtime") ? obj.get("mtime").getAsLong() : 0;
        size = obj.has("size") ? obj.get("size").getAsLong() : 0;
    }

    public FileProperty(byte[] hash, long mTime) {
        this.hash = hash;
        this.mTime = mTime;
        this.size = 0;
    }
}
