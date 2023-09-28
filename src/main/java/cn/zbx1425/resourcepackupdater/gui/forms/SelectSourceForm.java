package cn.zbx1425.resourcepackupdater.gui.forms;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.gui.gl.GlHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

import java.io.IOException;

public class SelectSourceForm implements GlScreenForm {

    public float selectSourceFormWidth = 500, selectSourceFormHeight = 400;

    int selectedIndex = -1;

    long countdownStartTime = -1;
    boolean countdownExpired = false;

    @Override
    public void render() {
        int sourceSize = ResourcePackUpdater.CONFIG.sourceList.value.size();
        selectSourceFormHeight = 30 + 30 + 30 + sourceSize * 40 + (sourceSize - 1) * 10;
        if (selectedIndex == -1) {
            selectedIndex = ResourcePackUpdater.CONFIG.sourceList.value.indexOf(ResourcePackUpdater.CONFIG.activeSource.value);
        }

        GlHelper.setMatCenterForm(selectSourceFormWidth, selectSourceFormHeight, 0.6f);
        GlHelper.begin(GlHelper.PRELOAD_FONT_TEXTURE);
        GlScreenForm.drawShadowRect(selectSourceFormWidth, selectSourceFormHeight, 0xffdee6ea);

        if (countdownStartTime > 0) {
            long countdown = ResourcePackUpdater.CONFIG.sourceSelectDelay.value - (System.currentTimeMillis() - countdownStartTime) / 1000;
            if (countdown > 0) {
                GlHelper.drawString(30, 30, selectSourceFormWidth - 60, 30, 20,
                        String.format("Select Source (%d)", countdown), 0xff222222, false, false);
            } else {
                countdownExpired = true;
            }
        } else {
            GlHelper.drawString(30, 30, selectSourceFormWidth - 60, 30, 20,
                    "Select Source", 0xff222222, false, false);
        }
        for (int i = 0; i < sourceSize; i++) {
            if (i == selectedIndex) {
                GlHelper.blit(30, 30 + 30 + i * 50, selectSourceFormWidth - 60, 40, 0xff63a0c6);
            } else {
                GlHelper.blit(30, 30 + 30 + i * 50, selectSourceFormWidth - 60, 40, 0xffc0d2db);
            }
            GlHelper.drawString(30 + 15, 30 + 30 + i * 50 + 10, selectSourceFormWidth - 90, 40, 20,
                    ResourcePackUpdater.CONFIG.sourceList.value.get(i).name, 0xff222222, false, false);
        }

        GlHelper.end();
    }

    private int heldKey = -1;

    @Override
    public boolean shouldStopPausing() {
        var glfwWindow = Minecraft.getInstance().getWindow().getWindow();
        if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_UP)) {
            if (heldKey != InputConstants.KEY_UP) {
                selectedIndex = Math.max(0, selectedIndex - 1);
                heldKey = InputConstants.KEY_UP;
                countdownStartTime = -1;
            }
        } else if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_DOWN)) {
            if (heldKey != InputConstants.KEY_DOWN) {
                selectedIndex = Math.min(ResourcePackUpdater.CONFIG.sourceList.value.size() - 1, selectedIndex + 1);
                heldKey = InputConstants.KEY_DOWN;
                countdownStartTime = -1;
            }
        } else if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_RETURN)
            || InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_SPACE)
            || InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_RIGHT)
            || countdownExpired) {
            ResourcePackUpdater.CONFIG.activeSource.value = ResourcePackUpdater.CONFIG.sourceList.value.get(selectedIndex);
            ResourcePackUpdater.CONFIG.activeSource.isFromLocal = true;
            try {
                ResourcePackUpdater.CONFIG.save();
            } catch (IOException ignored) { }
            return true;
        } else if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_ESCAPE)) {
            return true;
        } else {
            heldKey = -1;
        }
        return false;
    }

    @Override
    public void reset() {
        selectedIndex = -1;
        heldKey = -1;
        if (ResourcePackUpdater.CONFIG.sourceSelectDelay.value > 0) {
            countdownStartTime = System.currentTimeMillis();
        } else {
            countdownStartTime = -1;
        }
        countdownExpired = false;
    }

    @Override
    public void printLog(String line) throws GlHelper.MinecraftStoppingException {

    }

    @Override
    public void amendLastLog(String postfix) throws GlHelper.MinecraftStoppingException {

    }

    @Override
    public void setProgress(float primary, float secondary) throws GlHelper.MinecraftStoppingException {

    }

    @Override
    public void setInfo(String value, String textValue) throws GlHelper.MinecraftStoppingException {

    }

    @Override
    public void setException(Exception exception) throws GlHelper.MinecraftStoppingException {

    }
}
