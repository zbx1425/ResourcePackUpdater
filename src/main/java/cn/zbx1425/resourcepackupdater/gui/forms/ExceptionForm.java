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

    private static final float exceptionFormWidth = 600, exceptionFormHeight = 400;

    @Override
    public void render() {
        GlHelper.setMatCenterForm(exceptionFormWidth, exceptionFormHeight, 0.75f);
        GlHelper.begin(GlHelper.PRELOAD_FONT_TEXTURE);
        GlScreenForm.drawShadowRect(exceptionFormWidth, exceptionFormHeight, 0xffdee6ea);

        GlHelper.drawShadowString(exceptionFormWidth - 240 - 20, 20, 240, 16, 16, "Arrow Keys to Scroll", 0xffdddddd, false, true);
        int backColor = System.currentTimeMillis() % 400 >= 200 ? 0xff9722ff : 0xFF000000;
        GlHelper.blit(0, 0, exceptionFormWidth, LINE_HEIGHT, backColor);
        GlHelper.drawString(20, 0, exceptionFormWidth - 40, LINE_HEIGHT, FONT_SIZE,
                "There was an error, please report! Press ENTER to proceed.",
                0xff222222, false, true);

        final int LOG_FONT_SIZE = 16;
        final int LOG_LINE_HEIGHT = 20;
        float logBegin = 40;
        float usableLogHeight = exceptionFormHeight - logBegin - 20;
        for (int i = logViewOffset; i < logs.size(); i++) {
            GlHelper.drawShadowString(20, logBegin + LOG_LINE_HEIGHT * (i - logViewOffset), exceptionFormWidth - 40, usableLogHeight, LOG_FONT_SIZE,
                    logs.get(i), 0xFFDDDDDD, false, true);
        }

        GlHelper.end();
    }

    @Override
    public boolean shouldStopPausing() {
        var glfwWindow = Minecraft.getInstance().getWindow().getWindow();

        final int LOG_LINE_HEIGHT = 20;
        float logBegin = 60 + LOG_LINE_HEIGHT * 3 + 40;
        float usableLogHeight = exceptionFormHeight - logBegin - 20;
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

        return !InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_RETURN);
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
        float logBegin = 40;
        float usableLogHeight = exceptionFormHeight - logBegin - 20;
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
    public void setSecondaryProgress(float value, String textValue) throws GlHelper.MinecraftStoppingException {

    }

    @Override
    public void setException(Exception exception) throws GlHelper.MinecraftStoppingException {
        this.exception = exception;
        for (String line : exception.toString().split("\n")) {
            printLog(line);
        }
    }
}
