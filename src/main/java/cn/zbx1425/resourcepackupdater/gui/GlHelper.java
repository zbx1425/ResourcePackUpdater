package cn.zbx1425.resourcepackupdater.gui;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public class GlHelper {

    public static void clearScreen(float r, float g, float b) {
        RenderSystem.clearColor(r, g, b, 1f);
        RenderSystem.clear(16640, Minecraft.ON_OSX);
    }

    private static ShaderInstance previousShader;
    private static Matrix4f lastProjectionMat;

    public static void initGlStates() {
        previousShader = RenderSystem.getShader();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.getModelViewStack().setIdentity();
        RenderSystem.applyModelViewMatrix();
        lastProjectionMat = RenderSystem.getProjectionMatrix();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
    }

    public static void resetGlStates() {
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(() -> previousShader);
        RenderSystem.setProjectionMatrix(lastProjectionMat);
    }

    public static final ResourceLocation PRELOAD_FONT_TEXTURE =
            new ResourceLocation(ResourcePackUpdater.MOD_ID, "textures/font/unicode_page_00.png");

    private static BufferBuilder bufferBuilder;

    public static void begin(ResourceLocation texture) {
        bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
    }

    public static void end() {
        Tesselator.getInstance().end();
    }

    public static void swapBuffer() throws MinecraftStoppingException {
        Window window = Minecraft.getInstance().getWindow();
        if (window.shouldClose()) {
            throw new MinecraftStoppingException();
        } else {
            window.updateDisplay();
        }
    }

    public static void drawBlueGradientBackground() {
        withColor(bufferBuilder.vertex(-1, 1, 1f).uv(118f/256f, 8f/256f), 0xff014e7c).endVertex();
        withColor(bufferBuilder.vertex(1, 1, 1f).uv(118f/256f, 8f/256f), 0xff0d1033).endVertex();
        withColor(bufferBuilder.vertex(1, -1, 1f).uv(118f/256f, 8f/256f), 0xff501639).endVertex();
        withColor(bufferBuilder.vertex(-1, -1, 1f).uv(118f/256f, 8f/256f), 0xff02142a).endVertex();
    }

    public static void blit(float x1, float y1, float width, float height, float u1, float v1, float u2, float v2, int color) {
        float x2 = x1 + width;
        float y2 = y1 + height;
        withColor(bufferBuilder.vertex(x1, y1, 1f).uv(u1, v1), color).endVertex();
        withColor(bufferBuilder.vertex(x2, y1, 1f).uv(u2, v1), color).endVertex();
        withColor(bufferBuilder.vertex(x2, y2, 1f).uv(u2, v2), color).endVertex();
        withColor(bufferBuilder.vertex(x1, y2, 1f).uv(u1, v2), color).endVertex();
    }

    public static void blit(float x1, float y1, float width, float height, int color) {
        float x2 = x1 + width;
        float y2 = y1 + height;
        withColor(bufferBuilder.vertex(x1, y1, 1f).uv(118f/256f, 8f/256f), color).endVertex();
        withColor(bufferBuilder.vertex(x2, y1, 1f).uv(118f/256f, 8f/256f), color).endVertex();
        withColor(bufferBuilder.vertex(x2, y2, 1f).uv(118f/256f, 8f/256f), color).endVertex();
        withColor(bufferBuilder.vertex(x1, y2, 1f).uv(118f/256f, 8f/256f), color).endVertex();
    }

    private static final byte[] GLYPH_SIZES = {
            0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
            0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
            0x0F, 0x44, 0x26, 0x16, 0x17, 0x17, 0x17, 0x44, 0x35, 0x24, 0x17, 0x17, 0x34, 0x16, 0x34, 0x16,
            0x16, 0x26, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x34, 0x34, 0x26, 0x16, 0x15, 0x16,
            0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x26, 0x17, 0x16, 0x16, 0x16, 0x16, 0x16,
            0x16, 0x17, 0x16, 0x16, 0x17, 0x16, 0x17, 0x16, 0x16, 0x17, 0x16, 0x46, 0x16, 0x13, 0x16, 0x17,
            0x24, 0x16, 0x16, 0x16, 0x16, 0x16, 0x15, 0x16, 0x16, 0x26, 0x15, 0x16, 0x26, 0x17, 0x16, 0x16,
            0x16, 0x16, 0x16, 0x16, 0x15, 0x16, 0x16, 0x17, 0x16, 0x16, 0x16, 0x35, 0x44, 0x24, 0x17, 0x0F,
            0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
            0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F, 0x0F,
            0x0F, 0x44, 0x17, 0x17, 0x16, 0x17, 0x44, 0x16, 0x25, 0x07, 0x26, 0x16, 0x16, 0x0F, 0x07, 0x16,
            0x24, 0x17, 0x26, 0x26, 0x35, 0x26, 0x16, 0x34, 0x24, 0x24, 0x26, 0x16, 0x16, 0x16, 0x16, 0x16,
            0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x17, 0x16, 0x16, 0x16, 0x16, 0x16, 0x26, 0x26, 0x26, 0x26,
            0x06, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x17, 0x16, 0x16,
            0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x17, 0x16, 0x16, 0x16, 0x16, 0x16, 0x26, 0x26, 0x26, 0x26,
            0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x16, 0x26, 0x16
    };

    public static void drawShadowString(float x1, float y1, float width, float height, float fontSize,
                                  String text, int color, boolean monospace, boolean noWrap) {
        drawString(x1 + fontSize / 16, y1 + fontSize / 16, width, height, fontSize, text, 0xFF222222, monospace, noWrap);
        drawString(x1, y1, width, height, fontSize, text, color, monospace, noWrap);
    }

    public static void drawString(float x1, float y1, float width, float height, float fontSize,
                                  String text, int color, boolean monospace, boolean noWrap) {
        float CHAR_SPACING = 0.15f;
        float LINE_SPACING = 0.25f;
        float SPACE_WIDTH = 0.5f;

        var x = x1;
        var y = y1;
        for (int chr : text.toCharArray()) {
            if (chr >= 256) {
                chr = 0;
            }
            if (chr == '\n') {
                y += fontSize + LINE_SPACING * fontSize;
                x = x1;
            } else if (chr == '\r') {
                // Ignore CR
            } else if (chr == '\t') {
                x += (SPACE_WIDTH * 4 + CHAR_SPACING) * fontSize;
            } else if (chr == ' ') {
                x += (SPACE_WIDTH + CHAR_SPACING) * fontSize;
            } else {
                float chr_size = GLYPH_SIZES[chr];
                float chr_left = (chr_size / 16 - 1) / 16f;
                float chr_right = (chr_size % 16 + 1) / 16f;

                if (monospace) {
                    if ((chr_right - chr_left) < 0.8) {
                        if (chr_left + 0.5 > 1f) {
                            chr_right = 1f;
                            chr_left = 0.5f;
                        } else {
                            chr_right = chr_left + 0.5f;
                        }
                    } else {
                        chr_right = 1f;
                        chr_left = 0f;
                    }
                }

                var chr_width = fontSize * (chr_right - chr_left);

                if (x + chr_width + CHAR_SPACING * fontSize > x1 + width) {
                    if (noWrap) {
                        continue;
                    } else {
                        y += fontSize + LINE_SPACING * fontSize;
                        x = x1;
                    }
                }
                if (y + fontSize > y1 + height) {
                    return;
                }

                var chr_sprite_u = chr % 16;
                var chr_sprite_v = (chr >> 4) % 16;
                float u1 = chr_sprite_u * (1f / 16f) + chr_left * 16f / 256f;
                float v1 = chr_sprite_v * (1f / 16f);
                float uSpan = (chr_right - chr_left) * 16f / 256f;
                float vSpan = 16f / 256f;
                blit(x, y, chr_width, fontSize, u1, v1, u1 + uSpan, v1 + vSpan, color);
                x += chr_width + CHAR_SPACING * fontSize;
            }
        }
    }

    public static void setMatIdentity() {
        RenderSystem.getModelViewStack().setIdentity();
    }

    public static void setMatPixel() {
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.multiply(Matrix4f.createScaleMatrix(2, -2, 1));
        matrix.multiply(Matrix4f.createTranslateMatrix(-0.5f, -0.5f, 0));
        float rawWidth = Minecraft.getInstance().getWindow().getWidth();
        float rawHeight = Minecraft.getInstance().getWindow().getHeight();
        matrix.multiply(Matrix4f.createScaleMatrix(1 / rawWidth, 1 / rawHeight, 1));
        RenderSystem.setProjectionMatrix(matrix);
    }

    public static void setMatCenterForm(float width, float height, float widthPercent) {
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.multiply(Matrix4f.createScaleMatrix(2, -2, 1));
        matrix.multiply(Matrix4f.createTranslateMatrix(-0.5f, -0.5f, 0));
        float rawWidth = Minecraft.getInstance().getWindow().getWidth();
        float rawHeight = Minecraft.getInstance().getWindow().getHeight();
        matrix.multiply(Matrix4f.createScaleMatrix(1 / rawWidth, 1 / rawHeight, 1));
        float formRawWidth = rawWidth * widthPercent;
        float formRawHeight = height / width * formRawWidth;
        matrix.multiply(Matrix4f.createTranslateMatrix((rawWidth - formRawWidth) / 2f, (rawHeight - formRawHeight) / 2f, 0));
        matrix.multiply(Matrix4f.createScaleMatrix(formRawWidth / width, formRawHeight / height, 1));
        RenderSystem.setProjectionMatrix(matrix);
    }

    private static VertexConsumer withColor(VertexConsumer vc, int color) {
        int a = color >>> 24 & 0xFF;
        int r = color >>> 16 & 0xFF;
        int g = color >>> 8 & 0xFF;
        int b = color & 0xFF;
        return vc.color(r, g, b, a);
    }

    public static class MinecraftStoppingException extends RuntimeException {
        public MinecraftStoppingException() {
            super("Minecraft is now stopping.");
        }
    }
}
