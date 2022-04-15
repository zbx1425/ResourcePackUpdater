package cn.zbx1425.resourcepackupdater;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Work {

    private static final Logger LOGGER = LogManager.getLogger("ResourcepackUpdater");
    private static final String DEFAULT_URL = "rsync://xinghaicity.ldiorstudio.cn/srpack";
    private static final String LOCAL_NAME = "pack.zip";

    public static String resultMessage = "尚未检查更新";

    public static void download() {
        final int tcpProgressPort;
        final ServerSocket tcpServer;
        PrintWriter tcpProgressWriter = null;
        try {
            tcpServer = new ServerSocket(0, 50, InetAddress.getLocalHost());
            tcpProgressPort = tcpServer.getLocalPort();
        } catch (IOException ex) {
            LOGGER.error(ex);
            resultMessage = "更新资源包时发生错误：\nException when updating resource pack:\n" + ex.toString();
            return;
        }

        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "resourcepackupdater.txt");
        String url = DEFAULT_URL;
        if (configFile.exists()) {
            try {
                url = Files.readString(configFile.toPath()).trim();
                if (url.isEmpty()) url = DEFAULT_URL;
            } catch (Exception ignored) {

            }
        }
        String sx = FabricLoader.getInstance().getGameDir().toString();


        try {
            URL sourceURL = new URL(url);
            URL checksumURL = new URL(url + ".sha1");
            File rFile = Paths.get(sx, "resourcepacks", LOCAL_NAME).toFile();
            rFile.createNewFile();

            String remoteShaString = new String(checksumURL.openStream().readAllBytes()).trim().toLowerCase();
            String localShaString = calcSHA1(rFile).trim().toLowerCase();

            if (!remoteShaString.equals(localShaString)) {
                final Socket tcpProgressSocket;
                try {
                    new ProcessBuilder(
                            // "cmd", "/c", "start", "",
                            getJvmPath(), "-cp",
                            new File(Work.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath(),
                            "cn.zbx1425.resourcepackupdater.IPCHostEntryPoint", Integer.toString(tcpProgressPort)
                    ).start();
                    tcpProgressSocket = tcpServer.accept();
                } catch (IOException ex) {
                    LOGGER.error(ex);
                    resultMessage = "更新资源包时发生错误：\nException when updating resource pack:\n" + ex.toString();
                    return;
                }

                tcpProgressWriter = new PrintWriter(tcpProgressSocket.getOutputStream(), true, StandardCharsets.UTF_8);

                LOGGER.info("Resource pack needs update. Starting download.");
                tcpProgressWriter.write("资源包需要更新。正在开始下载。\nResource pack needs update. Starting download.\n……\n");
                tcpProgressWriter.flush();

                // Notify the user
                /* Runtime.getRuntime().exec(new String[] {
                    "cmd", "/c", "start", "cmd",
                    "/c", "echo 正在下载资源包更新,请稍等。下载完后游戏将正常启动。 && echo. && ping localhost -n 6 >nul"
                }); */

                int fileSize = getFileSize(sourceURL);

                FileOutputStream fos = new FileOutputStream(rFile);
                PrintWriter finalTcpProgressWriter = tcpProgressWriter;
                ProgressOutputStream pos = new ProgressOutputStream(fos, new ProgressOutputStream.WriteListener() {
                    long lastAmount = 0;
                    final long noticeDivisor = 16384;

                    @Override
                    public void registerWrite(long amountOfBytesWritten) {
                        if (lastAmount / noticeDivisor != amountOfBytesWritten / noticeDivisor) {
                            try {
                                finalTcpProgressWriter.write("正在下载资源包更新。\nDownloading resource pack update.\n"
                                        + (amountOfBytesWritten / 1024) + " / " + (fileSize / 1024) + " KiB ...\n");
                                finalTcpProgressWriter.flush();
                            } catch (Exception ex) {
                                System.out.println(ex.toString());
                            }

                            lastAmount = amountOfBytesWritten;
                        }
                    }
                });
                IOUtils.copy(sourceURL.openStream(), pos);

                localShaString = calcSHA1(rFile).trim().toLowerCase();
                if (!remoteShaString.equals(localShaString)) {
                    LOGGER.error("Resource pack update finished, but SHA1 mismatches. Remote: "
                            + remoteShaString + ", Local: " + localShaString);
                    resultMessage = "检查到更新版本；已经下载，但下载到的文件有问题。\nResource pack update finished, but SHA1 mismatches.\nRemote: "
                            + remoteShaString + ", Local:" + localShaString;
                } else {
                    LOGGER.info("Resource pack update finished. SHA1: " + remoteShaString);
                    resultMessage = "检查到更新版本；已经下载。\nResource pack update finished.\nSHA1: " + remoteShaString;
                }
            } else {
                LOGGER.info("Resource pack does not need update. SHA1: " + remoteShaString);
                resultMessage = "已检查；资源包无需更新。\nChecked; Resource pack does not need update.\nSHA1: " + remoteShaString;
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
            resultMessage = "更新资源包时发生错误：\nException when updating resource pack:\n" + ex.toString();
        }
        try {
            if (tcpProgressWriter != null) {
                tcpProgressWriter.write(resultMessage + "\n");
                tcpProgressWriter.write("end\n");
                tcpProgressWriter.flush();
                tcpServer.close();
            }
        } catch (Exception ignored) {

        }
    }

    private static int getFileSize(URL url) {
        URLConnection conn = null;
        try {
            conn = url.openConnection();
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).setRequestMethod("HEAD");
            }
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            return 0;
        } finally {
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).disconnect();
            }
        }
    }

    public static void updateOption() {
        try {
            File optionFile = new File(FabricLoader.getInstance().getGameDir().toFile(), "options.txt");
            if (!optionFile.exists()) {
                LOGGER.info("Creating options.txt in order to enable resource pack.");
                optionFile.createNewFile();
                Files.writeString(optionFile.toPath(),
                        "resourcePacks:[\"vanilla\",\"Fabric Mods\",\"file/" + LOCAL_NAME + "\"]");
            } else {
                boolean needWrite = false;
                StringBuilder sb = new StringBuilder();
                List<String> lines = Files.readAllLines(optionFile.toPath());
                for (String line : lines) {
                    String[] tokens = line.trim().split(":", 2);
                    if (tokens[0].toLowerCase().trim().equals("resourcepacks")) {
                        JsonArray ja = (JsonArray) new JsonParser().parse(tokens[1]);
                        if (!ja.contains(new JsonPrimitive("file/" + LOCAL_NAME))) {
                            LOGGER.info("Resource pack not enabled in options.txt, enabling.");
                            needWrite = true;
                            ja.add("file/" + LOCAL_NAME);
                        }
                        sb.append("resourcePacks:").append(ja.toString()).append("\r\n");
                    } else {
                        sb.append(line).append("\r\n");
                    }
                }
                if (needWrite) Files.writeString(optionFile.toPath(), sb.toString());
            }
        } catch (Exception ignored) {

        }
    }

    private static String calcSHA1(File file) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        try (InputStream input = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int len = input.read(buffer);

            while (len != -1) {
                sha1.update(buffer, 0, len);
                len = input.read(buffer);
            }

            return bytesToHex(sha1.digest());
        }
    }

    private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    private static String getJvmPath() {
        boolean isRunningOnWindowsPlatform = System.getProperties().getProperty("os.name").toUpperCase().contains("WINDOWS");
        if (isRunningOnWindowsPlatform)
            return System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
        else
            return System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }

}
