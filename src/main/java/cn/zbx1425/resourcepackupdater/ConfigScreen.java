package cn.zbx1425.resourcepackupdater;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public class ConfigScreen extends Screen {

    protected ConfigScreen() {
        super(new LiteralText("ResourcePackUpdater Config"));
    }

    @Override
    protected void init() {
        super.init();
        ButtonWidget actionBtn = new ButtonWidget(20, 20, 180, 20, new LiteralText("检查及下载更新"), (btn) -> {
            Work.download();
        });
        ButtonWidget continueBtn = new ButtonWidget(240, 20, 80, 20, new LiteralText("返回"), (btn) -> {
            assert client != null;
            client.setScreen((Screen)null);
        });
        addDrawableChild(actionBtn);
        addDrawableChild(continueBtn);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        String[] strs = Work.resultMessage.split("\n");
        for (int i = 0; i < strs.length; ++i) {
            this.textRenderer.drawWithShadow(matrices, strs[i], 20, 60 + i * 16, -1);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }
}
