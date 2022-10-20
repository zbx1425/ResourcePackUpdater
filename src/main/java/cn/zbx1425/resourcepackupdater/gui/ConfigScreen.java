package cn.zbx1425.resourcepackupdater.gui;

import cn.zbx1425.resourcepackupdater.Config;
import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfigScreen extends Screen {

    public ConfigScreen() {
        super(new TextComponent("ResourcePackUpdater Config"));
    }

    private boolean isShowingLog = false;

    private final HashMap<Config.SourceProperty, Button> sourceButtons = new HashMap<>();

    @Override
    protected void init() {
        super.init();
        Button actionBtn = new Button(20, 20, 180, 20, new TextComponent("Show Logs from Last Run"), (btn) -> {
            isShowingLog = true;
        });
        Button continueBtn = new Button(240, 20, 80, 20, new TextComponent("Return"), (btn) -> {
            assert minecraft != null;
            minecraft.setScreen((Screen)null);
        });
        addRenderableWidget(actionBtn);
        addRenderableWidget(continueBtn);

        int btnY = 80;
        for (Config.SourceProperty source : ResourcePackUpdater.CONFIG.sourceList) {
            Button btnUseSource = new Button(20, btnY, 180, 20, new TextComponent(source.name), (btn) -> {
                ResourcePackUpdater.CONFIG.activeSource = source;
                try {
                    ResourcePackUpdater.CONFIG.save();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                updateBtnEnable();
            });
            sourceButtons.put(source, btnUseSource);
            btnY += 20;
            addRenderableWidget(btnUseSource);
        }
        updateBtnEnable();
    }

    private void updateBtnEnable() {
        for (var entry : sourceButtons.entrySet()) {
            entry.getValue().active = !ResourcePackUpdater.CONFIG.activeSource.equals(entry.getKey());
        }
    }

    @Override
    public void render(@NotNull PoseStack matrices, int mouseX, int mouseY, float delta) {
        if (isShowingLog) {
            GlHelper.initGlStates();
            try {
                if (!ResourcePackUpdater.GL_PROGRESS_SCREEN.pause(false)) {
                    isShowingLog = false;
                }
            } catch (GlHelper.MinecraftStoppingException ignored) {
                isShowingLog = false;
            }
            GlHelper.resetGlStates();
        } else {
            this.renderBackground(matrices);
            this.font.drawShadow(matrices, "Source Servers:", 20, 60, 0xFFFFFFFF);
            super.render(matrices, mouseX, mouseY, delta);
        }
    }
}
