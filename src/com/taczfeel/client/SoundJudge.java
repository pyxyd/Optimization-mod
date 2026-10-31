package com.taczfeel.client;

import com.taczfeel.FeelConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * 判断一条 TaCZ 音效是不是"枪声"。
 * <p>
 * 主判据是"本地玩家刚刚开火"的时间窗（TaCZ 在 GunFireEvent 之后立刻播 shoot 音效），
 * 备用判据是资源路径里带 shoot / fire / shot 这类词 —— 处理别人开枪、
 * 或者某些枪包把射击音效放在延迟动画通道里播的情况。
 */
public final class SoundJudge {

    private SoundJudge() {
    }

    public static boolean isShot(ResourceLocation id) {
        if (FeelState.isShotWindow()) {
            return true;
        }
        return matchesName(id);
    }

    private static boolean matchesName(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        String extra = FeelConfig.soundNameExtra;
        if (extra == null || extra.isEmpty()) {
            return false;
        }
        // 用 toString 拼出 "命名空间:路径"，避开所有 MC 成员名映射
        String name = id.toString().toLowerCase(Locale.ROOT);
        for (String token : extra.split(",")) {
            String t = token.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty() && name.contains(t)) {
                return true;
            }
        }
        return false;
    }
}
