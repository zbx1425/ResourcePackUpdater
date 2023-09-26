package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundCustomPayloadPacket;<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)V"))
    private void sendServerLockS2C(Connection connection, ServerPlayer player, CallbackInfo ci) {
        if (!ResourcePackUpdater.CONFIG.serverLockKey.value.isEmpty()) {
            // This will be sent before BRAND.
            FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
            friendlyByteBuf.writeUtf(ResourcePackUpdater.CONFIG.serverLockKey.value);
            connection.send(new ClientboundCustomPayloadPacket(ResourcePackUpdater.SERVER_LOCK_PACKET_ID, friendlyByteBuf));
        }
    }

}
