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

    private static String rawFromConvention(String gunId) {
        int i = gunId.indexOf(':');
        String namespace = i > 0 ? gunId.substring(0, i) : "minecraft";
        String path = i > 0 ? gunId.substring(i + 1) : gunId;
        return translateRaw(namespace + ".gun." + path + ".name");
    }

    private static String rawFromItem(String gunId) {
        Item item = findItem(gunId);
        if (item == null) {
            return null;
        }
        return new ItemStack(item).m_41786_().getString();
    }

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

        }
        ResourceLocation id = parse(gunId);
        return id == null ? null : ForgeRegistries.ITEMS.getValue(id);
    }

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

    @SuppressWarnings({"deprecation", "removal"})
    private static ResourceLocation build(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static ResourceLocation build(String id) {
        return new ResourceLocation(id);
    }
}
