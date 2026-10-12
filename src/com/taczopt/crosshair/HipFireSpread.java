package com.taczopt.crosshair;

/**
 * 腰射扩散：<b>照搬 TaCZ 原版算法</b>，不做任何近似。
 *
 * <p>不引用游戏类，判定所需的输入（瞄准进度 / 是否游泳 / 姿态 / 是否移动）
 * 由调用方查好传进来，方便离线单测。
 *
 * <h2>算法出处</h2>
 * {@code com.tacz.guns.item.ModernKineticGunScriptAPI}（枪械开火时算扩散那段）
 * 与 {@code com.tacz.guns.resource.pojo.data.gun.InaccuracyType}。
 * 反编译出来的原始逻辑：
 * <pre>
 *   float mul = 1.0f;                                     // 过热系数
 *   if (hasHeatData()) {
 *       float ratio = getHeatAmount() / modifyProperty("max_heat", heat.getHeatMax());
 *       mul = Mth.lerp(ratio, heat.getMinInaccuracy(), heat.getMaxInaccuracy());
 *   }
 *   InaccuracyType type = InaccuracyType.getInaccuracyType(shooter);
 *   float base   = cache.getCache(GunProperties.INACCURACY).get(type);
 *   float spread = base * mul;
 *   spread = Math.max(0, modifyProperty(GunProperties.INACCURACY, spread));
 * </pre>
 *
 * <h2>档位判定（{@code InaccuracyType.getInaccuracyType}）</h2>
 * <b>是离散的，不是插值</b> —— 早先本模组按移动速度在 stand/move 之间插值，
 * 那是自创的近似，和 TaCZ 实际打出去的扩散对不上。原版顺序：
 * <ol>
 *   <li>{@code getSynAimingProgress() == 1.0} -> AIM（开镜）</li>
 *   <li>{@code isSwimming() || pose == SWIMMING} -> LIE（趴/游泳）</li>
 *   <li>{@code pose == CROUCHING} -> SNEAK（蹲）</li>
 *   <li>{@code isMove(实体)} -> MOVE（移动）</li>
 *   <li>否则 -> STAND（站定）</li>
 * </ol>
 *
 * <h2>isMove 的判定</h2>
 * <pre>
 *   double d = Math.abs(entity.f_19787_ - entity.f_19867_);   // 腿部摆动差
 *   if (entity instanceof Player p) {
 *       d = HitboxHelper.getPlayerVelocity(p).length();       // 玩家改用实际速度
 *   }
 *   return d &gt; 0.05;
 * </pre>
 */
public final class HipFireSpread {

    private HipFireSpread() {
    }

    /** 对应 TaCZ 的 {@code InaccuracyType} */
    public enum Stance {
        STAND, MOVE, SNEAK, LIE, AIM
    }

    /**
     * 档位判定 —— 与 {@code InaccuracyType.getInaccuracyType} 逐条一致。
     *
     * <p>⚠ 顺序不能改：AIM 优先于一切，LIE 优先于 SNEAK，SNEAK 优先于 MOVE。
     * 原版就是这么排的，顺序错了蹲着走会算成 MOVE。
     *
     * @param aimingProgress TaCZ 的 {@code getSynAimingProgress()}，0~1
     * @param swimming       {@code isSwimming() || getPose() == SWIMMING}
     * @param crouching      {@code getPose() == CROUCHING}
     * @param moving         {@code isMove(实体)} 的结果
     */
    public static Stance stanceFor(float aimingProgress, boolean swimming,
                                   boolean crouching, boolean moving) {
        if (aimingProgress == 1.0f) {
            return Stance.AIM;
        }
        if (swimming) {
            return Stance.LIE;
        }
        if (crouching) {
            return Stance.SNEAK;
        }
        if (moving) {
            return Stance.MOVE;
        }
        return Stance.STAND;
    }

    /**
     * {@code isMove} 的判定：腿部摆动差或玩家实际速度 &gt; 0.05。
     *
     * <p>⚠ 原版对腿部摆动差取了绝对值（{@code Math.abs(f_19787_ - f_19867_)}），
     * 这里也取 —— 摆动差是可能为负的，不取绝对值会出现"腿往后摆时不判定为移动"，
     * 表现为走路时准星时张时收。离线校验抓着过这个。
     *
     * @param limbSwingDiff {@code entity.f_19787_ - entity.f_19867_}（可以为负，内部取绝对值）
     * @param playerSpeed   玩家用 {@code HitboxHelper.getPlayerVelocity(p).length()}，
     *                      非玩家传 {@code NaN} 表示"不是玩家"
     */
    public static boolean isMove(double limbSwingDiff, double playerSpeed) {
        double d = Double.isNaN(playerSpeed) ? Math.abs(limbSwingDiff) : playerSpeed;
        return d > 0.05;
    }

    /** TaCZ 的 {@code Mth.lerp(delta, start, end)} */
    public static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    /**
     * 过热系数：没过热数据就是 1.0，否则在 min/max 之间按热量比例插值。
     *
     * @param hasHeatData   枪有没有过热数据
     * @param heatAmount    TaCZ 的 {@code getHeatAmount()}
     * @param maxHeat       {@code modifyProperty("max_heat", heat.getHeatMax())}
     * @param minInaccuracy {@code heat.getMinInaccuracy()}
     * @param maxInaccuracy {@code heat.getMaxInaccuracy()}
     */
    public static float heatMultiplier(boolean hasHeatData, float heatAmount, float maxHeat,
                                       float minInaccuracy, float maxInaccuracy) {
        if (!hasHeatData) {
            return 1.0f;
        }
        float ratio = maxHeat == 0f ? 0f : heatAmount / maxHeat;
        return lerp(ratio, minInaccuracy, maxInaccuracy);
    }

    /**
     * 最终扩散 = 档位基础值 * 过热系数，且不小于 0。
     *
     * @param baseInaccuracy 附件缓存里该档位的值（{@code GunProperties.INACCURACY} 那张表）
     * @param heatMultiplier {@link #heatMultiplier}
     */
    public static float spread(float baseInaccuracy, float heatMultiplier) {
        return Math.max(0f, baseInaccuracy * heatMultiplier);
    }

    /**
     * TaCZ 档位没有该条目时的兜底值 —— 取自
     * {@code InaccuracyType.getDefaultInaccuracy()} 的真实常量。
     */
    public static float defaultFor(Stance stance) {
        switch (stance) {
            case MOVE:
                return 5.75f;
            case SNEAK:
                return 3.5f;
            case LIE:
                return 2.5f;
            case AIM:
                return 0.1f;
            case STAND:
            default:
                return 5.0f;
        }
    }
}
