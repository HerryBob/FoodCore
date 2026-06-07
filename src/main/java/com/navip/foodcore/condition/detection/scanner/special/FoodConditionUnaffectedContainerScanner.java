package com.navip.foodcore.condition.detection.scanner.special;

import net.minecraft.server.level.ServerPlayer;

/**
 * 当前被视为不受环境影响的特殊容器入口。
 * 后续如果需要支持配置化的免疫容器，可以从这里扩展。
 */
/**
 * 不受环境影响容器的统一入口。
 *
 * 用来承载那些被设计为“免疫环境推进”的特殊容器逻辑，
 * 当前实际接入的是玩家末影箱，并采用 no-op 方式跳过状态推进。
 * 后续如果要支持配置化免疫容器，也应从这里继续扩展。
 */
public final class FoodConditionUnaffectedContainerScanner {
    private FoodConditionUnaffectedContainerScanner() {
    }

    public static void processPlayerEnderChest(ServerPlayer player, float envTemp, float deltaTicks) {
        // Intentionally no-op: ender chest food is currently unaffected.
    }
}

