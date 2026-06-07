# FoodCore 0.1.0

## Overview / 简介

FoodCore is a food condition mod for NeoForge 1.21.1.

FoodCore 是一个面向 NeoForge 1.21.1 的食物状态模组。

It uses Cold Sweat temperature data to drive food spoilage, freezing, and thawing, and extends that logic to player inventories, vanilla containers, and Create storages and contraptions.

它基于 Cold Sweat 的环境温度系统，让食物会因为环境温度逐步腐败、冻结和解冻，并将这一套逻辑扩展到玩家背包、原版容器以及 Create 容器与动态结构中。

> This mod must be installed on both the client and the server.
>
> 本模组需要客户端和服务端同时安装。

> Note: This mod was developed with AI assistance.
>
> 注意：本模组在开发过程中使用了 AI 辅助。

## Features In This Release / 本版本内容

- Complete core food condition update pipeline
- Player inventory food condition processing
- Dropped item food condition processing
- Vanilla static container support
- Vanilla entity container support
- Create static capability container support
- Create contraption and train mounted storage support
- Recursive food condition processing inside Create packages
- Added Preservation Lining, Refrigeration Module, and Stasis Core
- Added tooltip display
- Added Cold Sweat-linked frozen food consumption effects
- Added a set of configurable global options

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

## Dependencies / 依赖

- Minecraft `1.21.1`
- NeoForge `21.1.219`
- Create `6.0.10+`
- Cold Sweat `2.4.1+`

Create and Cold Sweat are required dependencies in this version.

当前版本将 Create 和 Cold Sweat 设为强依赖。

## Main Config Options / 主要配置项

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

## Known Limitations / 已知限制

- Dropped item detection still has room for further optimization
- Automatic merging for static containers and entity containers is not implemented yet
- Batch scanning is not implemented yet
- Aeronautics physical structure support has not started yet

- 掉落物检测仍有后续优化空间
- 静态容器和实体容器自动合并尚未接入
- 分批扫描机制尚未落地
- 航空学物理结构兼容尚未开始

## Uninstall Notes / 卸载提示

Before removing the mod, it is recommended to back up your world and clear the items added by this mod:

移除模组前，建议先备份存档并清理本模组新增物品：

- Spoiled Food
- Preservation Lining
- Refrigeration Module
- Stasis Core
- 腐败食物
- 保鲜内衬
- 冷藏模块
- 静滞核心

Pay special attention to player inventories, chests, Create vaults, dynamic structures, and package contents.

尤其注意检查玩家背包、箱子、Create 保险库、动态结构和包裹内部。

## Upload Notes / 上传建议

- Primary file: `build/libs/foodcore-0.1.0.jar`
- Loader: `NeoForge`
- Game version: `1.21.1`
- Dependencies:
  - `create` -> required
  - `cold-sweat` -> required
