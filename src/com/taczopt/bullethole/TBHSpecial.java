package com.taczopt.bullethole;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.gun.GunItemManager;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动识别"带特殊标签"的枪：看它的名字里有没有 § 颜色码
 * （枪包里这类枪的名字形如 "SCAR-H §c"电玩高手""）。
 *
 * 关键坑：直接拿 ItemStack.getHoverName().getString() 是**拿不到 § 的**，
 * 因为 MC 在取文本时会先把 § 解析成样式并把这些字符丢掉。所以这里去取"原始翻译字符串"：
 *   1) TaCZ 的枪索引里存着翻译键（ClientGunIndex.getName()），再用 I18n.get(key) 取原始值
 *   2) 退化：按 TaCZ 约定拼 "<命名空间>.gun.<路径>.name" 再 I18n.get
 *   3) 再退化：物品显示名（可能已被剥掉 §，聊胜于无）
 * 三者任一含 § 即判定为"带标签"。结果按 gunId 缓存，配置重载时清空。
 *
 * 纯客户端判定（依赖当前语言的翻译），服务端不会有影响。
 */
public final class TBHSpecial {

    private static final char SECTION = '\u00a7';
    private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();

    private TBHSpecial() {
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static boolean isSpecial(String gunId) {
        if (gunId == null || gunId.isEmpty()) {
            return false;
        }
        Boolean cached = CACHE.get(gunId);
        if (cached != null) {
            return cached;
        }
        String fromIndex = null;
        String fromConvention = null;
        String fromItem = null;
        boolean result = false;
        try {
            fromIndex = rawFromIndex(gunId);
            fromConvention = rawFromConvention(gunId);
            fromItem = rawFromItem(gunId);
            result = hasSection(fromIndex) || hasSection(fromConvention) || hasSection(fromItem);
        } catch (Throwable ignored) {
            result = false;
        }
        TBHLog.autoCheck(gunId, fromIndex, fromConvention, fromItem, result);
        CACHE.put(gunId, result);
        return result;
    }

    /** 仅用于日志：拿到的原始名字（含 §，可能为 null） */
    public static String displayName(String gunId) {
        if (gunId == null) {
            return null;
        }
        try {
            String s = rawFromIndex(gunId);
            if (s == null) {
                s = rawFromConvention(gunId);
            }
            if (s == null) {
                s = rawFromItem(gunId);
            }
            return s;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasSection(String s) {
        return s != null && s.indexOf(SECTION) >= 0;
    }

    /** 1) 从 TaCZ 枪索引拿翻译键，再取原始翻译（带 §） */
    private static String rawFromIndex(String gunId) {
        ResourceLocation id = parse(gunId);
        if (id == null) {
            return null;
        }
        Optional<ClientGunIndex> index = TimelessAPI.getClientGunIndex(id);
        if (index.isEmpty()) {
            return null;
        }
        String key = index.get().getName();
        return translateRaw(key);
    }

    /** 2) 按约定拼键 */
    private static String rawFromConvention(String gunId) {
        int i = gunId.indexOf(':');
        String namespace = i > 0 ? gunId.substring(0, i) : "minecraft";
        String path = i > 0 ? gunId.substring(i + 1) : gunId;
        return translateRaw(namespace + ".gun." + path + ".name");
    }

    /** 3) 物品显示名（MC 可能已把 § 转成样式，所以只作兜底） */
    private static String rawFromItem(String gunId) {
        Item item = findItem(gunId);
        if (item == null) {
            return null;
        }
        return new ItemStack(item).m_41786_().getString();
    }

    /** 用 I18n 取原始翻译字符串；键不存在时 I18n 会原样返回键，这里判掉 */
    private static String translateRaw(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String value = I18n.m_118938_(key);
        if (value == null || value.equals(key)) {
            return null;
        }
        return value;
    }

    private static Item findItem(String gunId) {
        try {
            RegistryObject<?> object = GunItemManager.getGunItemRegistryObject(gunId);
            if (object != null && object.isPresent()) {
                Object value = object.get();
                if (value instanceof Item) {
                    return (Item) value;
                }
            }
        } catch (Throwable ignored) {
            // 继续用注册表兜底
        }
        ResourceLocation id = parse(gunId);
        return id == null ? null : ForgeRegistries.ITEMS.getValue(id);
    }

    /**
     * 把 {@code namespace:path} 形式的枪械 id 解析成 ResourceLocation，非法返回 null。
     *
     * <p>⚠ 必须用 {@code new ResourceLocation(ns, path)}，而它在 1.20.1 标了
     * {@code @Deprecated(forRemoval = true)}。试过 {@code ResourceLocation.tryParse}
     * —— <b>这个方法在 1.20.1 不存在</b>，javac 直接"找不到符号"。所以保留旧构造器
     * 并抑制警告：不抑制的话 javac 会写 stderr，而 build.ps1 把"javac 的 stderr
     * 非空"当成构建失败信号（详见其中 Invoke-Native 的注释）。
     * 命名空间仍然只取第一个冒号之前的部分，行为和以前一致；
     * 非法输入仍然由下面的 catch 转成 null。
     */
    private static ResourceLocation parse(String gunId) {
        try {
            int i = gunId.indexOf(':');
            return i > 0
                    ? build(gunId.substring(0, i), gunId.substring(i + 1))
                    : build(gunId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 单独一层只为挂 {@code @SuppressWarnings}（原因见 {@link #parse}）。
     *
     * <p>必须同时写 {@code "removal"}：1.20.1 把这两个构造器标成
     * {@code @Deprecated(forRemoval = true)}，javac 报的是 {@code [removal]} 类别，
     * {@code "deprecation"} 抑制不了它。
     */
    @SuppressWarnings({"deprecation", "removal"})
    private static ResourceLocation build(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static ResourceLocation build(String id) {
        return new ResourceLocation(id);
    }
}