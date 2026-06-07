package com.navip.foodcore.util;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import com.navip.foodcore.FoodCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * 环境温度访问工具。
 *
 * 负责屏蔽 FoodCore 对外部温度来源的差异，
 * 优先读取 Cold Sweat 的世界温度或玩家温度，
 * 在未安装或调用失败时再回退到原版生物群系温度映射。
 */
public class TemperatureHelper {
    private static boolean loggedColdSweatFailure = false;

    public static float getTemperature(Level level, BlockPos pos) {
        return getColdSweatWorldTemperature(level, pos);
    }

    private static float getBiomeTemperature(Level level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        float baseTemp = biome.getBaseTemperature(); // 0.0 ~ 2.0
        // 映射到 -30°C ~ 50°C
        return -30.0f + baseTemp * 80.0f;
    }

    public static float getTemperature(ServerPlayer player) {
        return getColdSweatPlayerTemperature(player);
    }

    private static float getColdSweatWorldTemperature(Level level, BlockPos pos) {
        try {
            double worldTempMc = WorldHelper.getTemperatureAt(level, pos);
            return (float) Temperature.convert(worldTempMc, Temperature.Units.MC, Temperature.Units.C, true);
        } catch (Throwable t) {
            logColdSweatFailure(t);
            return getBiomeTemperature(level, pos);
        }
    }

    private static float getColdSweatPlayerTemperature(ServerPlayer player) {
        try {
            double worldTempMc = Temperature.get(player, Temperature.Trait.WORLD);
            return (float) Temperature.convertIfNeeded(worldTempMc, Temperature.Trait.WORLD, Temperature.Units.C, true);
        } catch (Throwable t) {
            logColdSweatFailure(t);
            return getBiomeTemperature(player.level(), player.blockPosition());
        }
    }

    private static void logColdSweatFailure(Throwable t) {
        if (!loggedColdSweatFailure) {
            loggedColdSweatFailure = true;
            FoodCore.LOGGER.warn("Failed to query Cold Sweat temperature API, falling back to biome temperature", t);
        }
    }
}
