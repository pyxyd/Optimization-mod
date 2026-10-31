#version 150

// TaCZ 质感增强 —— 开镜景深（屏幕外围虚化）
//
// 输入：主渲染目标的世界画面（第一人称枪模是在这个阶段之后才画的，
//       所以枪模永远保持锐利，只有世界会糊）。
// Focus    : 0.0 ~ 1.0，跟着开镜进度走；0 时整条链路等于什么都不做
// Strength : 最大模糊半径（像素）
// Core     : 中心保持锐利的范围（0 中心 ~ 1 角落）
uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform float Focus;
uniform float Strength;
uniform float Core;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 center = texture(DiffuseSampler, texCoord);

    // 到屏幕中心的归一化椭圆距离：0 = 中心，1 = 角落
    vec2 d = (texCoord - vec2(0.5)) * 2.0;
    d.x *= 0.82;                       // 宽屏下横向稍微收一点，看起来更自然
    float r = clamp(length(d) / 1.41421356, 0.0, 1.0);

    // 中心锐利、外围渐糊
    float mask = smoothstep(Core, 1.0, r) * Focus;
    if (mask <= 0.0025) {
        fragColor = center;
        return;
    }

    vec2 texel = 1.0 / max(InSize, vec2(1.0, 1.0));
    float radius = Strength * mask;

    // 12 次采样：6 个方向 x 2 个半径，均匀分布够平了
    vec4 sum = center;
    float weight = 1.0;
    for (int i = 0; i < 6; i++) {
        float ang = float(i) * 1.04719755;         // 60 度步进
        vec2 dir = vec2(cos(ang), sin(ang));
        vec2 off = dir * texel * radius;
        sum += texture(DiffuseSampler, texCoord + off * 0.55);
        sum += texture(DiffuseSampler, texCoord + off);
        weight += 2.0;
    }

    vec4 blurred = sum / weight;
    // 输出保持不透明，后面那一趟 blit 才是最终写回
    fragColor = vec4(mix(center, blurred, mask).rgb, 1.0);
}
