package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.drm.ServerLockRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundCustomPayloadPacket.class)
public class ClientboundCustomPayloadPacketMixin {

    @Shadow @Final private ResourceLocation identifier;
    @Shadow @Final private FriendlyByteBuf data;

    @Inject(method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V", at = @At("HEAD"), cancellable = true)
    void handle(ClientGamePacketListener handler, CallbackInfo ci) {
        if (identifier.equals(ServerLockRegistry.SERVER_LOCK_PACKET_ID)) {
            ServerLockRegistry.currentServerLock = data.readUtf();
            ci.cancel();
        } else if (identifier.equals(ClientboundCustomPayloadPacket.BRAND)) {
            ServerLockRegistry.currentServerLock = null;
        }
    }
}
