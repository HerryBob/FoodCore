package com.navip.foodcore.item;

import com.navip.foodcore.FoodCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组创造标签页注册表。
 *
 * 用来定义 FoodCore 在创造模式中的独立分类页，
 * 并统一控制图标物品与展示顺序。
 * 这个类不参与业务计算，但决定了测试和开发时的物品访问入口。
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FoodCore.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FOODCORE_TAB = CREATIVE_MODE_TABS.register("foodcore",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.foodcore"))
                    .icon(() -> new ItemStack(ModItems.SPOILED_FOOD.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.PRESERVATION_LINING.get());
                        output.accept(ModItems.REFRIGERATION_MODULE.get());
                        output.accept(ModItems.STASIS_CORE.get());
                        output.accept(ModItems.SPOILED_FOOD.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}

