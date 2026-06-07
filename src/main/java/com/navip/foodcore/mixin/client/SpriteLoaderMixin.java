package com.navip.foodcore.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.navip.foodcore.config.FoodConditionGroupConfigManager;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * 向贴图加载流程注入冻结贴图变体生成逻辑的 Mixin。
 *
 * 它会在物品贴图进入图集前，为受管控食物自动派生多个冷色版本，
 * 并为后续的模型烘焙阶段提供 `.foodcore_frozen_x` 后缀的贴图资源。
 * 这样客户端不需要手工维护多套冻结贴图文件。
 */
@Mixin(SpriteLoader.class)
public abstract class SpriteLoaderMixin {
    @Shadow
    @Final
    private ResourceLocation location;

    @Unique
    private static final int FOODCORE_COLD_BLUE = 255;
    private static final int FOODCORE_COLD_GREEN = 255;
    private static final int FOODCORE_COLD_RED = 100;

    @SuppressWarnings("deprecation")
    @ModifyVariable(method = "stitch", at = @At("HEAD"), argsOnly = true, index = 1)
    private List<SpriteContents> foodcore$addFrozenVariants(List<SpriteContents> contents) {
        if (!this.location.equals(TextureAtlas.LOCATION_BLOCKS)) {
            return contents;
        }

        List<SpriteContents> extendedContents = new ArrayList<>(contents);
        contents.stream()
                .filter(content -> content != null && isItemSprite(content.name()))
                .forEach(content -> {
                    ResourceMetadata metadata = content.metadata();
                    NativeImage original = content.getOriginalImage();
                    FrameSize frameSize = metadata.getSection(AnimationMetadataSection.SERIALIZER)
                            .map(section -> section.calculateFrameSize(original.getWidth(), original.getHeight()))
                            .orElse(new FrameSize(original.getWidth(), original.getHeight()));

                    for (int level = 1; level <= 3; level++) {
                        NativeImage frozenImage = new NativeImage(original.format(), original.getWidth(), original.getHeight(), true);
                        frozenImage.copyFrom(original);
                        foodcore$remapFrozenImage(frozenImage, Pair.of(content.width(), content.height()), level);

                        SpriteContents frozenContent = new SpriteContents(
                                content.name().withSuffix(".foodcore_frozen_" + level),
                                frameSize,
                                frozenImage,
                                metadata
                        );
                        extendedContents.add(frozenContent);
                    }
                });
        return extendedContents;
    }

    @Unique
    private static boolean isItemSprite(ResourceLocation name) {
        String path = name.getPath();
        if (!(path.startsWith("item/") || path.startsWith("items/"))) {
            return false;
        }

        int slashIndex = path.indexOf('/');
        if (slashIndex < 0 || slashIndex + 1 >= path.length()) {
            return false;
        }

        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(name.getNamespace(), path.substring(slashIndex + 1));
        return BuiltInRegistries.ITEM.containsKey(itemId)
                && FoodConditionGroupConfigManager.isFoodRegistered(BuiltInRegistries.ITEM.get(itemId));
    }

    @Unique
    private static void foodcore$remapFrozenImage(NativeImage image, Pair<Integer, Integer> size, int level) {
        int targetBlue = FOODCORE_COLD_RED;
        int targetGreen = FOODCORE_COLD_GREEN;
        int targetRed = FOODCORE_COLD_BLUE;
        double blendRate = 0.37D + level * 0.21D;
        int xFrameCount = image.getWidth() / size.getFirst();
        int yFrameCount = image.getHeight() / size.getSecond();

        for (int frameX = 0; frameX < xFrameCount; ++frameX) {
            for (int frameY = 0; frameY < yFrameCount; ++frameY) {
                for (int x = 0; x < size.getFirst(); ++x) {
                    for (int y = 0; y < size.getSecond(); ++y) {
                        int px = x + size.getFirst() * frameX;
                        int py = y + size.getSecond() * frameY;
                        int originalColor = image.getPixelRGBA(px, py);

                        int alpha = FastColor.ABGR32.alpha(originalColor);
                        if (alpha <= 0x0F) {
                            continue;
                        }

                        int blue = FastColor.ABGR32.blue(originalColor);
                        int green = FastColor.ABGR32.green(originalColor);
                        int red = FastColor.ABGR32.red(originalColor);

                        int gray = (red * 30 + green * 59 + blue * 11) / 100;
                        double luminance = gray / 255.0D;
                        double blend = Mth.clamp(blendRate * (0.62D + luminance * 0.28D), 0.0D, 0.79D);
                        double desat = Mth.clamp(0.18D + blend * 0.95D, 0.0D, 1.0D);

                        red = foodcore$lerp(red, gray, desat);
                        green = foodcore$lerp(green, gray, desat);
                        blue = foodcore$lerp(blue, gray, desat);

                        red = foodcore$lerp(red, targetRed, blend);
                        green = foodcore$lerp(green, targetGreen, blend);
                        blue = foodcore$lerp(blue, targetBlue, blend);

                        image.setPixelRGBA(px, py, FastColor.ABGR32.color(alpha, red, green, blue));
                    }
                }
            }
        }
    }

    @Unique
    private static int foodcore$lerp(int from, int to, double delta) {
        return Mth.clamp((int) Math.round(from * (1.0D - delta) + to * delta), 0, 255);
    }
}

