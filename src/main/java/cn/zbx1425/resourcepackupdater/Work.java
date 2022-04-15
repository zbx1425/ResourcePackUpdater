package cn.zbx1425.resourcepackupdater;

import com.github.fracpete.processoutput4j.core.StreamingProcessOutputType;
import com.github.fracpete.processoutput4j.core.StreamingProcessOwner;
import com.github.fracpete.processoutput4j.output.CollectingProcessOutput;
import com.github.fracpete.processoutput4j.output.ConsoleOutputProcessOutput;
import com.github.fracpete.processoutput4j.output.StreamingProcessOutput;
import com.github.fracpete.rsync4j.RSync;
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
    private static final String OBSOLETE_LOCAL_NAME = "pack.zip";
    private static final String PACK_DIR_NAME = "SyncedPack";

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
        String sourceURL = DEFAULT_URL;
        if (configFile.exists()) {
            try {
                sourceURL = Files.readString(configFile.toPath()).trim();
                if (sourceURL.isEmpty()) sourceURL = DEFAULT_URL;
            } catch (Exception ignored) {

            }
        }
        String sx = FabricLoader.getInstance().getGameDir().toString();
        String userHomeBackup = System.getProperty("user.home");

        // Very dirty, but RSync4J doesn't expose the PID to us, so...
        try {
            ProcessHandle
                .allProcesses()
                .filter(p -> p.info().command().map(c -> c.contains("rsync")).orElse(false))
                .forEach(processHandle -> {
                    LOGGER.info("Killing: " + processHandle.info().command().orElse(Long.toString(processHandle.pid())));
                    processHandle.destroy();
                });
        } catch (Exception ex) {
            LOGGER.error(ex);
            // ignore
        }

        try {
            File oldFile = Paths.get(sx, "resourcepacks", OBSOLETE_LOCAL_NAME).toFile();
            if (oldFile.exists()) oldFile.delete();

            Socket tcpProgressSocket = null;
            try {
                new ProcessBuilder(
                        // "cmd", "/c", "start", "",
                        getJvmPath(), "-cp",
                        new File(Work.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath(),
                        "cn.zbx1425.resourcepackupdater.IPCHostEntryPoint", Integer.toString(tcpProgressPort)
                ).start();
                tcpProgressSocket = tcpServer.accept();
                tcpProgressWriter = new PrintWriter(tcpProgressSocket.getOutputStream(), true, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                // No info, no big deal
                LOGGER.warn(ex);
            }

            LOGGER.info("Starting download.");
            if (tcpProgressWriter != null) {
                tcpProgressWriter.write("正在开始下载。\nStarting download.\n……\n");
                tcpProgressWriter.flush();
            }

            System.setProperty("user.home", sx);

            RSync rsync = new RSync()
                .source(sourceURL)
                .destination(Paths.get(sx, "resourcepacks", PACK_DIR_NAME).toAbsolutePath().toString())
                .archive(true).checksum(true).humanReadable(true).info("progress2");

            PrintWriter finalTcpProgressWriter = tcpProgressWriter;
            StringBuilder stdErrBuilder = new StringBuilder();
            StreamingProcessOutput processOutput = new StreamingProcessOutput(new StreamingProcessOwner() {
                public StreamingProcessOutputType getOutputType() {
                    return StreamingProcessOutputType.BOTH;
                }
                public void processOutput(String line, boolean stdout) {
                    if (!stdout) {
                        stdErrBuilder.append(line).append("\n");
                    }
                    if (finalTcpProgressWriter != null) {
                        try {
                            finalTcpProgressWriter.write("正在下载资源包更新。\nDownloading resource pack update.\n");
                            finalTcpProgressWriter.write((stdout ? "" : "STDERR: ") + line + "\n");
                            finalTcpProgressWriter.flush();
                        } catch (Exception ignored) {

                        }
                    }
                }
            });

            boolean launchRsyncSucceed = false;
            try {
                ProcessBuilder builder = rsync.builder();
                processOutput.monitor(builder);
                launchRsyncSucceed = true;
            } catch (IllegalStateException ex) {
                if (ex.getMessage().contains("not installed")) {
                    LOGGER.error(ex);
                    resultMessage = "请您安装 rsync, ssh 和 ssh-keygen。\nPlease install rsync, ssh and ssh-keygen.\n" + ex.toString();
                } else if (ex.getMessage().contains("windows32")) {
                    LOGGER.error(ex);
                    resultMessage = "此版本的资源包更新程序不支持32位Windows，请联系作者获取32位版。\n" +
                            "32-bit Windows not supported, contact author for a 32-bit version of this resource pack updater.\n" + ex.toString();
                } else {
                    throw ex;
                }
            }

            if (launchRsyncSucceed) {
                LOGGER.info("Rsync exit code: " + processOutput.getExitCode());
                if (!processOutput.hasSucceeded()) {
                    LOGGER.error(stdErrBuilder.toString());
                    resultMessage = "更新资源包时发生错误：\nException when updating resource pack:\n" + stdErrBuilder.toString().split("\n")[0];
                } else {
                    LOGGER.info("Resource pack update finished.");
                    resultMessage = "更新已经完成。\nResource pack update finished.\n";
                }
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
            resultMessage = "更新资源包时发生错误：\nException when updating resource pack:\n" + ex.toString();
        }

        System.setProperty("user.home", userHomeBackup);

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
                        "resourcePacks:[\"vanilla\",\"Fabric Mods\",\"file/" + PACK_DIR_NAME + "\"]");
            } else {
                boolean needWrite = false;
                StringBuilder sb = new StringBuilder();
                List<String> lines = Files.readAllLines(optionFile.toPath());
                for (String line : lines) {
                    String[] tokens = line.trim().split(":", 2);
                    if (tokens[0].toLowerCase().trim().equals("resourcepacks")) {
                        JsonArray ja = (JsonArray) new JsonParser().parse(tokens[1]);
                        JsonPrimitive target = new JsonPrimitive("file/" + PACK_DIR_NAME);
                        if (ja.size() == 0 || !ja.get(ja.size() - 1).equals(target)) {
                            LOGGER.info("Resource pack not enabled in options.txt, enabling.");
                            needWrite = true;
                            if (ja.contains(target)) ja.remove(target);
                            ja.add(target);
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
