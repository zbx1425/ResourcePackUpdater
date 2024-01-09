package cn.zbx1425.resourcepackupdater.gui.forms;

import cn.zbx1425.resourcepackupdater.gui.gl.GlHelper;
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
        GlHelper.blit(0, 0, GlHelper.getWidth(), GlHelper.getHeight(), 0x88000000);

        if (exception != null) {
            GlHelper.drawShadowString(20, 20, GlHelper.getWidth() - 40, LINE_HEIGHT, FONT_SIZE,
                    "There was an error! Please report.",
                    0xFFFF0000, false, true);
        }
        GlHelper.drawShadowString(GlHelper.getWidth() - 240 - 20, 20, 240, 16, 16, "Arrow Keys to Scroll", 0xffdddddd, false, true);
        int fontColor = System.currentTimeMillis() % 400 >= 200 ? 0xffffff00 : 0xffdddddd;
        GlHelper.drawShadowString(20, 20 + LINE_HEIGHT, GlHelper.getWidth() - 40, LINE_HEIGHT, FONT_SIZE,
                "Press ENTER to continue without the resource pack.",
                fontColor, false, true);

        final int LOG_FONT_SIZE = 16;
        final int LOG_LINE_HEIGHT = 20;
        float logBegin = 20 + LOG_LINE_HEIGHT * 3 + 20;
        float usableLogHeight = GlHelper.getHeight() - logBegin - 20;
        for (int i = logViewOffset; i < logs.size(); i++) {
            GlHelper.drawShadowString(20, logBegin + LOG_LINE_HEIGHT * (i - logViewOffset), GlHelper.getWidth() - 40, usableLogHeight, LOG_FONT_SIZE,
                    logs.get(i), 0xFFDDDDDD, false, true);
        }
        GlHelper.end();
    }

    @Override
    public boolean shouldStopPausing() {
        var glfwWindow = Minecraft.getInstance().getWindow().getWindow();

        final int LOG_LINE_HEIGHT = 20;
        float logBegin = 60 + LOG_LINE_HEIGHT * 3 + 40;
        float usableLogHeight = GlHelper.getHeight() - logBegin - 20;
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
        float usableLogHeight = GlHelper.getHeight() - logBegin - 20;
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
        printLog("");
        printLog("Update failed with this exception: ");
        for (String line : exception.toString().split("\n")) {
            printLog(line);
        }
    }
}
