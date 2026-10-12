package com.taczopt.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.particle.BulletHoleParticle;
import com.taczopt.bullethole.TBHConfig;
import com.taczopt.bullethole.TBHLog;
import com.taczopt.bullethole.TBHTextures;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 两层结构：
 *   1) 方块面上 = TaCZ 原版弹孔，完全不做任何改动（还是它原来的"先亮块、30 tick 内变黑、
 *      按 BulletHoleParticleLife 存活"那一套）；
 *   2) 悬浮在上面 = 你自己的图片，沿方块面法线抬高 logo_lift 格（像皮肤外层），
 *      显示 logo_hold 后开始 logo_fade 淡出，淡完就只剩原版弹孔。
 *
 * 做法：在 TaCZ 的 render（m_5744_）末尾往同一个 buffer 追加一个面片（贴图仍绑定方块图集），
 * 几何自己算：
 *   位置 = 粒子位置 - 相机位置（MC 粒子在相机空间渲染）
 *   朝向 = direction.getRotation()（m_253075_），抬高方向 = 该四元数作用在 (0,1,0) 上的法线
 *   UV 映射与 TaCZ 一致（p0=(u1,v1) p1=(u1,v0) p2=(u0,v0) p3=(u0,v1)），
 *   所以之前调好的 flip_u / flip_v 方向不变。
 *
 * 名称来源（javap 反汇编已安装的 TaCZ 1.1.8-hotfix2 / MC srg jar 确认）：
 *   m_5744_ = render, m_253075_ = Direction.getRotation, m_90583_ = Camera.getPosition
 *   Vec3: m_7096_ = x, m_7098_ = y, m_7094_ = z
 *   Mth.m_14139_ = lerp, LightTexture.m_109885_ = pack
 *   VertexConsumer: m_5483_ = vertex, m_7421_ = uv, m_85950_ = color, m_85969_ = uv2
 */
@Mixin(BulletHoleParticle.class)
public class BulletHoleParticleMixin {

    /** TaCZ 自己的私有字段：弹孔贴在哪个方块面上 */
    @Shadow
    @Final
    private Direction direction;

    @Unique
    private boolean tbhCustom = false;

    /** 是否要改这颗弹孔在方块面上的大小（默认对全部枪生效，和悬浮层列表独立） */
    @Unique
    private boolean tbhResize = false;

    @Inject(
            method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDLnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At("TAIL")
    )
    private void tbhOnInit(ClientLevel level, double x, double y, double z, Direction direction, BlockPos pos,
                           String ammoId, String gunId, String gunDisplayId, CallbackInfo ci) {
        this.tbhResize = TBHConfig.holeApplies(gunId);
        if (this.tbhResize) {
            TBHLog.holeScaled(gunId);
        }
        if (!TBHConfig.applies(gunId)) {
            return;
        }
        if (TBHConfig.logoTicks() <= 0) {
            return;
        }
        if (TBHTextures.spriteOrNull() == null) {
            TBHLog.textureMissing(TBHConfig.texture());
            return;
        }
        this.tbhCustom = true;
        TBHLog.applied(gunId);
    }

    /** 每帧把方块面上原版弹孔的大小设成 hole_scale 倍（TaCZ 原本是 0.05） */
    @Inject(method = "m_5744_", at = @At("HEAD"))
    private void tbhResizeHole(CallbackInfo ci) {
        if (!this.tbhResize) {
            return;
        }
        TBHConfig.reloadIfChanged();
        ((SingleQuadParticleAccessorMixin) (Object) this).setF_107663_(0.05F * TBHConfig.holeScale);
    }

    /** 在 TaCZ 画完原版弹孔之后，追加一层悬浮的 logo 面片 */
    @Inject(method = "m_5744_", at = @At("TAIL"))
    private void tbhDrawLogo(VertexConsumer buffer, Camera camera, float partialTicks, CallbackInfo ci) {
        if (!this.tbhCustom) {
            return;
        }
        TBHConfig.reloadIfChanged();

        ParticleAccessorMixin particle = (ParticleAccessorMixin) (Object) this;
        int age = particle.getF_107224_();
        int hold = Math.max(TBHConfig.logoHold, 0);
        int fade = Math.max(TBHConfig.logoFade, 0);
        int shift = hold + fade;
        if (shift <= 0 || age >= shift) {
            return;
        }
        float alpha = TBHConfig.alpha;
        if (age >= hold) {
            alpha *= fade > 0 ? 1.0F - (age - hold) / (float) fade : 0.0F;
        }
        if (alpha <= 0.002F) {
            return;
        }
        TextureAtlasSprite sprite = TBHTextures.spriteOrNull();
        if (sprite == null || this.direction == null) {
            return;
        }

        Vec3 view = camera.m_90583_();
        float particleX = (float) (Mth.m_14139_(partialTicks, particle.getF_107209_(), particle.getF_107212_()) - view.m_7096_());
        float particleY = (float) (Mth.m_14139_(partialTicks, particle.getF_107210_(), particle.getF_107213_()) - view.m_7098_());
        float particleZ = (float) (Mth.m_14139_(partialTicks, particle.getF_107211_(), particle.getF_107214_()) - view.m_7094_());

        Quaternionf rotation = this.direction.m_253075_();
        Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F).rotate(rotation);
        float lift = TBHConfig.logoLift;
        float half = 0.05F * TBHConfig.scale;

        float u0 = TBHConfig.flipU ? sprite.m_118410_() : sprite.m_118409_();
        float u1 = TBHConfig.flipU ? sprite.m_118409_() : sprite.m_118410_();
        float v0 = TBHConfig.flipV ? sprite.m_118412_() : sprite.m_118411_();
        float v1 = TBHConfig.flipV ? sprite.m_118411_() : sprite.m_118412_();
        int light = LightTexture.m_109885_(15, 15);

        Vector3f[] points = new Vector3f[]{
                new Vector3f(-1.0F, 0.0F, -1.0F),
                new Vector3f(-1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, -1.0F)
        };
        for (int i = 0; i < 4; i++) {
            Vector3f point = points[i];
            point.rotate(rotation);
            point.mul(half);
            point.add(particleX + normal.x() * lift, particleY + normal.y() * lift, particleZ + normal.z() * lift);
        }

        buffer.m_5483_(points[0].x(), points[0].y(), points[0].z()).m_7421_(u1, v1)
                .m_85950_(1.0F, 1.0F, 1.0F, alpha).m_85969_(light).m_5752_();
        buffer.m_5483_(points[1].x(), points[1].y(), points[1].z()).m_7421_(u1, v0)
                .m_85950_(1.0F, 1.0F, 1.0F, alpha).m_85969_(light).m_5752_();
        buffer.m_5483_(points[2].x(), points[2].y(), points[2].z()).m_7421_(u0, v0)
                .m_85950_(1.0F, 1.0F, 1.0F, alpha).m_85969_(light).m_5752_();
        buffer.m_5483_(points[3].x(), points[3].y(), points[3].z()).m_7421_(u0, v1)
                .m_85950_(1.0F, 1.0F, 1.0F, alpha).m_85969_(light).m_5752_();
    }
}