package cn.zbx1425.resourcepackupdater.mixin;

import cn.zbx1425.resourcepackupdater.ResourcePackUpdater;
import cn.zbx1425.resourcepackupdater.gui.GlHelper;
import cn.zbx1425.resourcepackupdater.io.Dispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableResourceManager.class)
public class ReloadableResourceManagerMixin {

    @Inject(at = @At("HEAD"), method = "createReload")
    void createReload(Executor executor, Executor executor2, CompletableFuture<Unit> completableFuture, List<PackResources> list, CallbackInfoReturnable<ReloadInstance> cir) {
        GlHelper.initGlStates();

        Dispatcher syncDispatcher = new Dispatcher();
        ResourcePackUpdater.GL_PROGRESS_SCREEN.reset();
        try {
            syncDispatcher.runSync(ResourcePackUpdater.CONFIG.getPackBaseDir(), ResourcePackUpdater.CONFIG.activeSource, ResourcePackUpdater.GL_PROGRESS_SCREEN);

            if (ResourcePackUpdater.CONFIG.pauseWhenSuccess || ResourcePackUpdater.GL_PROGRESS_SCREEN.hasException()) {
                while (ResourcePackUpdater.GL_PROGRESS_SCREEN.pause(true)) {
                    Thread.sleep(50);
                }
            }
        } catch (Exception ignored) {

        }
        GlHelper.resetGlStates();
    }
}
