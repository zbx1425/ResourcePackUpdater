package cn.zbx1425.resourcepackupdater.io;

import cn.zbx1425.resourcepackupdater.gui.GlHelper;

public interface ProgressReceiver {

    void printLog(String line) throws GlHelper.MinecraftStoppingException;
    void amendLastLog(String postfix) throws GlHelper.MinecraftStoppingException;
    void setProgress(float primary, float secondary) throws GlHelper.MinecraftStoppingException;
    void setSecondaryProgress(float value, String textValue) throws GlHelper.MinecraftStoppingException;
    void setException(Exception exception) throws GlHelper.MinecraftStoppingException;
}