package com.navip.foodcore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.navip.foodcore.FoodCore;

import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 食物组配置加载与映射中心。
 *
 * 负责从运行目录读取 `food_groups.json`，
 * 在首次启动时生成默认配置，并把配置中的食物条目映射为 `Item -> GroupConfig` 快速查询表。
 * 业务代码通常通过这里判断某个物品是否受 FoodCore 管控。
 */
public class FoodConditionGroupConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(FoodCore.MODID + "/food_groups.json");
    private static final Map<Item, FoodConditionGroupConfig> ITEM_TO_GROUP = new ConcurrentHashMap<>();
    private static List<FoodConditionGroupConfig> groups = new ArrayList<>();

    /**
     * 从运行目录重新加载食物组配置。
     *
     * 会在文件缺失时生成默认配置，
     * 然后重建运行时的组列表和物品映射缓存。
     */
    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                createDefaultConfig();
            }
            String json = Files.readString(CONFIG_PATH);
            Type type = new TypeToken<Map<String, List<FoodConditionGroupConfig>>>(){}.getType();
            Map<String, List<FoodConditionGroupConfig>> wrapper = GSON.fromJson(json, type);
            if (wrapper == null) {
                groups = new ArrayList<>();
            } else {
                groups = wrapper.getOrDefault("groups", new ArrayList<>());
            }


            ITEM_TO_GROUP.clear();
            for (FoodConditionGroupConfig group : groups) {
                for (Item item : group.getParsedFoods()) {
                    ITEM_TO_GROUP.put(item, group);
                }
            }
            FoodCore.LOGGER.info("Loaded {} food groups, {} items registered", groups.size(), ITEM_TO_GROUP.size());
        } catch (Exception e) {
            groups = new ArrayList<>();
            ITEM_TO_GROUP.clear();
            FoodCore.LOGGER.error("Failed to load food groups config", e);
        }
    }

    private static void createDefaultConfig() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            Map<String, List<FoodConditionGroupConfig>> defaultWrapper = new HashMap<>();
            defaultWrapper.put("groups", getDefaultGroups());
            GSON.toJson(defaultWrapper, writer);
        }
    }

    private static List<FoodConditionGroupConfig> getDefaultGroups() {
        List<FoodConditionGroupConfig> list = new ArrayList<>();


        FoodConditionGroupConfig fruits = new FoodConditionGroupConfig();
        fruits.setName("perishable_fruits");
        fruits.setEnableFreezing(true);
        fruits.setFreezeStartTemp(0.0f);
        fruits.setFreezeMinTemp(-30.0f);
        fruits.setFreezeTimeSeconds(600);
        fruits.setEnableSpoilage(true);
        fruits.setSpoilageMinTemp(5.0f);
        fruits.setSpoilageMaxTemp(35.0f);
        fruits.setSpoilageTimeSeconds(1200);
        fruits.setFoods(Arrays.asList(
                Arrays.asList("minecraft:apple", 20.0f, 1200),
                Arrays.asList("minecraft:sweet_berries", 15.0f, 1200),
                Arrays.asList("minecraft:melon_slice", 10.0f, 1200)
        ));
        list.add(fruits);

        return list;
    }

    /**
     * 查询某个物品所属的食物组；若未注册则返回 `null`。
     */
    public static FoodConditionGroupConfig getGroupForItem(Item item) {
        return ITEM_TO_GROUP.get(item);
    }

    /**
     * 判断某个物品是否受 FoodCore 的状态系统管理。
     */
    public static boolean isFoodRegistered(Item item) {
        return ITEM_TO_GROUP.containsKey(item);
    }

    /**
     * 返回当前已加载的全部食物组的只读视图。
     */
    public static List<FoodConditionGroupConfig> getAllGroups() {
        return Collections.unmodifiableList(groups);
    }
}





