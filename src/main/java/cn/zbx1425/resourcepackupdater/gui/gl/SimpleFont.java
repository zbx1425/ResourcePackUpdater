package cn.zbx1425.resourcepackupdater.gui.gl;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SimpleFont {

    public final float lineHeight;
    public final float sheetWidth, sheetHeight;
    public final float baseLineYPl;

    public final Char2ObjectMap<GlyphProperty> glyphMap = new Char2ObjectOpenHashMap<>();
    private static final char REPLACEMENT_CHAR = '▯';

    public final ResourceLocation textureLocation;
    public final float whiteU, whiteV;
    public final float spaceWidthPl;

    public SimpleFont(ResourceLocation textureLocation) {
        this.textureLocation = textureLocation;

        ResourceLocation metadataLocation = new ResourceLocation(textureLocation.getNamespace(),
                textureLocation.getPath().replace(".png", ".json"));
        JsonObject srcObj;
        try (InputStream metadataIs = getClass().getResourceAsStream("/assets/" + metadataLocation.getNamespace()
                + "/" + metadataLocation.getPath())) {
            assert metadataIs != null;
            srcObj = ResourcePackUpdater.JSON_PARSER.parse(IOUtils.toString(metadataIs, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.lineHeight = srcObj.get("size").getAsInt();
        this.sheetWidth = srcObj.get("width").getAsInt();
        this.sheetHeight = srcObj.get("height").getAsInt();

        JsonObject characterObj = srcObj.getAsJsonObject("characters");
        for (Map.Entry<String, JsonElement> entry : characterObj.entrySet()) {
            char codePoint = entry.getKey().charAt(0);
            glyphMap.put(codePoint, new GlyphProperty(entry.getValue().getAsJsonObject(), this));
        }

        baseLineYPl = -glyphMap.get('A').offsetYPl;
        GlyphProperty whiteGlyph = glyphMap.get('■');
        whiteU = (whiteGlyph.u1 + whiteGlyph.u2) / 2;
        whiteV = (whiteGlyph.v1 + whiteGlyph.v2) / 2;
        spaceWidthPl = glyphMap.get(' ').advancePl;
    }

    public GlyphProperty getGlyph(char chr) {
        if (!glyphMap.containsKey(chr)) {
            return glyphMap.get(REPLACEMENT_CHAR);
        }
        return glyphMap.get(chr);
    }

    public static class GlyphProperty {

        public final float u1, v1;
        public final float u2, v2;
        public final float widthPl, heightPl;
        public final float offsetXPl, offsetYPl;
        public final float advancePl;

        public GlyphProperty(JsonObject srcObj, SimpleFont font) {
            this.u1 = srcObj.get("x").getAsInt() * 1f / font.sheetWidth;
            this.v1 = srcObj.get("y").getAsInt() * 1f / font.sheetHeight;
            this.u2 = u1 + srcObj.get("width").getAsInt() * 1f / font.sheetWidth;
            this.v2 = v1 + srcObj.get("height").getAsInt() * 1f / font.sheetHeight;
            this.widthPl = srcObj.get("width").getAsInt() * 1f / font.lineHeight;
            this.heightPl = srcObj.get("height").getAsInt() * 1f / font.lineHeight;
            this.advancePl = srcObj.get("advance").getAsInt() * 1f / font.lineHeight;
            this.offsetXPl = -srcObj.get("originX").getAsInt() * 1f / font.lineHeight;
            this.offsetYPl = -srcObj.get("originY").getAsInt() * 1f / font.lineHeight;
        }
    }
}
