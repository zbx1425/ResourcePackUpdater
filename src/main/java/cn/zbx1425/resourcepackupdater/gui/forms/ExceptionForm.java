package cn.zbx1425.resourcepackupdater.gui.forms;

import cn.zbx1425.resourcepackupdater.gui.GlHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class ExceptionForm implements GlScreenForm {

    private final List<String> logs = new ArrayList<>();
    private int logViewOffset = 0;
    private Exception exception;

    @Override
    public void render() {
        GlHelper.setMatScaledPixel();
        GlHelper.begin(GlHelper.PRELOAD_FONT_TEXTURE);
        GlHelper.blit(0, 0, GlHelper.getScaledWidth(), GlHelper.getScaledHeight(), 0x88000000);

        // Minecraft.getInstance().getTextureManager().getTexture(GlHelper.PRELOAD_FONT_TEXTURE).setFilter(true, false);
        GlHelper.drawBlueGradientBackground();
        if (exception != null) {
            GlHelper.drawShadowString(20, 60, GlHelper.getScaledWidth() - 40, LINE_HEIGHT, FONT_SIZE,
                    "There was an error! Please report.",
                    0xFFFF0000, false, true);
        }
        GlHelper.drawShadowString(GlHelper.getScaledWidth() - 240 - 20, 20, 240, 16, 16, "Arrow Keys to Scroll", 0xffdddddd, false, true);
        int backColor = System.currentTimeMillis() % 400 >= 200 ? 0xff9722ff : 0xFF000000;
        GlHelper.blit(0, 60 + LINE_HEIGHT, GlHelper.getScaledWidth(), LINE_HEIGHT, backColor);
        GlHelper.drawShadowString(20, 60 + LINE_HEIGHT, GlHelper.getScaledWidth() - 40, LINE_HEIGHT, FONT_SIZE,
                "Press ENTER to proceed.",
                0xffdddddd, false, true);

        final int LOG_FONT_SIZE = 16;
        final int LOG_LINE_HEIGHT = 20;
        float logBegin = 60 + LOG_LINE_HEIGHT * 3 + 40;
        float usableLogHeight = GlHelper.getScaledHeight() - logBegin - 20;
        for (int i = logViewOffset; i < logs.size(); i++) {
            GlHelper.drawShadowString(20, logBegin + LOG_LINE_HEIGHT * (i - logViewOffset), GlHelper.getScaledWidth() - 40, usableLogHeight, LOG_FONT_SIZE,
                    logs.get(i), 0xFFDDDDDD, false, true);
        }
        GlHelper.end();
    }

    @Override
    public boolean shouldStopPausing() {
        var glfwWindow = Minecraft.getInstance().getWindow().getWindow();

        final int LOG_LINE_HEIGHT = 20;
        float logBegin = 60 + LOG_LINE_HEIGHT * 3 + 40;
        float usableLogHeight = GlHelper.getScaledHeight() - logBegin - 20;
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

        return InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_RETURN);
    }

    @Override
    public void reset() {
        logs.clear();
        exception = null;
    }

    @Override
    public void printLog(String line) throws GlHelper.MinecraftStoppingException {
        logs.add(line);
        final int LOG_LINE_HEIGHT = 20;
        float logBegin = 60 + LOG_LINE_HEIGHT * 3 + 40;
        float usableLogHeight = GlHelper.getScaledHeight() - logBegin - 20;
        int logLines = (int) Math.floor(usableLogHeight / LOG_LINE_HEIGHT);
        logViewOffset = Math.max(0, logs.size() - logLines);
    }

    @Override
    public void amendLastLog(String postfix) throws GlHelper.MinecraftStoppingException {
        logs.set(logs.size() - 1, logs.get(logs.size() - 1) + postfix);
    }

    @Override
    public void setProgress(float primary, float secondary) throws GlHelper.MinecraftStoppingException {

    }

    @Override
    public void setInfo(String value, String textValue) throws GlHelper.MinecraftStoppingException {

    }

    @Override
    public void setException(Exception exception) throws GlHelper.MinecraftStoppingException {
        this.exception = exception;
        for (String line : exception.toString().split("\n")) {
            printLog(line);
        }
    }
}
