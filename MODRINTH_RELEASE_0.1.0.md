# FoodCore 0.1.0

## 简介

FoodCore 是一个面向 NeoForge 1.21.1 的食物状态模组。

它基于 Cold Sweat 的环境温度系统，让食物会因为环境温度逐步腐败、冻结和解冻，并将这一套逻辑扩展到玩家背包、原版容器以及 Create 容器与动态结构中。

## 本版本内容

- 完成食物状态推进主链
- 支持玩家背包食物状态推进
- 支持掉落物食物状态推进
- 支持原版静态容器
- 支持原版实体容器
- 支持 Create 静态 capability 容器
- 支持 Create 动态结构与列车 mounted storage
- 支持 Create 包裹内部食物递归检测
- 加入保鲜内衬、冷藏模块、静滞核心三种保鲜物品
- 加入 Tooltip 状态显示
- 加入冻结食物的 Cold Sweat 联动食用效果
- 加入一组可控的全局配置项

## 依赖

- Minecraft `1.21.1`
- NeoForge `21.1.219`
- Create `6.0.10+`
- Cold Sweat `2.4.1+`

当前版本将 Create 和 Cold Sweat 设为强依赖。

## 主要配置项

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

## 已知限制

- 掉落物检测仍有后续优化空间
- 静态容器和实体容器自动合并尚未接入
- 分批扫描机制尚未落地
- 航空学物理结构兼容尚未开始

## 卸载提示

移除模组前，建议先备份存档并清理本模组新增物品：

- 腐败食物
- 保鲜内衬
- 冷藏模块
- 静滞核心

尤其注意检查玩家背包、箱子、Create 保险库、动态结构和包裹内部。

## 上传建议

- Primary file: `build/libs/foodcore-0.1.0.jar`
- Loader: `NeoForge`
- Game version: `1.21.1`
- Dependencies:
  - `create` -> required
  - `cold-sweat` -> required
