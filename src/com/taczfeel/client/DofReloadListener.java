package com.taczfeel.client;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * F3+T / 换资源包后，MC 会重建所有 shader program，我们缓存的后期处理链就作废了。
 * 这个 reload listener 只做一件事：把它扔掉，下次开镜时重新构造。
 */
public final class DofReloadListener extends SimplePreparableReloadListener<Void> {

    @Override
    protected Void m_5944_(ResourceManager manager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void m_5787_(Void unused, ResourceManager manager, ProfilerFiller profiler) {
        DofEffect.invalidate();
    }
}
