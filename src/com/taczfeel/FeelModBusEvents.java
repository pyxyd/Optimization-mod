package com.taczfeel;

import com.taczfeel.client.DofReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** MOD 总线上的注册：给景深加一个资源重载监听（F3+T 后重建后期处理链）。 */
@Mod.EventBusSubscriber(modid = FeelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FeelModBusEvents {

    private FeelModBusEvents() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new DofReloadListener());
        FeelLog.debug("registered dof reload listener");
    }
}
