package com.navip.foodcore.client.model;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Function;

/**
 * FoodCore 的模型烘焙适配器。
 *
 * 它对原版 `ModelBaker` 做了一层轻量封装，
 * 目的是在运行时重新烘焙带有特殊贴图后缀的冻结模型变体。
 * 这样可以复用原始物品模型结构，而不必为每个阶段手写完整模型文件。
 */
public record FoodConditionModelBaker(
        Map<ResourceLocation, UnbakedModel> models,
        Map<ModelResourceLocation, UnbakedModel> topLevelModels,
        UnbakedModel missingModel,
        Function<Material, TextureAtlasSprite> spriteGetter
) implements ModelBaker {
    @Override
    public UnbakedModel getModel(ResourceLocation location) {
        return this.models.getOrDefault(location, this.missingModel);
    }

    @Override
    @Nullable
    public BakedModel bake(ResourceLocation location, ModelState modelState) {
        return this.bake(location, modelState, this.getModelTextureGetter());
    }

    @Override
    @Nullable
    public UnbakedModel getTopLevelModel(ModelResourceLocation location) {
        return this.topLevelModels.getOrDefault(location, this.missingModel);
    }

    @Override
    @Nullable
    public BakedModel bake(ResourceLocation location, ModelState modelState, Function<Material, TextureAtlasSprite> spriteGetter) {
        return this.bakeUncached(this.getModel(location), modelState, spriteGetter);
    }

    @Nullable
    public BakedModel bakeUncached(UnbakedModel model) {
        return this.bakeUncached(model, BlockModelRotation.X0_Y0, this.spriteGetter);
    }

    @Override
    @Nullable
    public BakedModel bakeUncached(UnbakedModel model, ModelState modelState, Function<Material, TextureAtlasSprite> sprites) {
        if (model instanceof BlockModel blockModel) {
            return new ItemModelGenerator().generateBlockModel(this.spriteGetter, blockModel).bake(
                    this,
                    blockModel,
                    this.spriteGetter,
                    modelState,
                    false
            );
        }
        return model.bake(this, this.spriteGetter, modelState);
    }

    @Override
    public Function<Material, TextureAtlasSprite> getModelTextureGetter() {
        return this.spriteGetter;
    }
}

