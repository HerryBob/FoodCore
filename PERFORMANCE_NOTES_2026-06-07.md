# FoodCore Performance Notes (2026-06-07)

## 背景

本轮在实际游戏测试和性能分析中，确认 `FoodCore` 当前版本在服务器线程上存在不可忽视的性能开销。

本文件用于记录：

- 当前已确认的性能现象
- 已看到的主要热点链路
- 当前阶段的初步结论
- 后续建议的优化顺序

本次只做记录，不在这一轮继续修改实现。

## 当前现象

- 在集成服务端环境下，`FoodCore` 在性能分析中占据了较明显的服务器线程时间。
- 热点主要集中在容器检测链，而不是食物状态数学本身。
- 当前问题更偏向“每轮扫描中对环境温度的重复重计算”，而不是单个 `ItemStack` 更新逻辑太重。

## 已确认的热点链路

本轮看到的主要热点链如下：

```text
FoodConditionEventHandler.onServerTick()
  -> FoodConditionDetectionScheduler.tick()
    -> FoodConditionStaticContainerScanner.processLoadedStaticContainers()
      -> TemperatureHelper.getTemperature()
        -> TemperatureHelper.getColdSweatTemperature()
          -> Cold Sweat Temperature.apply()
            -> BlockTempModifier.calculate()
```

同时还观察到：

- 原版静态容器扫描是当前最重的主要入口
- Create 静态容器扫描也存在同类问题，但不是这次最重的一段
- `FoodConditionMath` 和 `FoodConditionUpdater` 不是这次的主瓶颈

## 当前结论

### 1. 问题主要来自温度查询成本

当前 `FoodCore` 在处理容器时，需要为每个容器计算环境温度。

而 `TemperatureHelper` 现在直接调用 `Cold Sweat` 的温度链路，这条链会进一步进入多个 modifier 计算。容器数量一多，这部分成本会被线性放大。

因此，这次性能问题的本质更接近：

- `FoodCore` 把 `Cold Sweat` 的温度计算高频批量调用起来了

而不是：

- 食物状态推进公式太慢

### 2. 责任归属应视为 FoodCore 实现策略问题

虽然热点深入到了 `Cold Sweat` 的温度实现内部，但它是由 `FoodCore` 的容器扫描频繁调用触发的。

因此，从模组实现责任来看，这个性能问题应当算在 `FoodCore` 当前扫描策略上，而不是简单归因于 `Cold Sweat` 本身。

### 3. 当前版本不适合在高密度容器环境中长期维持 5 秒全量扫描

尤其是在以下场景中更容易放大问题：

- 大量原版静态容器
- 大量 Create 静态容器
- 大量方块实体同时处于已加载范围
- 同时叠加 `Cold Sweat` 的环境修饰器计算

## 当前不准备在这一轮做的事

以下内容本轮只记录，不继续实现：

- 分批扫描机制
- 航空学结构支持
- 更复杂的任务流水线
- 包裹检测进一步性能特化

原因是当前目标是先把模组整理出来并发布，再在后续版本中做性能优化。

## 后续建议优化顺序

### 第一优先级

#### A. 温度查询前置过滤

尽量在调用 `TemperatureHelper.getTemperature()` 之前，就先完成可跳过目标的过滤。

例如：

- 没有受控食物的容器尽早跳过
- 不需要推进的容器不要先算温度

目标：

- 减少“无效温度查询”

#### B. 同轮扫描温度缓存

同一轮扫描中，附近容器往往环境接近，没有必要为每个容器都完整跑一遍 `Cold Sweat` 温度链。

可考虑的缓存粒度：

- `level + chunk`
- `level + coarse block pos`
- 或其他离散化位置策略

目标：

- 降低 `Cold Sweat Temperature.apply()` 的重复调用次数

### 第二优先级

#### C. 分批扫描

将当前“固定间隔全量扫描”逐步改成：

- 分桶滚动扫描
- 每目标独立计算 `deltaTicks`

目标：

- 降低单次扫描尖峰
- 平滑 MSPT 波动

### 第三优先级

#### D. 温度精度分级或快速模式

可考虑增加配置：

- 高精度模式：完整走 `Cold Sweat` 温度链
- 快速模式：使用近似温度或简化温度来源

目标：

- 让性能敏感服务器可以主动换取更低开销

## 临时缓解建议

如果在当前版本继续游玩，可先通过配置做缓解：

- 提高 `detectionScanIntervalSeconds`
- 在高压场景下先关闭：
  - `enableStaticContainerDetection`
  - `enableCreateStaticContainerDetection`

这不是最终解决方案，但可以在短期内降低压力。

## 下一次继续时的建议入口

下次继续处理这个问题时，建议优先从这些文件入手：

- `src/main/java/com/navip/foodcore/condition/detection/scanner/container/FoodConditionStaticContainerScanner.java`
- `src/main/java/com/navip/foodcore/condition/detection/scanner/container/create/FoodConditionCreateStaticContainerScanner.java`
- `src/main/java/com/navip/foodcore/util/TemperatureHelper.java`
- `src/main/java/com/navip/foodcore/condition/detection/core/FoodConditionDetectionScheduler.java`

建议顺序：

1. 先做温度查询前置过滤
2. 再做温度缓存
3. 最后再评估是否需要分批扫描

## 备注

当前结论基于本轮实机测试与性能分析结果整理。

本文件的目标不是给出最终修复，而是保证后续回来时可以快速恢复上下文，直接继续优化工作。
