package cn.zbx1425.resourcepackupdater.util;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;

public class MismatchingVersionException extends Exception {

    public MismatchingVersionException(String requested, String current) {
        super("\n\n" +
            String.format("資源同步實用程式版本不合: 請您去下載安裝 %s 版 (您現有 %s)", requested, current) + "\n" +
            String.format("Please update your Resource Pack Updater mod to the version %s (You are now using %s)", requested, current) +
            "\n\n"
        );
    }

    public MismatchingVersionException(String message) {
        super(message);
    }
}
