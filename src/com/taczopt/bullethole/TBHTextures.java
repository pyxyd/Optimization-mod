package com.taczopt.bullethole;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.function.Function;

/**
 * 取自定义弹孔贴图在方块图集里的 sprite。
 * 每次实时查表（不缓存），这样资源包 F3+T 热重载后立刻生效；
 * 贴图不存在时返回 null（调用方回退到 TaCZ 原版弹孔），避免画成紫黑"丢失贴图"方块。
 */
public final class TBHTextures {

    private TBHTextures() {
    }

    public static TextureAtlasSprite spriteOrNull() {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft == null) {
            return null;
        }
        Function<ResourceLocation, TextureAtlasSprite> atlas = minecraft.m_91258_(InventoryMenu.f_39692_);
        TextureAtlasSprite missing = atlas.apply(MissingTextureAtlasSprite.m_118071_());
        TextureAtlasSprite sprite = atlas.apply(TBHConfig.texture());
        return sprite == missing ? null : sprite;
    }
}
