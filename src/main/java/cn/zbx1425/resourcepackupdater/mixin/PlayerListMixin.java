package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.drm.ServerLockRegistry;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    void sendServerLockS2C(ServerGamePacketListenerImpl instance, Packet<?> packet) {
        if (packet instanceof ClientboundCustomPayloadPacket
            && ((ClientboundCustomPayloadPacket) packet).getIdentifier().equals(ClientboundCustomPayloadPacket.BRAND)) {
            if (ResourcePackUpdater.CONFIG.serverLockKey != null) {
                FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
                friendlyByteBuf.writeUtf(ResourcePackUpdater.CONFIG.serverLockKey);
                instance.send(new ClientboundCustomPayloadPacket(ResourcePackUpdater.SERVER_LOCK_PACKET_ID, friendlyByteBuf));
            }
        }
        instance.send(packet);
    }
}
