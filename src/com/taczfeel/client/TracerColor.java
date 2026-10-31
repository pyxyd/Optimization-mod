package com.taczfeel.client;

/** 曳光颜色解析：RRGGBB -> float[]{r,g,b}（0~1），留空/写错返回 null 表示"用枪包自己的颜色"。 */
public final class TracerColor {

    private TracerColor() {
    }

    public static float[] parse(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 6) {
            return null;
        }
        try {
            int rgb = Integer.parseInt(s, 16);
            return new float[]{
                    ((rgb >> 16) & 0xFF) / 255.0F,
                    ((rgb >> 8) & 0xFF) / 255.0F,
                    (rgb & 0xFF) / 255.0F
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
