package cn.zbx1425.resourcepackupdater.gui;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.gui.forms.ExceptionForm;
import cn.zbx1425.resourcepackupdater.gui.forms.GlScreenForm;
import cn.zbx1425.resourcepackupdater.gui.forms.ProgressForm;
import cn.zbx1425.resourcepackupdater.gui.forms.SelectSourceForm;
import cn.zbx1425.resourcepackupdater.gui.gl.GlHelper;
import cn.zbx1425.resourcepackupdater.io.ProgressReceiver;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class GlProgressScreen implements ProgressReceiver {

    private final SelectSourceForm selectSourceForm = new SelectSourceForm();
    private final ProgressForm progressForm = new ProgressForm();
    private final ExceptionForm exceptionForm = new ExceptionForm();
    private final List<GlScreenForm> forms = List.of(selectSourceForm, progressForm, exceptionForm);

    private GlScreenForm activeForm = progressForm;

    private boolean paused;

    @Override
    public void printLog(String line) throws GlHelper.MinecraftStoppingException {
        for (GlScreenForm form : forms) form.printLog(line);
        ResourcePackUpdater.LOGGER.info(line);
        redrawScreen(true);
    }

    @Override
    public void amendLastLog(String postfix) throws GlHelper.MinecraftStoppingException {
        for (GlScreenForm form : forms) form.amendLastLog(postfix);
        redrawScreen(true);
    }

    @Override
    public void setProgress(float primary, float secondary) throws GlHelper.MinecraftStoppingException {
        for (GlScreenForm form : forms) form.setProgress(primary, secondary);
        redrawScreen(true);
    }

    @Override
    public void setInfo(String secondary, String textValue) throws GlHelper.MinecraftStoppingException {
        for (GlScreenForm form : forms) form.setInfo(secondary, textValue);
        redrawScreen(true);
    }

    @Override
    public void setException(Exception exception) throws GlHelper.MinecraftStoppingException {
        for (GlScreenForm form : forms) form.setException(exception);
        activeForm = exceptionForm;
        this.paused = true;
        ResourcePackUpdater.LOGGER.error("Resource Update Exception", exception);
    }

    public boolean shouldContinuePausing(boolean swap) throws GlHelper.MinecraftStoppingException {
        paused = paused && !activeForm.shouldStopPausing();
        if (paused) redrawScreen(swap);
        return paused;
    }

    public void reset() {
        for (GlScreenForm form : forms) form.reset();
        activeForm = progressForm;
        paused = false;
    }

    public void resetToSelectSource() {
        for (GlScreenForm form : forms) form.reset();
        activeForm = selectSourceForm;
        paused = true;
    }

    public void setToException() {
        activeForm = exceptionForm;
        paused = true;
    }

    public void redrawScreen(boolean swap) throws GlHelper.MinecraftStoppingException {
        GlHelper.clearScreen(1f, 0f, 1f);

        drawBackground();
        activeForm.shouldStopPausing();
        activeForm.render();

        if (swap) {
            GlHelper.swapBuffer();
        }
    }


    public static final ResourceLocation PRELOAD_HEADER_TEXTURE =
            new ResourceLocation(ResourcePackUpdater.MOD_ID, "textures/gui/header.png");

    public static final ResourceLocation PRELOAD_BACKGROUND_TEXTURE =
            new ResourceLocation(ResourcePackUpdater.MOD_ID, "textures/gui/background.png");

    private static void drawBackground() {
        GlHelper.setMatScaledPixel();
        GlHelper.begin(PRELOAD_BACKGROUND_TEXTURE);
        Minecraft.getInstance().getTextureManager().getTexture(PRELOAD_BACKGROUND_TEXTURE).setFilter(true, false);
        float bgScale = Math.max(GlHelper.getWidth() / 16f, GlHelper.getHeight() / 9f);
        float bgW = 16 * bgScale, bgH = 9 * bgScale;
        float bgX = (GlHelper.getWidth() - bgW) / 2, bgY = (GlHelper.getHeight() - bgH) / 2;
        GlHelper.blit(bgX, bgY, bgW, bgH, 0, 0, 1, 1, 0xffffffff);
        GlHelper.end();

        GlHelper.begin(GlHelper.PRELOAD_FONT_TEXTURE);
        GlHelper.drawShadowString(GlHelper.getWidth() - 10 - 80, GlHelper.getHeight() - 10 - 16, 80, 20, 16,
                "v" + ResourcePackUpdater.MOD_VERSION, 0xffffff00, false, true);
        GlHelper.end();

        GlHelper.begin(PRELOAD_HEADER_TEXTURE);
        float hdW = 512, hdH = hdW * 32 / 512;
        GlHelper.blit(10, GlHelper.getHeight() - 10 - hdH, hdW, hdH, 0, 0, 1, 1, 0xffffffff);
        GlHelper.end();
    }
}
