package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.drm.ServerLockRegistry;
import cn.zbx1425.resourcepackupdater.mappings.Text;
import cn.zbx1425.resourcepackupdater.util.MismatchingVersionException;
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
            if (clientVersion.equals(ResourcePackUpdater.MOD_VERSION) || !ResourcePackUpdater.CONFIG.clientEnforceSameVersion) {
                ((RPUClientVersionSupplier)player).setRPUClientVersion(clientVersion);
            } else {
                disconnect(Text.literal(new MismatchingVersionException(ResourcePackUpdater.MOD_VERSION, clientVersion).getMessage()));
            }
            ci.cancel();
        } else if (identifier.equals(ClientboundCustomPayloadPacket.BRAND)) {
            if (((RPUClientVersionSupplier)player).getRPUClientVersion() == null && ResourcePackUpdater.CONFIG.clientEnforceInstall) {
                disconnect(Text.literal(new MismatchingVersionException(ResourcePackUpdater.MOD_VERSION, "N/A").getMessage()));
                ci.cancel();
            }
        }
    }
}
