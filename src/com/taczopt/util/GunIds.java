package com.taczopt.util;

import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class GunIds {

    private GunIds() {
    }

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

            return null;
        }
    }

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
