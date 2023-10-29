package cn.zbx1425.resourcepackupdater.io.network;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.drm.AssetEncryption;
import cn.zbx1425.resourcepackupdater.io.FileProperty;
import cn.zbx1425.resourcepackupdater.io.HashCache;
import cn.zbx1425.resourcepackupdater.io.ProgressReceiver;
import cn.zbx1425.resourcepackupdater.util.MismatchingVersionException;
import cn.zbx1425.resourcepackupdater.util.MtrVersion;
import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public class RemoteMetadata {

    public String baseUrl;
    public boolean encrypt = false;
    public List<String> dirs = new ArrayList<>();
    public HashMap<String, FileProperty> files = new HashMap<>();

    public long downloadStartTime;
    public long downloadedBytes;

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
                files.put(entry.getKey(), new FileProperty(entry.getValue().getAsJsonObject()));
            }
        } else if (metadataVersion == 2) {
            JsonObject contentObj = metadataObj.get("file_content").getAsJsonObject();
            for (var entry : contentObj.get("dirs").getAsJsonObject().entrySet()) {
                dirs.add(entry.getKey());
            }
            for (var entry : contentObj.get("files").getAsJsonObject().entrySet()) {
                files.put(entry.getKey(), new FileProperty(entry.getValue().getAsJsonObject()));
            }
        } else {
            throw new MismatchingVersionException("Unsupported metadata protocol version: " + metadataVersion);
        }
    }

    private String httpGetString(String urlStr, ProgressReceiver cb) throws IOException {
        URL url = new URL(urlStr);
        int retryCount = 0;
        final int MAX_RETRIES = 3;
        while (true) {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                try {
                    urlToStream(url, 0, bos, cb);
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

    public void beginDownloads(ProgressReceiver cb) throws IOException {
        downloadStartTime = System.currentTimeMillis();
        downloadedBytes = 0;
    }

    public void endDownloads(ProgressReceiver cb) throws IOException {
        long elapsedTimeSecs = (System.currentTimeMillis() - downloadStartTime) / 1000;
        long speedKibPS = elapsedTimeSecs == 0 ? 0 : downloadedBytes / elapsedTimeSecs / 1024;
        cb.setInfo("", String.format("%.2f MiB in %02d:%02d, Average speed %d KiB/s",
                downloadedBytes * 1f / 1024 / 1024, elapsedTimeSecs / 60, elapsedTimeSecs % 60, speedKibPS));
    }

    private void urlToStream(URL url, long expectedSize, OutputStream target, ProgressReceiver cb) throws IOException {
        URI requestUri;
        try {
            requestUri = url.toURI();
        } catch (URISyntaxException e) { throw new IOException(e); }
        HttpResponse<InputStream> httpResponse = DownloadTask.sendHttpRequest(requestUri);
        long fileSize = Long.parseLong(httpResponse.headers().firstValue("Content-Length").orElse(Long.toString(expectedSize)));

        long downloadedBytesBefore = downloadedBytes;
        try {
            try (BufferedOutputStream bos = new BufferedOutputStream(target); InputStream inputStream = DownloadTask.unwrapHttpResponse(httpResponse)) {
                final ProgressOutputStream pOfs = new ProgressOutputStream(bos, new ProgressOutputStream.WriteListener() {
                    long lastAmount = -1;
                    final long noticeDivisor = 8192;

                    @Override
                    public void registerWrite(long amountOfBytesWritten) throws IOException {
                        if (lastAmount / noticeDivisor != amountOfBytesWritten / noticeDivisor) {
                            downloadedBytes += (amountOfBytesWritten - lastAmount);
                            long elapsedTimeSecs = (System.currentTimeMillis() - downloadStartTime) / 1000;
                            if (fileSize > 0) {
                                String message = String.format(": %5d KiB / %5d KiB; %5d KiB/s",
                                        amountOfBytesWritten / 1024, fileSize / 1024, elapsedTimeSecs == 0 ? 0 : downloadedBytes / elapsedTimeSecs / 1024);
                                cb.setProgress(amountOfBytesWritten * 1f / fileSize, 0);
                                cb.setInfo(String.format("%.2f%%", amountOfBytesWritten * 1f / fileSize * 100), message);
                            } else {
                                String message = String.format(": %5d KiB downloaded; %5d KiB/s",
                                        amountOfBytesWritten / 1024, elapsedTimeSecs == 0 ? 0 : downloadedBytes / elapsedTimeSecs / 1024);
                                cb.setProgress((System.currentTimeMillis() % 1000) / 1000f, 0);
                                cb.setInfo(String.format("%.2f%%", (System.currentTimeMillis() % 1000) / 1000f * 100), message);
                            }
                            lastAmount = amountOfBytesWritten;
                        }
                    }
                });
                IOUtils.copy(new BufferedInputStream(inputStream), pOfs);
            }
        } catch (Exception ex) {
            downloadedBytes = downloadedBytesBefore;
            throw ex;
        }
        downloadedBytes = downloadedBytesBefore + fileSize;
    }

    public void assertMetadataVersion(JsonObject metadataObj) throws MismatchingVersionException {
        if (metadataObj.has("client_version")) {
            String requestedVer = metadataObj.get("client_version").getAsString();
            if (!MtrVersion.parse(ResourcePackUpdater.MOD_VERSION).matches(requestedVer)) {
                throw new MismatchingVersionException(requestedVer, ResourcePackUpdater.MOD_VERSION);
            }
        }
        int metadataVersion = 1;
        if (metadataObj.has("version")) metadataVersion = metadataObj.get("version").getAsInt();
        if (metadataVersion > 2) throw new MismatchingVersionException("Unsupported metadata protocol version: " + metadataVersion);
    }
}
