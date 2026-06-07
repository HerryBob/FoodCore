package com.navip.foodcore.client;

import com.navip.foodcore.config.FoodConditionGroupConfigManager;
import com.navip.foodcore.data.ModDataComponents;
import com.navip.foodcore.condition.FoodConditionState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 食物状态的客户端视觉辅助工具。
 *
 * 负责把内部状态值转换为渲染层可直接使用的视觉参数，
 * 当前主要提供“是否显示冻结覆盖层”以及“冻结覆盖层透明度”的计算。
 * 它是纯客户端工具，不参与服务器侧业务推进。
 */
public final class FoodConditionVisuals {
    public static final float MAX_FROZEN_OVERLAY_ALPHA = 0.65f;

    private FoodConditionVisuals() {
    }

    public static boolean shouldRenderFrozenOverlay(ItemStack stack) {
        return FoodConditionGroupConfigManager.isFoodRegistered(stack.getItem()) && getFrozenOverlayAlpha(stack) > 0.0f;
    }

    public static float getFrozenOverlayAlpha(ItemStack stack) {
        FoodConditionState state = stack.getOrDefault(ModDataComponents.FOOD_STATE.get(), FoodConditionState.initial());
        return getFrozenOverlayAlpha(state.value());
    }

    public static float getFrozenOverlayAlpha(float foodStateValue) {
        if (foodStateValue >= -0.5f) {
            return 0.0f;
        }

        float progress = Mth.clamp(((-foodStateValue) - 0.5f) / 1.0f, 0.0f, 1.0f);
        return progress * MAX_FROZEN_OVERLAY_ALPHA;
    }
}


