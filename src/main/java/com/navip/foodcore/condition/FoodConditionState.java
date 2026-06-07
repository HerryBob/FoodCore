package com.navip.foodcore.condition;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * 食物状态值对象。
 *
 * 该记录类封装了单个食物堆的状态轴取值，
 * 统一规定了最小值、最大值以及编解码方式，
 * 供 Data Component 持久化、网络同步和运行时计算共同使用。
 */
public record FoodConditionState(float value) {
    public static final float MIN_VALUE = -1.5f;
    public static final float MAX_VALUE = 1.5f;
    public static final Codec<FoodConditionState> CODEC = Codec.FLOAT.xmap(FoodConditionState::new, FoodConditionState::value);
    public static final StreamCodec<ByteBuf, FoodConditionState> STREAM_CODEC =
            ByteBufCodecs.FLOAT.map(FoodConditionState::new, FoodConditionState::value);

    public static FoodConditionState initial() {
        return new FoodConditionState(0f);
    }

    public FoodConditionState withValue(float newValue) {
        return new FoodConditionState(Mth.clamp(newValue, MIN_VALUE, MAX_VALUE));
    }

    public boolean isFrozen() {
        return value <= MIN_VALUE;
    }

    public boolean isSpoiled() {
        return value >= MAX_VALUE;
    }
}


