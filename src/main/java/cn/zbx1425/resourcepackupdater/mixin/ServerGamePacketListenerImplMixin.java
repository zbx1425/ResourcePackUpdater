package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.mappings.Text;
import cn.zbx1425.resourcepackupdater.util.MismatchingVersionException;
import cn.zbx1425.resourcepackupdater.util.MtrVersion;
import cn.zbx1425.resourcepackupdater.util.RPUClientVersionSupplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;

    @Shadow public abstract void disconnect(Component textComponent);

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    void handleCustomPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        ResourceLocation identifier = packet.getIdentifier();
        if (identifier.equals(ResourcePackUpdater.CLIENT_VERSION_PACKET_ID)) {
            // This will arrive before BRAND.
            FriendlyByteBuf friendlyByteBuf = packet.getData();
            String clientVersion = friendlyByteBuf.readUtf();
            if (!ResourcePackUpdater.CONFIG.clientEnforceVersion.value.isEmpty()) {
                String versionCriteria = ResourcePackUpdater.CONFIG.clientEnforceVersion.value.replace("current", ResourcePackUpdater.MOD_VERSION);
                if (MtrVersion.parse(clientVersion).matches(versionCriteria)) {
                    ((RPUClientVersionSupplier)player).setRPUClientVersion(clientVersion);
                } else {
                    disconnect(Text.literal(new MismatchingVersionException(ResourcePackUpdater.MOD_VERSION, clientVersion).getMessage().trim()));
                }
            } else {
                ((RPUClientVersionSupplier)player).setRPUClientVersion(clientVersion);
            }
            ci.cancel();
        } else if (identifier.equals(ClientboundCustomPayloadPacket.BRAND)) {
            if (((RPUClientVersionSupplier)player).getRPUClientVersion() == null && ResourcePackUpdater.CONFIG.clientEnforceInstall.value) {
                disconnect(Text.literal(new MismatchingVersionException(ResourcePackUpdater.MOD_VERSION, "N/A").getMessage().trim()));
                ci.cancel();
            }
        }
    }
}
