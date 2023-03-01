package cn.zbx1425.resourcepackupdater.io;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.drm.AssetEncryption;
import cn.zbx1425.resourcepackupdater.util.MismatchingVersionException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

public class RemoteMetadata {

    public String baseUrl;
    public boolean encrypt = false;
    public List<String> dirs = new ArrayList<>();
    public HashMap<String, byte[]> files = new HashMap<>();

    public RemoteMetadata(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public byte[] fetchDirChecksum(ProgressReceiver cb) throws Exception {
        String metaString = httpGetString(baseUrl + "/metadata.sha1", cb);
        if (metaString.startsWith("{")) {
            JsonObject metadataObj = ResourcePackUpdater.JSON_PARSER.parse(metaString).getAsJsonObject();
            assertMetadataVersion(metadataObj);
            if (metadataObj.has("encrypt")) encrypt = metadataObj.get("encrypt").getAsBoolean();
            return Hex.decodeHex(metadataObj.get("sha1").getAsString().toCharArray());
        } else {
            return Hex.decodeHex(metaString.trim().toCharArray());
        }
    }

    public void fetch(ProgressReceiver cb) throws Exception {
        dirs.clear();
        files.clear();
        var metadataObj = ResourcePackUpdater.JSON_PARSER.parse(
                httpGetString(baseUrl + "/metadata.json", cb)
        ).getAsJsonObject();
        assertMetadataVersion(metadataObj);
        int metadataVersion = 1;
        if (metadataObj.has("version")) metadataVersion = metadataObj.get("version").getAsInt();
        if (metadataObj.has("encrypt")) encrypt = metadataObj.get("encrypt").getAsBoolean();

        if (metadataVersion == 1) {
            for (var entry : metadataObj.get("dirs").getAsJsonObject().entrySet()) {
                dirs.add(entry.getKey());
            }
            for (var entry : metadataObj.get("files").getAsJsonObject().entrySet()) {
                files.put(entry.getKey(), Hex.decodeHex(entry.getValue().getAsJsonObject().get("sha1").getAsString().toCharArray()));
            }
        } else if (metadataVersion == 2) {
            JsonObject contentObj = metadataObj.get("file_content").getAsJsonObject();
            for (var entry : contentObj.get("dirs").getAsJsonObject().entrySet()) {
                dirs.add(entry.getKey());
            }
            for (var entry : contentObj.get("files").getAsJsonObject().entrySet()) {
                files.put(entry.getKey(), Hex.decodeHex(entry.getValue().getAsJsonObject().get("sha1").getAsString().toCharArray()));
            }
        } else {
            throw new MismatchingVersionException("Unsupported metadata protocol version: " + metadataVersion);
        }
    }

    private static String httpGetString(String urlStr, ProgressReceiver cb) throws IOException {
        URL url = new URL(urlStr);
        int retryCount = 0;
        final int MAX_RETRIES = 3;
        while (true) {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                try {
                    urlToStream(url, bos, cb);
                    return bos.toString(StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    if (retryCount < MAX_RETRIES) {
                        cb.printLog(ex.toString());
                        retryCount++;
                        cb.printLog(String.format("Retrying (%d/%d) ...", retryCount, MAX_RETRIES));
                    } else {
                        throw ex;
                    }
                }
            }
        }
    }

    public void httpGetFile(Path localPath, String file, ProgressReceiver cb) throws Exception {
        Files.deleteIfExists(localPath);

        URL url = new URL(baseUrl + "/dist/" + file);
        int retryCount = 0;
        final int MAX_RETRIES = 3;
        while (true) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                urlToStream(url, baos, cb);
                if (encrypt) {
                    AssetEncryption.writeEncrypted(baos.toByteArray(), localPath.toFile());
                } else {
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(localPath.toFile()))) {
                        bos.write(baos.toByteArray());
                    }
                }
                byte[] expectedSha = files.get(file);
                byte[] localSha = HashCache.getDigest(localPath.toFile());
                if (!Arrays.equals(localSha, expectedSha)) {
                    throw new IOException("SHA1 mismatch: " + Hex.encodeHexString(localSha) + " downloaded, " +
                            Hex.encodeHexString(expectedSha) + " expected");
                }
                return;
            } catch (Exception ex) {
                if (retryCount < MAX_RETRIES) {
                    cb.printLog(ex.toString());
                    retryCount++;
                    cb.printLog(String.format("Retrying (%d/%d) ...", retryCount, MAX_RETRIES));
                } else {
                    throw ex;
                }
            }
        }
    }

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static void urlToStream(URL url, OutputStream target, ProgressReceiver cb) throws IOException {
        URI requestUri;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest((url.getPath() + "?REALLY-BAD-VALIDATION-IDEA")
                    .getBytes(StandardCharsets.UTF_8));
            String digestStr = StringUtils.stripEnd(Base64.getEncoder().encodeToString(digest).replace('+', '-').replace('/', '_'), "=");
            requestUri = new URIBuilder(url.toURI()).addParameter("md5", digestStr).build();
        } catch (Exception ex) {
            throw new IOException(ex);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(requestUri)
                .timeout(Duration.ofSeconds(10))
                .setHeader("User-Agent", "ResourcePackUpdater/" + ResourcePackUpdater.MOD_VERSION + " +https://www.zbx1425.cn")
                .GET()
                .build();
        HttpResponse<InputStream> httpResponse;
        try {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }

        long fileSize = Long.parseLong(httpResponse.headers().firstValue("Content-Length").orElse("0"));
        final long completeFileSize = fileSize > 0 ? fileSize : Integer.MAX_VALUE;

        try (BufferedOutputStream bos = new BufferedOutputStream(target); InputStream inputStream = httpResponse.body()) {
            final ProgressOutputStream pOfs = new ProgressOutputStream(bos, new ProgressOutputStream.WriteListener() {
                long lastAmount = -1;
                final long noticeDivisor = 8192;

                @Override
                public void registerWrite(long amountOfBytesWritten) throws IOException {
                    if (lastAmount / noticeDivisor != amountOfBytesWritten / noticeDivisor) {
                        String message;
                        message = String.format(": %6d KiB / %6d KiB", amountOfBytesWritten / 1024, completeFileSize / 1024);
                        cb.setSecondaryProgress(amountOfBytesWritten * 1f / completeFileSize, message);
                        lastAmount = amountOfBytesWritten;
                    }
                }
            });
            IOUtils.copy(new BufferedInputStream(inputStream), pOfs);
        }
    }

    public void assertMetadataVersion(JsonObject metadataObj) throws MismatchingVersionException {
        if (metadataObj.has("client_version")) {
            String requestedVer = metadataObj.get("client_version").getAsString();
            if (!requestedVer.equals(ResourcePackUpdater.MOD_VERSION)) {
                throw new MismatchingVersionException(requestedVer, ResourcePackUpdater.MOD_VERSION);
            }
        }
        int metadataVersion = 1;
        if (metadataObj.has("version")) metadataVersion = metadataObj.get("version").getAsInt();
        if (metadataVersion > 2) throw new MismatchingVersionException("Unsupported metadata protocol version: " + metadataVersion);
    }
}
