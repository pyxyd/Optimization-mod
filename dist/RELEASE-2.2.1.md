# Optimization for Tacz + Feel 2.2.1

修复 2.2.0 的拖动旋转**完全不生效**。

## 原因
2.2.0 用 `@Mixin(Screen.class)` 注入 `mouseClicked` / `mouseDragged` / `mouseReleased`。
但 `Screen` **没有声明这三个方法**——它们是 `GuiEventListener` 的默认方法，而 `AbstractContainerEventHandler`
也没有实现，所以注入点根本不存在。mixin 配置里 `defaultRequire: 0`，注入失败**完全静默**，
表现就是"按了没反应、日志也没有任何错误"。

## 改法
鼠标捕获改用 Forge 事件，不再依赖 mixin 注入点：

- `RefitDragHandler`（`@Mod.EventBusSubscriber`，FORGE / CLIENT）
  - `InputEvent.MouseButton.Post`：改装界面里左键按下开始拖动、松开结束
  - `ScreenEvent.Render.Pre`：每帧拿到光标坐标（`getMouseX/getMouseY`），累计位移
- 旋转仍由 `FirstPersonGunTransformMixin` 在 `applyFirstPersonGunTransform` 的 TAIL 施加
- `GunRefitScreenInitMixin` 保留（`GunRefitScreen.m_7856_` 确实存在），负责重载配置与复位

另外加了"移动超过 3 像素才开始转"，避免点按钮/槽位时枪被蹭歪。

## 日志自检
打开改装界面应看到：
```
[taczopt] refit screen opened, drag=true (yaw=0 pitch=0 dragging=false)
```
在界面里按一次左键应看到：
```
[taczopt] refit drag active: sensitivity=0.55 pitch_limit=85
```
两条都有但不转 → 是渲染阶段的注入问题；一条都没有 → 事件没进来。