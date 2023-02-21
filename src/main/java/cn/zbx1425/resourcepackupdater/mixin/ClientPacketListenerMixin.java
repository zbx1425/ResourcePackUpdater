package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.drm.ServerLockRegistry;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Shadow @Final private Connection connection;

    @Inject(method = "handleLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundCustomPayloadPacket;<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)V"))
    void handleLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        // This will be sent before BRAND.
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
        friendlyByteBuf.writeUtf(ResourcePackUpdater.MOD_VERSION);
        ServerLockRegistry.onLoginInitiated();
        connection.send(new ServerboundCustomPayloadPacket(ResourcePackUpdater.CLIENT_VERSION_PACKET_ID, friendlyByteBuf));
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    void handleCustomPayload(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        ResourceLocation identifier = packet.getIdentifier();
        if (identifier.equals(ResourcePackUpdater.SERVER_LOCK_PACKET_ID)) {
            // This will arrive before BRAND.
            FriendlyByteBuf friendlyByteBuf = packet.getData();
            try {
                ServerLockRegistry.onSetServerLock(friendlyByteBuf.readUtf());
            } finally {
                friendlyByteBuf.release();
            }
            ci.cancel();
        } else if (identifier.equals(ClientboundCustomPayloadPacket.BRAND)) {
            ServerLockRegistry.onAfterSetServerLock();
        }
    }
}
