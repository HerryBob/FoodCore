package com.navip.foodcore.mixin.client;

import com.navip.foodcore.client.model.FoodConditionBakedModel;
import com.navip.foodcore.client.model.FoodConditionModelBaker;
import com.navip.foodcore.config.FoodConditionGroupConfigManager;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 向原版模型烘焙流程注入 FoodCore 冻结变体模型的 Mixin。
 *
 * 在顶层模型烘焙完成后，
 * 它会为受管控的食物物品额外生成三个冻结阶段模型，
 * 并用 `FoodConditionBakedModel` 包装原始模型，接管后续渲染选择逻辑。
 */
@Mixin(ModelBakery.class)
public abstract class ModelBakeryMixin {
    @Shadow
    @Final
    private Map<ModelResourceLocation, UnbakedModel> topLevelModels;

    @Shadow
    @Final
    private Map<ModelResourceLocation, BakedModel> bakedTopLevelModels;

    @Shadow
    @Final
    private Map<ResourceLocation, UnbakedModel> unbakedCache;

    @Shadow
    @Final
    private UnbakedModel missingModel;

    @Inject(method = "bakeModels", at = @At("RETURN"))
    private void foodcore$addFrozenModels(ModelBakery.TextureGetter spriteGetter, CallbackInfo ci) {
        BiFunction<ModelResourceLocation, String, Function<Material, TextureAtlasSprite>> spriteMapper = (spriteId, suffix) -> material -> {
            TextureAtlasSprite sprite = spriteGetter.get(spriteId, new Material(
                    material.atlasLocation(),
                    material.texture().withSuffix(suffix)
            ));
            return sprite.contents().name().equals(ModelBakery.MISSING_MODEL_LOCATION)
                    ? spriteGetter.get(spriteId, material)
                    : sprite;
        };

        this.topLevelModels.forEach((spriteId, unbakedModel) -> {
            if (unbakedModel instanceof BlockModel blockModel
                    && blockModel.getRootModel() == ModelBakery.GENERATION_MARKER
                    && this.foodcore$isRegisteredFoodItem(spriteId)) {
                BakedModel original = this.bakedTopLevelModels.get(spriteId);
                if (original == null) {
                    return;
                }

                this.bakedTopLevelModels.put(spriteId, new FoodConditionBakedModel(
                        original,
                        this.foodcore$bakeModel(spriteMapper, unbakedModel, ".foodcore_frozen_1", spriteId),
                        this.foodcore$bakeModel(spriteMapper, unbakedModel, ".foodcore_frozen_2", spriteId),
                        this.foodcore$bakeModel(spriteMapper, unbakedModel, ".foodcore_frozen_3", spriteId)
                ));
            }
        });
    }

    @Unique
    private BakedModel foodcore$bakeModel(
            BiFunction<ModelResourceLocation, String, Function<Material, TextureAtlasSprite>> spriteMapper,
            UnbakedModel unbakedModel,
            String suffix,
            ModelResourceLocation spriteId
    ) {
        return Objects.requireNonNull(new FoodConditionModelBaker(
                this.unbakedCache,
                this.topLevelModels,
                this.missingModel,
                spriteMapper.apply(spriteId, suffix)
        ).bakeUncached(unbakedModel));
    }

    @Unique
    private boolean foodcore$isRegisteredFoodItem(ModelResourceLocation modelId) {
        if (!"inventory".equals(modelId.getVariant())) {
            return false;
        }

        ResourceLocation itemId = modelId.id();
        return BuiltInRegistries.ITEM.containsKey(itemId)
                && FoodConditionGroupConfigManager.isFoodRegistered(BuiltInRegistries.ITEM.get(itemId));
    }
}

