package cn.zbx1425.resourcepackupdater.gui;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.io.ProgressReceiver;
import com.google.common.base.Throwables;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class GlProgressScreen implements ProgressReceiver {

    private final List<String> logs = new ArrayList<>();
    private String primaryInfo = "";
    private float primaryProgress;
    private float secondaryProgress;
    private Exception exception;
    private boolean paused;

    private int logViewOffset = 0;

    @Override
    public void printLog(String line) throws GlHelper.MinecraftStoppingException {
        logs.add(line);
        primaryInfo = line;
        ResourcePackUpdater.LOGGER.info(line);

        final int LOG_LINE_HEIGHT = 20;
        float logBegin = 60 + LOG_LINE_HEIGHT * 3 + 40;
        float usableLogHeight = Minecraft.getInstance().getWindow().getHeight() - logBegin - 20;
        int logLines = (int) Math.floor(usableLogHeight / LOG_LINE_HEIGHT);
        logViewOffset = Math.max(0, logs.size() - logLines);

        redrawScreen(true);
    }

    @Override
    public void amendLastLog(String postfix) throws GlHelper.MinecraftStoppingException {
        logs.set(logs.size() - 1, logs.get(logs.size() - 1) + postfix);
        redrawScreen(true);
    }

    @Override
    public void setProgress(float primary, float secondary) throws GlHelper.MinecraftStoppingException {
        this.primaryProgress = primary;
        this.secondaryProgress = secondary;
        redrawScreen(true);
    }

    @Override
    public void setSecondaryProgress(float secondary, String textValue) throws GlHelper.MinecraftStoppingException {
        this.secondaryProgress = secondary;
        this.primaryInfo = textValue;
        redrawScreen(true);
    }

    @Override
    public void setException(Exception exception) throws GlHelper.MinecraftStoppingException {
        this.exception = exception;
        this.paused = true;
        ResourcePackUpdater.LOGGER.error(exception);
        redrawScreen(true);
    }

    public boolean pause(boolean swap) throws GlHelper.MinecraftStoppingException {
        var glfwWindow = Minecraft.getInstance().getWindow().getWindow();
        paused = !InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_RETURN);
        if (paused) {
            final int LOG_LINE_HEIGHT = 20;
            float logBegin = 60 + LOG_LINE_HEIGHT * 3 + 40;
            float usableLogHeight = Minecraft.getInstance().getWindow().getHeight() - logBegin - 20;
            int logLines = (int) Math.floor(usableLogHeight / LOG_LINE_HEIGHT);
            int maxLogViewOffset = Math.max(0, logs.size() - logLines);

            if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_HOME)) {
                logViewOffset = 0;
            } else if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_END)) {
                logViewOffset = maxLogViewOffset;
            } else if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_PAGEUP)) {
                logViewOffset = Math.max(0, logViewOffset - logLines);
            } else if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_PAGEDOWN)) {
                logViewOffset = Math.min(maxLogViewOffset, logViewOffset + logLines);
            } else if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_UP)) {
                logViewOffset = Math.max(0, logViewOffset - 1);
            } else if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_DOWN)) {
                logViewOffset = Math.min(maxLogViewOffset, logViewOffset + 1);
            }
            redrawScreen(swap);
        }
        return paused;
    }

    public void reset() {
        logs.clear();
        primaryInfo = "";
        primaryProgress = 0;
        secondaryProgress = 0;
        exception = null;
        paused = false;
    }

    public boolean hasException() {
        return exception != null;
    }

    public void redrawScreen(boolean swap) throws GlHelper.MinecraftStoppingException {
        GlHelper.clearScreen(1f, 0f, 1f);
        var window = Minecraft.getInstance().getWindow();
        final int FONT_SIZE = 24;
        final int LINE_HEIGHT = 30;

        GlHelper.begin();
        RenderSystem.setShaderTexture(0, GlHelper.PRELOAD_FONT_TEXTURE);
        GlHelper.drawBlueGradientBackground();
        if (exception == null) {
            GlHelper.drawString(20, 60, window.getWidth() - 40, LINE_HEIGHT * 2, FONT_SIZE,
                    String.format("%3d%%\n%3d%%\n", Math.round(primaryProgress * 100), Math.round(secondaryProgress * 100)),
                    0xFFFFFFFF, true, true);
            if (paused) {
                GlHelper.drawString(window.getWidth() - 144 - 20, 20, 144, 16, 16, "Scroll AVAIL", 0xFFFFFFFF, false, true);
                int backColor = System.currentTimeMillis() % 400 >= 200 ? 0xFF008800 : 0xFF000000;
                GlHelper.blit(0, 60 + LINE_HEIGHT * 2, window.getWidth(), LINE_HEIGHT, backColor);
                int color = System.currentTimeMillis() % 400 < 200 ? 0xFFFF0000 : 0xFFFFFF00;
                GlHelper.drawString(20, 60 + LINE_HEIGHT * 2, window.getWidth() - 40, LINE_HEIGHT, FONT_SIZE,
                        "Press ENTER to proceed.",
                        color, false, true);
            } else {
                GlHelper.drawString(window.getWidth() - 144 - 20, 20, 144, 16, 16, "Cancel: ESC", 0xFFFFFFFF, false, true);
                boolean monospace = primaryInfo.length() > 0 && primaryInfo.charAt(0)== ':';
                GlHelper.drawString(20, 60 + LINE_HEIGHT * 2, window.getWidth() - 40, LINE_HEIGHT, FONT_SIZE,
                        primaryInfo,
                        0xFFFFFFFF, monospace, true);
            }
            float barBegin = 20 + FONT_SIZE * 2 + 20;
            float usableBarWidth = window.getWidth() - barBegin - 50;
            GlHelper.blit(barBegin, 60 + 3, window.getWidth() - barBegin - 40, LINE_HEIGHT - 6, 0xFFDDDDDD);
            GlHelper.blit(barBegin + 3, 60 + 6, window.getWidth() - barBegin - 46, LINE_HEIGHT - 12, 0xFF222222);
            GlHelper.blit(barBegin + 5, 60 + 8, usableBarWidth * primaryProgress, LINE_HEIGHT - 16, 0xFFFFFF00);
            GlHelper.blit(barBegin, 60 + LINE_HEIGHT + 3, window.getWidth() - barBegin - 40, LINE_HEIGHT - 6, 0xFFDDDDDD);
            GlHelper.blit(barBegin + 3, 60 + LINE_HEIGHT + 6, window.getWidth() - barBegin - 46, LINE_HEIGHT - 12, 0xFF222222);
            GlHelper.blit(barBegin + 5, 60 + LINE_HEIGHT + 8, usableBarWidth * secondaryProgress, LINE_HEIGHT - 16, 0xFF00FFFF);

            final int LOG_FONT_SIZE = 16;
            final int LOG_LINE_HEIGHT = 20;
            float logBegin = 60 + LOG_LINE_HEIGHT * 3 + 40;
            float usableLogHeight = Minecraft.getInstance().getWindow().getHeight() - logBegin - 20;
            for (int i = logViewOffset; i < logs.size(); i++) {
                GlHelper.drawString(20, logBegin + LOG_LINE_HEIGHT * (i - logViewOffset), window.getWidth() - 40, usableLogHeight, LOG_FONT_SIZE,
                        logs.get(i), 0xFFDDDDDD, false, true);
            }
        } else {
            GlHelper.drawString(20, 60, window.getWidth() - 40, LINE_HEIGHT, FONT_SIZE,
                    "There was an error!",
                    0xFFFF0000, false, true);
            if (paused) {
                GlHelper.drawString(window.getWidth() - 144 - 20, 20, 144, 16, 16, "Scroll AVAIL", 0xFFFFFFFF, false, true);
                int backColor = System.currentTimeMillis() % 400 >= 200 ? 0xFF880000 : 0xFF000000;
                GlHelper.blit(0, 60 + LINE_HEIGHT, window.getWidth(), LINE_HEIGHT, backColor);
                int color = System.currentTimeMillis() % 400 < 200 ? 0xFFFF0000 : 0xFFFFFF00;
                GlHelper.drawString(20, 60 + LINE_HEIGHT, window.getWidth() - 40, LINE_HEIGHT, FONT_SIZE,
                        "Please report. Press ENTER to proceed.",
                        color, false, true);
            } else {
                GlHelper.drawString(20, 60 + LINE_HEIGHT, window.getWidth() - 40, LINE_HEIGHT, FONT_SIZE,
                        primaryInfo,
                        0xFFFFFFFF, true, true);
            }
            final int LOG_FONT_SIZE = 16;
            GlHelper.drawString(20, 60 + LINE_HEIGHT * 2 + 10, window.getWidth() - 40,  window.getHeight() - 100 - 10, LOG_FONT_SIZE,
                    Throwables.getStackTraceAsString(exception),
                    0xFFDDDDDD, false, false);
        }
        GlHelper.end();

        GlHelper.begin();
        // GlHelper.drawString(20, 20, window.getWidth() - 40, LINE_HEIGHT, LINE_HEIGHT,
        //        "Resource Pack Updater © Zbx1425", 0xFFFFFF00, false, true);
        RenderSystem.setShaderTexture(0, GlHelper.PRELOAD_HEADER_TEXTURE);
        GlHelper.blit(20, 20, 512, 32, 0, 0, 1, 1, 0xFFFFFFFF);
        GlHelper.end();

        if (swap) {
            GlHelper.swapBuffer();
            if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_ESCAPE)) {
                throw new GlHelper.MinecraftStoppingException();
            }
        }
    }
}
