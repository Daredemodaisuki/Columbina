# ARC_EXTEND 圆曲线延伸子模式 — 需求与实现计划

## 一、概述

在 Maalaus 绘制框架中新增 `ARC_EXTEND` 子模式，实现**从当前起点沿当前方向角直接开始弯曲的圆曲线**（暂不考虑缓和曲线）。该模式借鉴 `roadrailalignment` 的「大角度圆曲线/回环」的最终绘制效果，但采用不同的交互流程——以圆心参数（半径+左右转）为第一阶段、转角参数为第二阶段的两阶段绘制方式。

> ⚠️ **命名说明**：`MaalausSubMode.java` 中已存在 `ARC_EXTEND` 存根（`requiredPointCount = 1`，描述为旧语义），需将其改为新模式的配置：
> - `requiredPointCount` 从 `1` → `2`（已修改）
> - 描述文本待后续更新
> - 四个抽象方法从返回 `null`/空列表改为委托给新模式的具体实现

---

## 二、需求详述

### 2.1 基本行为

- 从当前起点（S）出发，沿当前方向角（θₜ，假定已存在）开始绘制圆曲线。
- 曲线从起点处**立即弯曲**，不先延伸一段直线。
- 除起点外，需要 **2 个控制点**（以下称 CP1、CP2）。

### 2.2 控制点定义

#### 第一控制点（CP1）—— 圆心

CP1 通过鼠标位置相对于起点方向角的几何关系计算得出：

| 参数 | 确定方式 |
|------|----------|
| **左右转方向** | 鼠标位于当前方向左侧 → 左转（逆时针）；右侧 → 右转（顺时针） |
| **半径（R）** | 将鼠标位置投影到过起点的法线（垂直于方向角的直线，向两侧延伸）上；投影点到起点的距离即为半径 |
| **圆心（C）** | 从起点沿法线向左右转方向走距离 R 的位置 |

CP1 同时也是圆曲线的圆心。

> ⚠️ **投影计算的稳定性**：由于法线方向 n 已根据 turnSign 选择（左法线取左手边，右法线取右手边），投影参数 t = (M-S)·n 应为非负。但浮点误差可能导致 t 为极小负值，实现时需做防御处理（若 t < 0，则取 t = -t 后将圆心置于法线反向，或直接取 R = |t|，C = S + R·n）。

#### 第二控制点（CP2）—— 曲线终点

- 在圆心（CP1）已固定的前提下，鼠标位置与圆心连成一条线（称为**转角线**）。
- 以起点到圆心的连线为基准，转角线与该基准之间的夹角即为**转角（Δθ）**。
- 最终 CP2 即为曲线的终点（以鼠标点击时的位置为准）。

### 2.3 绘制流程（DRAW 状态下）

```
阶段 0：待提交控制点列表为空
  ├─ mouseMoved → 计算鼠标在法线上的投影点，显示法线、投影点位置、半径、左右转
  └─ mouseClicked → 确定 CP1（圆心），加入待提交列表

阶段 1：待提交控制点列表 = [CP1]
  ├─ mouseMoved → 基于已固定的圆心和鼠标位置计算转角线、绘制圆弧预览、显示转角值
  └─ mouseClicked → 确定 CP2（终点），加入待提交列表 → 自动调用 confirmSec()
```

### 2.4 预览要求

| 阶段 | 画布预览内容 |
|------|-------------|
| 确定 CP1 前 | 法线（虚线/半透明）、鼠标在法线上的投影点标记、控制点标记 |
| 确定 CP1 后、CP2 前 | 已固定的法线（或不再显示）、圆心标记、转角线（圆心→鼠标）、从起点到鼠标沿圆曲线的弧段预览 |
| CP2 确定后 | 转为已完成的曲段，以绿色实线绘制 |

### 2.5 信息面板显示（SecInfoPanel）

| 字段 | 含义 | 更新时机 |
|------|------|---------|
| 方向角（Bearing） | 起点方向角（度） | 进入该段后即固定显示 |
| 半径（Radius） | 当前半径（米） | CP1 确定前实时计算并显示；CP1 确定后固定 |
| 左右转（Turn） | 左转（L）/ 右转（R） | CP1 确定前实时判定；CP1 确定后固定 |
| 转角（Turn Angle） | 法线与转角线夹角（度） | CP2 确定前实时计算并显示 |

此外，`CircleCurveSecInfoPanel` 需实现 `SecInfoPanel.requestFieldFocus()`，进入 INFO 状态时将焦点设到第一个可编辑字段（Turn 或 Radius），方便用户直接键盘输入。

#### 部分值更新策略

`CircleCurveDisplayData` 中**允许未知字段为 `Double.NaN`，面板只更新有值的字段**。`updateValues(CircleCurveDisplayData)` 内部实现：

```text
bearingField.setText(formatBearing(d.bearingDeg));                    // 始终更新
if (!Double.isNaN(d.radiusM))        radiusField.setText(formatR(d.radiusM));
if (d.turnSign != 0)                 turnField.setText(d.turnSign > 0 ? "L" : "R");
if (!Double.isNaN(d.turnAngleDeg))   angleField.setText(formatDeg(d.turnAngleDeg));
```

这样无论 `extractDisplayData` 返回的是部分还是完整参数，面板都能正确显示当前可计算的数值。

### 2.6 INFO 状态下参数手动编辑

满足需求：用户可以不通过 DRAW 鼠标交互，直接在 INFO 状态下填写参数来生成控制点。

#### 参数分组规则

| 参数组 | 包含字段 | 该组完整条件 | 对应控制点 |
|--------|---------|-------------|-----------|
| 组1 | `turnSign` + `radius` | 两者均有效（turnSign ≠ 0 且 radius > 0） | CP1（圆心） |
| 组2 | `turnAngle` | 组1完整且 turnAngle 有效 | CP2（终点） |

#### 行为细则

1. **只有组1完整时**：系统可计算并渲染圆心（CP1），但不渲染 CP2。此时用户若切回 DRAW 状态，移动鼠标即进入「阶段1」（选择 CP2）。
2. **组1 + 组2 均完整时**：系统依次计算 CP1、CP2，并在画布上渲染完整预览。此时点击「添加曲段」即可提交。
3. **组1不完整时**：无论组2是否填写，系统不做任何计算和渲染（即先填转角不会先算出 CP2）。
4. 参数从前向后顺序判定：只有前 N 组完整，才能确定前 N 个控制点。

#### `generateAllControlPoints` 返回值策略（实现影响层）

`generateAllControlPoints(startAnchor, data)` 方法内部实现分组完整性检查，返回值策略：

```text
if (turnSign == 0 || radius <= 0)    → return List.of()            // 组1不完整，不返回任何控制点
if (turnAngle 为 NaN/无效)            → return List.of(centerCp)    // 只有组1完整，仅返回CP1
else                                  → return List.of(centerCp, endCp) // 两组完整，返回CP1+CP2
```

上游 `MaalausMapMode.onSecInputChanged` 根据返回值长度决定：
- `size == 0`：不清空/更新待提交控制点
- `size == 1`：设置预览点为 CP1，不触发 confirmSec
- `size == 2`：设置预览点为 CP2，可触发「添加曲段」按钮提交

### 2.7 BackSpace 回退行为

| 当前状态 | 按 BackSpace 的效果 |
|----------|-------------------|
| DRAW，待提交控制点列表非空 | 移除最后一个待提交控制点（若是 CP2 则回到阶段1；若是 CP1 则回到阶段0），同时清除该控制点对应组的参数。回到鼠标选择状态 |
| DRAW，待提交控制点列表为空 | 调用 `undoLastSec()`：移除最近一段已确认曲段，恢复起点和方向角，进入 DRAW 状态 |
| 已经没有曲段可回退 | 不执行任何操作 |

### 2.8 控制点变换策略与数据流

#### 问题背景

`LINE_EXTEND` 的控制点 = 鼠标原始位置，但 `ARC_EXTEND` 的两个控制点都不是鼠标原始位置：

| 阶段 | 鼠标位置 M | 实际控制点 | 变换类型 |
|------|-----------|-----------|---------|
| Phase 0（选 CP1 前） | M | 圆心 C = 投影(M→法线) | 法线投影 |
| Phase 1（选 CP2 前） | M | 弧终点 E = 沿径向到圆 | 圆弧交点 |

因此鼠标→控制点的几何变换**不能**在 `MaalausMapMode`（Controller）中硬编码判断，应委派给子模式层。

#### 解决方案：`calculatePendingControlPoint`

在 `MaalausSubMode` 中新增抽象方法，将「鼠标位置→派生控制点」的变换逻辑放入子模式枚举实现中：

```java
// MaalausSubMode.java 新增
public abstract ColumbinaEN calculatePendingControlPoint(
    ColumbinaEN startAnchor,
    ColumbinaEN startTangent,
    ColumbinaEN mousePos,
    List<ColumbinaEN> currentPendingCPs
);
```

**LINE_EXTEND 实现**（恒等映射，一行）：

```java
LINE_EXTEND(...) {
    @Override
    public ColumbinaEN calculatePendingControlPoint(..., ColumbinaEN mousePos, ...) {
        return mousePos;  // 直线：鼠标位置 = 控制点
    }
}
```

**ARC_EXTEND 实现**（带阶段判断）：

```java
ARC_EXTEND(...) {
    @Override
    public ColumbinaEN calculatePendingControlPoint(..., ColumbinaEN mousePos,
                                                     List<ColumbinaEN> pendingCPs) {
        if (pendingCPs.isEmpty()) {
            // Phase 0：鼠标→法线投影→圆心
            return projectMouseToNormal(startAnchor, startTangent, mousePos).center;
        } else {
            // Phase 1：鼠标沿径向投影到圆上→弧终点
            ColumbinaEN center = pendingCPs.get(0);
            double radius = startAnchor.distance(center);
            return projectMouseToCircle(center, radius, mousePos);
        }
    }
}
```

其中 `projectMouseToNormal` 和 `projectMouseToCircle` 作为 `CircleCurveSec` 的静态工具方法实现。

#### 改造后的整体数据流

```text
mouseMoved(mouseEN)
  │
  ├─► derivedCP = submode.calculatePendingControlPoint(
  │       startAnchor, startTangent, mouseEN, pendingCPs)
  │
  ├─① service.setPreviewPoint(derivedCP)        ← 派生坐标
  │
  ├─② 更新信息面板（去掉 size==required-1 的前提条件）
  │    pendingCPsForUpdate = pendingCPs + [derivedCP]
  │    displayData = submode.extractDisplayData(start, pendingCPsForUpdate)
  │    lastDisplayData = displayData
  │    infoWindow.updateSecInfoValues(displayData)  ← 面板只更新有值的字段
  │
  ├─③ 更新辅助几何预览 + 曲线预览点（法线/圆心/转角线/弧）
  │    renderables = submode.calculatePreviewGeometry(
  │        startAnchor, startTangent, pendingCPs, derivedCP)
  │    previewer.setRenderables(renderables)         ← 由 Renderable 接口统一绘制
  │    previewer.setPreview(curvePoints, controls)   ← 曲线预览点仍用现有通道（可选）
  │
  └─④ mapView.repaint()（由 PCE 自动触发）

mouseClicked(clickEN)
  │
  ├─► derivedCP = submode.calculatePendingControlPoint(
  │       startAnchor, startTangent, clickEN, pendingCPs)
  │
  ├─service.addControlPoint(derivedCP)           ← 派生坐标而非原始鼠标
  ├─service.confirmSec()
  └─service.setPreviewPoint(null)
```

#### 带来的变化

- `MaalausMapMode.mouseMoved` 中现有的条件判断 `pending.size() == required-1` 可以移除 → 改为无条件调用 `extractDisplayData`
- `MaalausMapMode.mouseMoved` 底部的 `service.setPreviewPoint(mouseEN)` 移除 → 改为设置 `derivedCP`
- `MaalausMapMode.mouseClicked` 的 `service.addControlPoint(clickEN)` 移除 → 改为添加 `derivedCP`
- `CircleCurveDisplayData` 允许字段为 `NaN`，面板只更新有值部分

---

## 三、实现计划

### 3.1 新增文件

| 文件 | 职责 | 预估工作量 |
|------|------|-----------|
| `data/columbinaCurveSection/basic/ArcBasicCurveSec.java` | 基本圆曲线段：walk、sample | 小 |
| `data/columbinaCurveSection/CircleCurveSec.java` | 组合曲段 + 静态工具方法（计算显示数据、反算控制点） | 中 |
| `data/dto/modelsDTO/maalaus/CircleCurveDisplayData.java` | 显示数据 DTO：bearingDeg（度）、turnSign（+1左/-1右/0未知）、radiusM（米）、turnAngleDeg（度，NaN=未知）。字段约定：非 NaN 表示该参数当前可计算 | 极小 |
| `modes/maalaus/secInfoPanel/CircleCurveSecInfoPanel.java` | 信息子面板布局与输入回调 | 中 |

### 3.2 需修改的现有文件

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| `MaalausSubMode.java` | 修改已有 `ARC_EXTEND` 存根：`requiredPointCount` 从 `1` → `2`（已修改），实现四个原有抽象方法（`createSecInfoPanel`、`createCurveSec`、`extractDisplayData`、`generateAllControlPoints`），**并新增** `calculatePendingControlPoint` 和 `calculatePreviewGeometry` 抽象方法，为 `LINE_EXTEND` 实现恒等映射（已实现） | ✅ 抽象方法已添加，LINE_EXTEND 已实现，ARC_EXTEND/PI_ARC_EXTEND 已存根 |
| `MaalausMapMode.java` | `refreshPreview()` 分发；移除 `mouseMoved`/`mouseClicked` 中对鼠标坐标直写的代码，改为调用 `submode.calculatePendingControlPoint`；移除信息面板更新条件中的 `pending.size()==required-1` 前提（TODO）；BackSpace 逐级回退逻辑 | ✅ `mouseMoved`/`mouseClicked`/`refreshPreview` 已改用新架构 |
| `Previewer.java` | 新增 `Renderable` 接口 + `RenderableLine`/`RenderablePoint` 记录类 + `setRenderables()` 方法；`paint()` 中新增辅助几何绘制层 | ✅ 已实现 |

### 3.3 预览渲染架构

#### 已提交曲段的渲染（该部分无需改动）

现有架构已天然支持混合渲染：

```text
MaalausMapMode.refreshPreview()
  → AbstractCurveSec.sampleAll(session.getSecs(), 2.0)
       → secs[0].sample(2.0)    // LineBasicCurveSec → 直线等分
       → secs[1].sample(2.0)    // ArcBasicCurveSec  → 圆弧等分  ★新增
       → previewer.setCommittedPoints(allPoints)     ← 多态，全线共用
```

`sampleAll()` 对每段调用 `sec.sample(interval)`，多态分派到具体类型。`ArcBasicCurveSec.sample()` 只需用 `UtilsArc.getCircleArc()` 实现即可。**渲染层零改动**。

#### 当前段预览的渲染（委托给子模式）

当前段的预览包含两种内容，均由子模式 `calculatePreviewGeometry` 计算：

| 内容 | 以前（直线）| 改造后（ARC_EXTEND）|
|------|-----------|-------------------|
| **预览线**（蓝色半透明） | `MaalausMapMode.generateLinePreview()` 硬编码直线 | `calculatePreviewGeometry` 返回的 `RenderableLine`（含圆弧采样点）|
| **辅助几何**（法线/圆心/转角线） | 不存在 | `calculatePreviewGeometry` 返回的 `RenderableLine`/`RenderablePoint` |

`MaalausMapMode.refreshPreview()` 中的对应调整：

```java
// 旧：硬编码直线预览
List<ColumbinaEN> previewPoints = generateLinePreview(start, target);

// 新：委托给子模式（同时获得辅助几何 + 曲线预览点）
List<Previewer.Renderable> geo = submode.calculatePreviewGeometry(
    start, startTangent, pendingCPs, previewTarget);
previewer.setRenderables(geo);
```

#### Previewer 的 `Renderable` 接口

Previewer **不感知子模式类型**，只接收统一的渲染原语列表：

```java
public class Previewer implements MapViewPaintable {

    /** 可渲染原语：子模式计算几何数据，Previewer 统一绘制 */
    public interface Renderable {
        void draw(Graphics2D g, MapView mv);
    }

    /** 线段/折线 */
    public static class RenderableLine implements Renderable {
        public final List<ColumbinaEN> points;
        public final Color color;
        public final float width;
        public final boolean dashed;
        public RenderableLine(...) { ... }
    }

    /** 点标记 */
    public static class RenderablePoint implements Renderable {
        public final ColumbinaEN point;
        public final Color color;
        public final float size;
        public final boolean filled;
        public RenderablePoint(...) { ... }
    }

    // 新增设置方法
    public void setRenderables(List<Renderable> renderables) { ... }
}
```

#### 绘制顺序（paint 方法）

```
1. 已完成曲段（绿色实线）                    ← committedPoints（不变）
2. 辅助几何（法线虚线/圆心标记/转角线）        ← renderables（★新增）
3. 当前段预览线（蓝色半透明）                 ← previewPoints（或整合入 renderables）
4. 起点标记（蓝色圆）                        ← startPoint（不变）
5. 控制点标记（红色圆）                      ← controlPoints（不变）
```

#### ARC_EXTEND 的 `calculatePreviewGeometry` 实现

```java
ARC_EXTEND(...) {
    @Override
    public List<Previewer.Renderable> calculatePreviewGeometry(
            ColumbinaEN start, ColumbinaEN tangent,
            List<ColumbinaEN> pendingCPs, ColumbinaEN previewCP) {

        List<Previewer.Renderable> result = new ArrayList<>();

        if (pendingCPs.isEmpty()) {
            // Phase 0：法线 + 投影点（圆心候选）
            CircleCurveProjection proj = CircleCurveSec.projectMouseToNormal(
                start, tangent, previewCP);
            result.add(new RenderableLine(proj.normalPoints,
                Color.GRAY, 1.5f, true));              // 法线虚线
            result.add(new RenderablePoint(proj.center,
                Color.ORANGE, 8, true));               // 圆心候选
        } else {
            // Phase 1：圆心标记 + 转角线 + 圆弧预览
            ColumbinaEN center = pendingCPs.get(0);
            double radius = start.distance(center);
            List<ColumbinaEN> arcPts = CircleCurveSec.sampleArc(
                center, radius, start, previewCP);
            result.add(new RenderablePoint(center,
                Color.ORANGE, 8, true));               // 已固定圆心
            result.add(new RenderableLine(List.of(center, previewCP),
                new Color(0, 120, 255, 120), 2f, false));  // 转角线
            result.add(new RenderableLine(arcPts,
                new Color(0, 120, 255, 180), 3f, false));  // 圆弧预览
        }

        return result;
    }
}
```

`projectMouseToNormal` 和 `sampleArc` 作为 `CircleCurveSec` 的静态工具方法。


### 3.4 核心算法

#### 3.4.1 法线投影与圆心计算

```text
输入：起点 S、方向角 θₜ（单位向量 T）、鼠标位置 M
输出：圆心 C、半径 R、左右转方向 turnSign ∈ {+1(左), -1(右)}

1. 左右转判定
   cross = Tₓ·(Mᵧ - Sᵧ) - Tᵧ·(Mₓ - Sₓ)
   turnSign = sign(cross)  // 正=左，负=右

2. 法线方向（已根据 turnSign 选择方向）
   n = turnSign > 0 ? (-Tᵧ, Tₓ) : (Tᵧ, -Tₓ)  // 左法线/右法线，已归一化

3. 鼠标到法线的投影参数
   t = (M - S) · n  // n 为单位向量，t ≥ 0 预期

4. 半径与圆心（防御浮点误差）
   R = max(t, 0)                   // t 理论上应 ≥ 0
   C = S + R · n                   // 沿已选定的法线方向走 R 的距离
```

> **解释**：由于 n 已根据 turnSign 选择方向（leftTurn→左法线，rightTurn→右法线），t 应为非负。防御性取 `R = max(t, 0)` 防止极端浮点误差导致圆心反弹到另一侧。

#### 3.4.2 转角与终点计算

```text
输入：圆心 C、起点 S、终点 E（CP2，鼠标点击位置）、turnSign
输出：转角 Δθ

1. 圆心到起点、终点的方向角
   θ_start = atan2(Sᵧ - Cᵧ, Sₓ - Cₓ)
   θ_end   = atan2(Eᵧ - Cᵧ, Eₓ - Cₓ)

2. 计算带符号转角
   Δθ = θ_end - θ_start
   if (turnSign > 0 && Δθ < 0)  Δθ += 2π
   if (turnSign < 0 && Δθ > 0)  Δθ -= 2π
```

#### 3.4.3 圆弧采样

利用现有工具 `UtilsArc.getCircleArc(C, R, θ_start, θ_end, segments, turnSign)` 生成圆弧离散点，其中段数 `segments = max(1, ceil(R · |Δθ| / interval))`。

### 3.5 INFO 参数反算控制点

当用户在 INFO 状态下填写组1参数（turnSign, radius）时：

```text
1. 根据 turnSign 确定法线方向 n（与 3.3.1 一致）
2. 圆心 C = S + R · n（沿法线方向）
3. CP1 = C
```

当组2参数（turnAngle）也填写时：

```text
1. 先按上述方法计算 CP1（C）
2. 计算起点方向角 θ_start = atan2(Sᵧ - Cᵧ, Sₓ - Cₓ)
3. 计算终点方向角 θ_end = θ_start + turnSign · turnAngle_rad
4. 终点 CP2 = C + R · (cos θ_end, sin θ_end)
5. 返回 [CP1, CP2]
```

#### `generateAllControlPoints` 完整实现伪码

```java
public List<ColumbinaEN> generateAllControlPoints(ColumbinaEN start, SecDisplayData data) {
    if (!(data instanceof CircleCurveDisplayData)) return List.of();
    CircleCurveDisplayData d = (CircleCurveDisplayData) data;

    // 检查组1完整性
    if (d.turnSign == 0 || d.radiusM <= 0) return List.of();

    // 计算法线方向和圆心
    ColumbinaEN tangent = new ColumbinaEN(Math.cos(d.bearingRad), Math.sin(d.bearingRad));
    ColumbinaEN normal = d.turnSign > 0
        ? new ColumbinaEN(-tangent.getY(), tangent.getX())   // 左法线
        : new ColumbinaEN(tangent.getY(), -tangent.getX());  // 右法线
    ColumbinaEN center = start.add(normal.mul(d.radiusM));

    // 组2（转角）未填 → 仅返回 CP1
    if (Double.isNaN(d.turnAngleDeg) || d.turnAngleDeg <= 0) return List.of(center);

    // 组2完整 → 计算 CP2
    double startAngle = Math.atan2(start.getY() - center.getY(), start.getX() - center.getX());
    double turnAngleRad = Math.toRadians(d.turnAngleDeg) * d.turnSign;
    double endAngle = startAngle + turnAngleRad;
    ColumbinaEN end = new ColumbinaEN(
        center.getX() + d.radiusM * Math.cos(endAngle),
        center.getY() + d.radiusM * Math.sin(endAngle)
    );

    return List.of(center, end);
}
```

### 3.6 开发顺序

| 步骤 | 内容 | 依赖 | 状态 |
|------|------|------|------|
| 1 | `ArcBasicCurveSec`：实现 walk/sample | — | ❌ 未开始 |
| 2 | `CircleCurveSec`：核心计算逻辑 + 静态工具方法（`projectMouseToNormal`、`sampleArc`、`projectMouseToCircle`） | 1 | ❌ 未开始 |
| 3 | `CircleCurveDisplayData`：DTO + 字段 NaN 约定 | — | ❌ 未开始 |
| 4 | `CircleCurveSecInfoPanel`：信息面板（实现部分值更新） | 3 | ❌ 未开始 |
| 5 | `MaalausSubMode.ARC_EXTEND` 实现 + `LINE_EXTEND.calculatePendingControlPoint` | 1,2,4 | ⚠️ 部分完成：`calculatePendingControlPoint` 和 `calculatePreviewGeometry` 抽象方法已添加，`LINE_EXTEND` 已实现，`ARC_EXTEND`/`PI_ARC_EXTEND` 已存根 |
| 6 | `Previewer`：`Renderable` 接口 + 辅助几何渲染 | — | ✅ 已完成 |
| 7 | `MaalausMapMode`：`mouseMoved`/`mouseClicked`/`refreshPreview` 改用新架构 | 5,6 | ✅ 已完成 |
| 8 | 参数分组管理 + BackSpace 逐级回退 | 7 | ❌ 未开始 |
| 9 | INFO 手动输入计算与渲染 | 7,8 | ❌ 未开始 |
| 10 | 边界测试 | 全部 | ❌ 未开始 |

---

## 四、与现有架构的关系

```
MaalausMapMode (Controller — 简化)
  │  mouseMoved/mouseClicked 不再硬编码鼠标坐标逻辑
  │  ├─委托├──→ submode.calculatePendingControlPoint()  ★新增
  │  └─委托├──→ submode.extractDisplayData()
  │          （无条件调用，接受部分参数）
  │
  ├── MaalausSessionData (不变) ─── MaalausDrawingService (不变)
  │                                          └── confirmSec() → submode.createCurveSec()
  │
  ├── Previewer (扩展：Renderable 通用渲染原语，子模式计算几何、Previewer统一绘制)
  │
  └── MaalausInfoWindow (不变)
        ├── CircleCurveSecInfoPanel (新增，部分值更新)
        └── LineExtendSecInfoPanel (不变)

MaalausSubMode.ARC_EXTEND (Strategy — 扩展)
  ├── createSecInfoPanel() → CircleCurveSecInfoPanel
  ├── createCurveSec() → CircleCurveSec → ArcBasicCurveSec
  ├── extractDisplayData() → CircleCurveDisplayData (允许NaN)
  ├── generateAllControlPoints() → 反算控制点（INFO方向）
  └── calculatePendingControlPoint() → 几何变换（DRAW方向） ★新增
```

现有组件在本次扩展中均无需结构性改动：
- `MaalausState` — 完全复用
- `MaalausSessionData` — 完全复用
- `MaalausDrawingService` — `confirmSec()` 工厂方法机制兼容
- `MaalausInfoWindow` — 子面板由 `SecInfoPanel` 体系自动接管
- `SecInfoPanel` 接口 — 完全复用