# FoodCore

FoodCore is a NeoForge 1.21.1 food spoilage and preservation mod built around
Cold Sweat temperature data and Create storage compatibility.


# Note: This mod was developed with AI assistance.
# 注意：本模组在开发过程中使用了 AI 辅助。
## 概述

FoodCore 为食物提供持续变化的状态系统：

- 高温环境下逐步腐败
- 低温环境下逐步冻结
- 解冻后继续回到正常状态推进
- 容器内可通过保鲜物品降低或停止变化速度

当前版本已经把食物状态推进从玩家背包扩展到了常见容器和部分模组载体，适合直接用于整合包或生存玩法测试。

## 已实现内容

- 玩家背包食物状态推进
- 掉落物食物状态推进
- 原版静态容器检测
  - 箱子
  - 木桶
  - 潜影盒
- 原版实体容器检测
  - 箱船
  - 矿车箱
  - 驴/骡箱子
- Create 静态 capability 容器检测
- Create 动态结构 mounted storage 检测
- Create 包裹内部食物递归检测
- 三种保鲜物品
  - 保鲜内衬
  - 冷藏模块
  - 静滞核心
- Tooltip 状态显示
- 冻结食物的 Cold Sweat 联动食用效果
- 一组可控制的全局配置项

## 当前兼容重点

- Minecraft `1.21.1`
- NeoForge `21.1.219`
- Create `6.0.10+`
- Cold Sweat `2.4.1+`

当前版本将 Create 和 Cold Sweat 设为强依赖，缺少任一模组时不会加载。

## 容器与结构支持范围

当前版本已经接通的主要载体：

- 玩家背包
- 末影箱检测链中的无环境处理逻辑
- 掉落物
- 原版静态容器
- 原版实体容器
- Create 静态容器
- Create 动态结构与列车 mounted storage
- Create 包裹在上述载体中的内部内容

## 全局配置

`foodcore-common.toml` 当前支持以下主要配置：

- `enableFoodConditionSystem`
- `detectionScanIntervalSeconds`
- `enablePlayerInventoryDetection`
- `enableStaticContainerDetection`
- `enableEntityContainerDetection`
- `enableCreateStaticContainerDetection`
- `enableCreateContraptionDetection`
- `enableItemEntityDetection`
- `enablePackageProcessing`
- `enableFoodConditionTooltip`
- `enableColdSweatConsumptionEffect`
- `enableStackingMerge`
- `stackingMergeIntervalSeconds`
- `freezingReductionMax`

默认配置以“功能全开”为目标，便于直接体验完整功能。

## 卸载说明

可以移除本模组，但建议先执行以下步骤：

1. 备份存档
2. 清理 FoodCore 自带物品
   - 腐败食物
   - 保鲜内衬
   - 冷藏模块
   - 静滞核心
3. 检查玩家背包、容器、Create 保险库、动态结构和包裹内部是否仍存有这些物品
4. 再移除模组并测试存档

如果仅保留了被写入食物状态数据的原版食物，通常风险较低；真正的卸载风险主要来自已注册物品缺失。

## 已知限制

- 掉落物检测仍是较旧的全量扫描实现，后续会继续优化
- 静态容器和实体容器的自动合并目前尚未接入
- 更细粒度的分批扫描机制尚未落地
- 航空学物理结构兼容尚未开始

## 开发与构建

构建命令：

```bash
./gradlew build
```

构建产物默认位于：

```text
build/libs/foodcore-<version>.jar
```

当前发布版本产物：

```text
build/libs/foodcore-0.1.0.jar
```
