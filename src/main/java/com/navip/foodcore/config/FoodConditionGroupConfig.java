package com.navip.foodcore.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单个食物组的配置数据对象。
 *
 * 它描述了一组食物共享的腐败、冻结、解冻相关规则，
 * 同时也负责把配置文件中的原始食物条目解析为可直接查询的运行时结构。
 * 配置加载完成后，大部分业务逻辑都会通过这个对象读取组级规则。
 */
public class FoodConditionGroupConfig {
    private String name;
    private boolean enableFreezing;     
    private float freezeStartTemp;      
    private float freezeMinTemp;        
    private int freezeTimeSeconds;     
    private boolean enableSpoilage;     
    private float spoilageMinTemp;      
    private float spoilageMaxTemp;      
    private int spoilageTimeSeconds;    
    private List<Object> foods;
    // Legacy fallback for older configs. New configs should encode temperature in foods entries.
    private List<Float> coldedFoodTemperature;

    private transient Set<Item> parsedFoods;
    private transient Map<Item, ParsedFoodEntry> parsedFoodEntries;
    private transient Map<Item, Float> parsedColdFoodTemperatures;

    // Getters
    public String getName() { return name; }
    public boolean isEnableFreezing() { return enableFreezing; }
    public float getFreezeStartTemp() { return freezeStartTemp; }
    public float getFreezeMinTemp() { return freezeMinTemp; }
    public int getFreezeTimeSeconds() { return freezeTimeSeconds; }
    public boolean isEnableSpoilage() { return enableSpoilage; }
    public float getSpoilageMinTemp() { return spoilageMinTemp; }
    public float getSpoilageMaxTemp() { return spoilageMaxTemp; }
    public int getSpoilageTimeSeconds() { return spoilageTimeSeconds; }
    public List<Object> getFoods() { return foods; }
    public List<Float> getColdedFoodTemperature() { return coldedFoodTemperature; }

    //setters
    public void setName(String name) { this.name = name; }
    public void setEnableFreezing(boolean enableFreezing) { this.enableFreezing = enableFreezing; }
    public void setFreezeStartTemp(float freezeStartTemp) { this.freezeStartTemp = freezeStartTemp; }
    public void setFreezeMinTemp(float freezeMinTemp) { this.freezeMinTemp = freezeMinTemp; }
    public void setFreezeTimeSeconds(int freezeTimeSeconds) { this.freezeTimeSeconds = freezeTimeSeconds; }
    public void setEnableSpoilage(boolean enableSpoilage) { this.enableSpoilage = enableSpoilage; }
    public void setSpoilageMinTemp(float spoilageMinTemp) { this.spoilageMinTemp = spoilageMinTemp; }
    public void setSpoilageMaxTemp(float spoilageMaxTemp) { this.spoilageMaxTemp = spoilageMaxTemp; }
    public void setSpoilageTimeSeconds(int spoilageTimeSeconds) { this.spoilageTimeSeconds = spoilageTimeSeconds; }
    public void setFoods(List<Object> foods) { this.foods = foods; }
    public void setColdedFoodTemperature(List<Float> coldedFoodTemperature) { this.coldedFoodTemperature = coldedFoodTemperature; }

    public Set<Item> getParsedFoods() {
        ensureParsedFoodEntries();
        return parsedFoods;
    }

    public float getColdedFoodTemperature(Item item) {
        if (item == null) {
            return 0.0f;
        }

        ensureParsedFoodEntries();
        ParsedFoodEntry entry = parsedFoodEntries.get(item);
        if (entry != null && entry.coldTemperature() > 0.0f) {
            return entry.coldTemperature();
        }

        if (parsedColdFoodTemperatures == null) {
            parsedColdFoodTemperatures = buildItemValueMap(coldedFoodTemperature);
        }
        return parsedColdFoodTemperatures.getOrDefault(item, 0.0f);
    }

    public boolean hasColdedFoodTemperature(Item item) {
        if (item == null) {
            return false;
        }
        ensureParsedFoodEntries();
        ParsedFoodEntry entry = parsedFoodEntries.get(item);
        if (entry != null && entry.coldTemperature() > 0.0f) {
            return true;
        }
        if (parsedColdFoodTemperatures == null) {
            parsedColdFoodTemperatures = buildItemValueMap(coldedFoodTemperature);
        }
        return parsedColdFoodTemperatures.containsKey(item);
    }

    public int getColdedFoodDurationTicks(Item item) {
        if (item == null) {
            return 0;
        }
        ensureParsedFoodEntries();
        ParsedFoodEntry entry = parsedFoodEntries.get(item);
        return entry != null ? Math.max(0, entry.coldDurationTicks()) : 0;
    }

    private Map<Item, Float> buildItemValueMap(List<Float> values) {
        Map<Item, Float> result = new HashMap<>();
        if (foods == null || values == null) {
            return result;
        }

        int size = Math.min(foods.size(), values.size());
        for (int i = 0; i < size; i++) {
            Item mappedItem = resolveItemId(extractItemId(foods.get(i)));
            Float value = values.get(i);
            if (mappedItem != null && value != null) {
                result.put(mappedItem, Math.max(0.0f, value));
            }
        }
        return result;
    }

    private void ensureParsedFoodEntries() {
        if (parsedFoodEntries != null && parsedFoods != null) {
            return;
        }

        parsedFoodEntries = new HashMap<>();
        parsedFoods = new HashSet<>();
        if (foods == null) {
            parsedFoods = Collections.emptySet();
            return;
        }

        for (Object rawEntry : foods) {
            ParsedFoodEntry entry = parseFoodEntry(rawEntry);
            if (entry == null) {
                continue;
            }
            parsedFoodEntries.put(entry.item(), entry);
            parsedFoods.add(entry.item());
        }
    }

    @Nullable
    private ParsedFoodEntry parseFoodEntry(Object rawEntry) {
        String itemId = extractItemId(rawEntry);
        Item item = resolveItemId(itemId);
        if (item == null) {
            return null;
        }

        if (rawEntry instanceof List<?> list) {
            float coldTemperature = getOptionalFloat(list, 1);
            int coldDurationTicks = getOptionalInt(list, 2);
            return new ParsedFoodEntry(item, coldTemperature, coldDurationTicks);
        }

        return new ParsedFoodEntry(item, 0.0f, 0);
    }

    @Nullable
    private static String extractItemId(Object rawEntry) {
        if (rawEntry instanceof String itemId) {
            return itemId;
        }
        if (rawEntry instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof String itemId) {
            return itemId;
        }
        return null;
    }

    private static float getOptionalFloat(List<?> list, int index) {
        if (index >= list.size()) {
            return 0.0f;
        }
        Object rawValue = list.get(index);
        if (rawValue instanceof Number number) {
            return Math.max(0.0f, number.floatValue());
        }
        return 0.0f;
    }

    private static int getOptionalInt(List<?> list, int index) {
        if (index >= list.size()) {
            return 0;
        }
        Object rawValue = list.get(index);
        if (rawValue instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        return 0;
    }

    @Nullable
    private static Item resolveItemId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(location);
    }

    private record ParsedFoodEntry(Item item, float coldTemperature, int coldDurationTicks) {
    }

}

