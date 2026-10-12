package com.taczopt.util;

import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 从物品里读出 TaCZ 的枪械 id。
 *
 * <p>注意：枪的 id <b>不等于</b> 物品的注册名 —— 一把 {@code tacz:modern_kinetic_gun}
 * 物品可以通过 NBT 变成任意一把枪，所以必须走 {@link IGun#getGunId}。
 */
public final class GunIds {

    private GunIds() {
    }

    /** @return 例如 {@code tacz:ak47}；不是枪就返回 null */
    public static String of(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        try {
            IGun gun = IGun.getIGunOrNull(stack);
            if (gun == null) {
                return null;
            }
            ResourceLocation id = gun.getGunId(stack);
            return id == null ? null : id.toString();
        } catch (Throwable t) {
            // 枪包没加载完之类的情况下不要因为解析 id 把渲染搞崩
            return null;
        }
    }

    /** 从附件的物品里读出配件 id（例如 {@code tacz:scope_standard_8x}） */
    public static String attachmentOf(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        try {
            com.tacz.guns.api.item.IAttachment att =
                    com.tacz.guns.api.item.IAttachment.getIAttachmentOrNull(stack);
            if (att == null) {
                return null;
            }
            ResourceLocation id = att.getAttachmentId(stack);
            return id == null ? null : id.toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
