package com.taczfeel.client;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunRecoil;
import com.tacz.guns.resource.pojo.data.gun.GunRecoilKeyFrame;
import com.taczfeel.FeelConfig;
import com.taczfeel.FeelLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 每把枪的"份量"：把 TaCZ 枪包里的后坐力曲线换算成一个 0.55 ~ 2.2 的倍数，
 * 让 AK 的枪身甩动明显比维克托重，而不是所有枪一个手感。
 * <p>
 * 后坐力曲线是弧度：0.30 rad ≈ 17°，拿它当 1.0 基准。
 */
public final class GunProfiles {

    /** 雷达基准：这个弧度值对应倍数 1.0 */
    private static final float REFERENCE_RAD = 0.30F;
    private static final Map<ResourceLocation, Float> CACHE = new HashMap<>();

    private GunProfiles() {
    }

    public static float recoilScale(ItemStack stack) {
        if (!FeelConfig.kickRecoilScale && !FeelConfig.cameraRecoilScale) {
            return 1.0F;
        }
        if (stack == null || stack.m_41619_()) {
            return 1.0F;
        }
        try {
            IGun gun = IGun.getIGunOrNull(stack);
            if (gun == null) {
                return 1.0F;
            }
            ResourceLocation id = gun.getGunId(stack);
            if (id == null) {
                return 1.0F;
            }
            Float cached = CACHE.get(id);
            if (cached != null) {
                return cached;
            }
            float scale = computeScale(id);
            CACHE.put(id, scale);
            FeelLog.debug("recoil scale " + id + " = " + scale);
            return scale;
        } catch (Throwable t) {
            FeelLog.softError("gun recoil profile", t);
            return 1.0F;
        }
    }

    private static float computeScale(ResourceLocation id) {
        try {
            Optional<ClientGunIndex> index = TimelessAPI.getClientGunIndex(id);
            if (index.isPresent()) {
                GunData data = index.get().getGunData();
                if (data != null) {
                    GunRecoil recoil = data.getRecoil();
                    if (recoil != null) {
                        float rad = firstPitch(recoil);
                        if (rad > 0.0001F) {
                            return clamp(rad / REFERENCE_RAD, 0.55F, 2.2F);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            FeelLog.softError("gun recoil data " + id, t);
        }
        return 1.0F;
    }

    private static float firstPitch(GunRecoil recoil) {
        GunRecoilKeyFrame[] frames = recoil.getPitch();
        if (frames == null || frames.length == 0) {
            return 0F;
        }
        GunRecoilKeyFrame frame = frames[0];
        if (frame == null) {
            return 0F;
        }
        float[] value = frame.getValue();
        if (value == null || value.length == 0) {
            return 0F;
        }
        return Math.abs(value[0]);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    /** 换枪包 / 热重载资源后清缓存（由数据重载事件调用） */
    public static void clear() {
        CACHE.clear();
    }
}
