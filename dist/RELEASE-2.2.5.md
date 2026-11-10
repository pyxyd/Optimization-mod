# Optimization for Tacz + Feel 2.2.5

把上下拖动的方向翻过来：现在**往上拖 = 枪口抬高**。

2.2.4 换成 Z 轴做俯仰后方向反了（往上拖枪口往下），这里把 pitch 的累积符号取负。
`refitdrag.invert_y` 仍然保留，想反回来就设 true。

## 可调
- `refitdrag.sensitivity=0.55` 每像素度数
- `refitdrag.pitch_limit=85` 俯仰上限
- `refitdrag.invert_x` / `invert_y` 方向反转
按下鼠标即重载配置，改完不用重开界面。