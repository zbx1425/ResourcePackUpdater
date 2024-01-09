package cn.zbx1425.resourcepackupdater.gui.forms;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.gui.gl.GlHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

import java.io.IOException;

public class SelectSourceForm implements GlScreenForm {

    public float selectSourceFormWidth = 500, selectSourceFormHeight = 400;

    int selectedIndex = -1;

    boolean countdownExpired = false;

    @Override
    public void render() {
        int sourceSize = ResourcePackUpdater.CONFIG.sourceList.value.size() + 1;
        selectSourceFormHeight = 30 + 30 + 30 + sourceSize * 30 + (sourceSize - 1) * 10;
        if (selectedIndex == -1) {
            selectedIndex = ResourcePackUpdater.CONFIG.sourceList.value.indexOf(ResourcePackUpdater.CONFIG.selectedSource.value);
        }
        if (selectedIndex == -1) {
            selectedIndex = 0;
        }

        GlHelper.setMatCenterForm(selectSourceFormWidth, selectSourceFormHeight, 0.6f);
        GlHelper.begin(GlHelper.PRELOAD_FONT_TEXTURE);
        GlScreenForm.drawShadowRect(selectSourceFormWidth, selectSourceFormHeight, 0xffdee6ea);

        GlHelper.drawString(20, 15, selectSourceFormWidth - 40, 50, 18,
                "Select a Server to Download Resource Pack from\nDon't worry, you can later get back and try another one.", 0xff222222, false, false);

        for (int i = 0; i < sourceSize; i++) {
            if (i == selectedIndex) {
                GlHelper.blit(30, 30 + 30 + i * 40, selectSourceFormWidth - 60, 30, 0xff63a0c6);
            } else {
                GlHelper.blit(30, 30 + 30 + i * 40, selectSourceFormWidth - 60, 30, 0xffc0d2db);
            }
            String btnLabel = i == sourceSize - 1 ? "[Cancel Update]" : ResourcePackUpdater.CONFIG.sourceList.value.get(i).name;
            GlHelper.drawString(30 + 15, 30 + 30 + i * 40 + 5, selectSourceFormWidth - 90, 40, 20,
                    btnLabel, 0xff222222, false, false);
        }

        String escBtnHint = "W/S: Select, Enter: Confirm";
        GlHelper.drawString(20, selectSourceFormHeight - 20, selectSourceFormWidth - 40, 16, 16, escBtnHint, 0xff222222, false, true);

        GlHelper.end();
    }

    private int heldKey = -1;

    @Override
    public boolean shouldStopPausing() {
        var glfwWindow = Minecraft.getInstance().getWindow().getWindow();
        if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_UP) || InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_W)) {
            if (heldKey != InputConstants.KEY_UP && heldKey != InputConstants.KEY_W) {
                selectedIndex = Math.max(0, selectedIndex - 1);
                heldKey = InputConstants.KEY_UP;
            }
        } else if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_DOWN) || InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_S)) {
            if (heldKey != InputConstants.KEY_DOWN && heldKey != InputConstants.KEY_S) {
                selectedIndex = Math.min(ResourcePackUpdater.CONFIG.sourceList.value.size(), selectedIndex + 1);
                heldKey = InputConstants.KEY_DOWN;
            }
        } else if (InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_RETURN)
            || InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_SPACE)
            || InputConstants.isKeyDown(glfwWindow, InputConstants.KEY_RIGHT)
            || countdownExpired) {
            if (selectedIndex == ResourcePackUpdater.CONFIG.sourceList.value.size()) {
                throw new GlHelper.MinecraftStoppingException();
            }
            ResourcePackUpdater.CONFIG.selectedSource.value = ResourcePackUpdater.CONFIG.sourceList.value.get(selectedIndex);
            ResourcePackUpdater.CONFIG.selectedSource.isFromLocal = true;
            try {
                ResourcePackUpdater.CONFIG.save();
            } catch (IOException ignored) { }
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
