package com.taczopt.bullethole;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.function.Function;

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
