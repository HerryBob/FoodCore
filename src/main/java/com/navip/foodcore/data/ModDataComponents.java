package com.navip.foodcore.data;

import com.navip.foodcore.FoodCore;
import com.navip.foodcore.condition.FoodConditionState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * FoodCore 使用到的数据组件注册表。
 *
 * 当前最重要的内容是 `FOOD_STATE`，
 * 它把食物状态正式存放到 `ItemStack` 的 Data Component 系统中，
 * 从而支持持久化、网络同步和统一读写，而不是依赖零散的自定义 NBT。
 */
public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FoodCore.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FoodConditionState>> FOOD_STATE_HOLDER =
            DATA_COMPONENTS.registerComponentType("food_state",
                    builder -> builder
                            .persistent(FoodConditionState.CODEC)
                            .networkSynchronized(FoodConditionState.STREAM_CODEC)
                            .cacheEncoding()
            );

    public static final Supplier<DataComponentType<FoodConditionState>> FOOD_STATE = FOOD_STATE_HOLDER;

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}


