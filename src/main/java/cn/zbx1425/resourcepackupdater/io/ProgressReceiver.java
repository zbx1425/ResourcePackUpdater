package cn.zbx1425.resourcepackupdater.io;

import cn.zbx1425.resourcepackupdater.gui.gl.GlHelper;

public interface ProgressReceiver {

    void printLog(String line) throws GlHelper.MinecraftStoppingException;
    void amendLastLog(String postfix) throws GlHelper.MinecraftStoppingException;
    void setProgress(float primary, float secondary) throws GlHelper.MinecraftStoppingException;
    void setInfo(String aux1, String aux2) throws GlHelper.MinecraftStoppingException;
    void setException(Exception exception) throws GlHelper.MinecraftStoppingException;
}
