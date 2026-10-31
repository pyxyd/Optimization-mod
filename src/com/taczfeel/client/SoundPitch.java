package com.taczfeel.client;

import com.taczfeel.FeelLog;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;

import java.lang.reflect.Field;

/**
 * 写音高字段。
 * <p>
 * 为什么用反射而不是 Mixin @Accessor：Accessor 找不到字段时会抛异常，
 * 而这个字段一旦改名就是"启动崩溃"；反射失败只是"音高随机不生效"，
 * 属于可以接受的降级。生产环境是 SRG 名（f_119574_ = pitch），
 * 这里同时保留了几个候选名，谁都匹配不上就放弃并打一条日志。
 */
public final class SoundPitch {

    private static Field field;
    private static boolean resolved = false;

    private SoundPitch() {
    }

    public static void set(Object instance, float pitch) {
        if (instance == null) {
            return;
        }
        Field f = resolve();
        if (f == null) {
            return;
        }
        try {
            f.setFloat(instance, pitch);
        } catch (Throwable t) {
            FeelLog.softError("set pitch", t);
        }
    }

    private static Field resolve() {
        if (resolved) {
            return field;
        }
        resolved = true;
        for (String name : new String[]{"f_119574_", "pitch", "field_119574_"}) {
            try {
                Field f = AbstractSoundInstance.class.getDeclaredField(name);
                if (f.getType() != float.class) {
                    continue;
                }
                f.setAccessible(true);
                field = f;
                FeelLog.info("pitch field resolved: " + name);
                return field;
            } catch (Throwable ignored) {
                // 换下一个候选名
            }
        }
        FeelLog.warn("pitch field not resolved -> gunshot pitch randomization disabled");
        return null;
    }
}
