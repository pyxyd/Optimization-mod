package com.taczfeel.client;

import com.tacz.guns.client.sound.GunSoundInstance;
import com.taczfeel.FeelConfig;
import com.taczfeel.FeelLog;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 枪声"尾巴"：把刚播的那条枪声复制一份，降调降音量、延后几十毫秒再放一次。
 * 用的是 TaCZ 自己的 GunSoundInstance，所以枪包里的自定义 .ogg 一样能复读（不用额外音频文件）。
 */
public final class TailSounds {

    private static final List<Entry> QUEUE = new ArrayList<>();
    private static final int MAX_QUEUE = 48;

    /** 我们自己在造尾音实例时置位，防止 ctor 混入再次排队（递归） */
    private static boolean building = false;

    private TailSounds() {
    }

    public static boolean isBuilding() {
        return building;
    }

    /**
     * 由 GunSoundInstance 的构造函数混入调用：每播一次"枪声"就排一条尾音。
     * 参数就是那条枪声的真实参数，尾音在它的基础上变频变轻。
     */
    public static void onShotSound(SoundEvent sound, SoundSource source, float volume, float pitch,
                                   Entity entity, int distance, ResourceLocation registryName, boolean flag) {
        if (!FeelConfig.soundEnabled || !FeelConfig.soundTail) {
            return;
        }
        if (sound == null || entity == null || registryName == null) {
            return;
        }
        int count = FeelConfig.soundTailCount;
        if (count <= 0) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        boolean self = mc != null && mc.f_91074_ == entity;

        long now = System.nanoTime();
        float baseVolume = volume * FeelConfig.soundTailVolume * (self ? 1.0F : 1.2F);
        for (int i = 1; i <= count; i++) {
            long due = now + (long) FeelConfig.soundTailDelayMs * i * 1_000_000L;
            float vol = baseVolume * (1.0F - 0.3F * (i - 1));
            float p = pitch * FeelConfig.soundTailPitch * (1.0F - 0.06F * (i - 1));
            if (p < 0.5F) {
                p = 0.5F;
            }
            if (p > 2.0F) {
                p = 2.0F;
            }
            if (vol <= 0.001F) {
                continue;
            }
            // 尾音给更大的传播半径，做出"远处还有回响"的感觉
            QUEUE.add(new Entry(due, sound, source, vol, p, entity, distance + 24, registryName, flag));
        }
        while (QUEUE.size() > MAX_QUEUE) {
            QUEUE.remove(0);
        }
        FeelLog.debug("tail queued: " + registryName + " x" + count + " vol=" + baseVolume);
    }

    /** 每 tick 处理到期的尾音 */
    public static void tick() {
        if (QUEUE.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        Iterator<Entry> it = QUEUE.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (now < e.dueNanos) {
                continue;
            }
            it.remove();
            play(e);
        }
    }

    public static void clear() {
        QUEUE.clear();
    }

    private static void play(Entry e) {
        building = true;
        try {
            GunSoundInstance instance = new GunSoundInstance(e.sound, e.source, e.volume, e.pitch,
                    e.entity, e.distance, e.registryName, e.flag);
            Minecraft mc = Minecraft.m_91087_();
            if (mc != null) {
                mc.m_91106_().m_120399_(instance);
            }
        } catch (Throwable t) {
            FeelLog.softError("tail sound", t);
        } finally {
            building = false;
        }
    }

    private static final class Entry {
        final long dueNanos;
        final SoundEvent sound;
        final SoundSource source;
        final float volume;
        final float pitch;
        final Entity entity;
        final int distance;
        final ResourceLocation registryName;
        final boolean flag;

        Entry(long dueNanos, SoundEvent sound, SoundSource source, float volume, float pitch,
              Entity entity, int distance, ResourceLocation registryName, boolean flag) {
            this.dueNanos = dueNanos;
            this.sound = sound;
            this.source = source;
            this.volume = volume;
            this.pitch = pitch;
            this.entity = entity;
            this.distance = distance;
            this.registryName = registryName;
            this.flag = flag;
        }
    }
}
