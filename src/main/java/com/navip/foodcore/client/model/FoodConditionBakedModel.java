package com.navip.foodcore.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.navip.foodcore.client.FoodConditionVisuals;
import com.navip.foodcore.config.FoodConditionGroupConfigManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 带冻结阶段选择能力的包装模型。
 *
 * 该模型包裹原始物品模型，并额外持有三个冻结阶段的变体模型。
 * 在物品真正渲染时，会根据当前 `ItemStack` 的食物状态值动态选出合适的模型，
 * 从而实现冻结程度不同、显示外观不同的客户端效果。
 */
@SuppressWarnings("deprecation")
public record FoodConditionBakedModel(
        BakedModel original,
        BakedModel frozen1,
        BakedModel frozen2,
        BakedModel frozen3
) implements BakedModel {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return this.original.getQuads(state, direction, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.original.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.original.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.original.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return this.original.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.original.getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return new ItemOverrides() {
            @Override
            @Nullable
            public BakedModel resolve(BakedModel bakedModel, ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int seed) {
                BakedModel selected = FoodConditionBakedModel.this.getFrozenBakedModel(itemStack);
                return selected.getOverrides().resolve(selected, itemStack, clientLevel, livingEntity, seed);
            }
        };
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.original.getTransforms();
    }

    private BakedModel getFrozenBakedModel(ItemStack itemStack) {
        if (!FoodConditionGroupConfigManager.isFoodRegistered(itemStack.getItem())) {
            return this.original;
        }

        float alpha = FoodConditionVisuals.getFrozenOverlayAlpha(itemStack);
        if (alpha <= 0.0F) {
            return this.original;
        }

        float progress = Mth.clamp(alpha / FoodConditionVisuals.MAX_FROZEN_OVERLAY_ALPHA, 0.0F, 1.0F);
        int frozenLevel = Mth.clamp(Mth.ceil(progress * 3.0F), 1, 3);
        return switch (frozenLevel) {
            case 1 -> this.frozen1;
            case 2 -> this.frozen2;
            case 3 -> this.frozen3;
            default -> this.original;
        };
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        return this.original.getQuads(state, side, rand, data, renderType);
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        return this.original.useAmbientOcclusion(state, data, renderType);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        return new FoodConditionBakedModel(
                this.original.applyTransform(transformType, poseStack, applyLeftHandTransform),
                this.frozen1.applyTransform(transformType, poseStack, applyLeftHandTransform),
                this.frozen2.applyTransform(transformType, poseStack, applyLeftHandTransform),
                this.frozen3.applyTransform(transformType, poseStack, applyLeftHandTransform)
        );
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return this.original.getModelData(level, pos, state, modelData);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return this.original.getParticleIcon(data);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return this.original.getRenderTypes(state, rand, data);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        return this.original.getRenderTypes(itemStack, fabulous);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
        return this.original.getRenderPasses(itemStack, fabulous);
    }
}

