package cn.zbx1425.resourcepackupdater;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class PreLaunchEntryPoint implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        Work.download();
        Work.updateOption();
    }
}
