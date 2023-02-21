package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.util.RPUClientVersionSupplier;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements RPUClientVersionSupplier {

    private String rpuClientVersion;

    @Override
    public String getRPUClientVersion() {
        return rpuClientVersion;
    }

    @Override
    public void setRPUClientVersion(String version) {
        rpuClientVersion = version;
    }
}
