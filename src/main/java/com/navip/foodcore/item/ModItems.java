package com.navip.foodcore.item;

import com.navip.foodcore.FoodCore;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组物品注册表。
 *
 * 负责集中注册 FoodCore 自己提供的物品内容，
 * 当前包括腐败食物，以及容器保鲜系统使用的三种功能物品。
 * 任何新增的玩法型物品通常都应首先从这里进入注册链。
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FoodCore.MODID);

    public static final DeferredItem<Item> PRESERVATION_LINING = ITEMS.register("preservation_lining",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REFRIGERATION_MODULE = ITEMS.register("refrigeration_module",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STASIS_CORE = ITEMS.register("stasis_core",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPOILED_FOOD = ITEMS.register("spoiled_food", () ->
            new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0.1f)
                    .alwaysEdible()
                    .fast()
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 20 * 15, 0), 1.0f)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 20 * 10, 0), 0.65f)
                    .effect(() -> new MobEffectInstance(MobEffects.POISON, 20 * 8, 0), 0.35f)
                    .build())));

    private ModItems() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}

