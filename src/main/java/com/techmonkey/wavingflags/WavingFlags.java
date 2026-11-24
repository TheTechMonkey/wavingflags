package com.techmonkey.wavingflags;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

public class WavingFlags implements ModInitializer {
    public static final String MOD_ID = "wavingflags";

    public static final FlagBlock SMALL_FLAG = registerBlock("small_flag", 1);
    public static final FlagBlock MEDIUM_FLAG = registerBlock("medium_flag", 2);
    public static final FlagBlock LARGE_FLAG = registerBlock("large_flag", 3);

    public static final BlockItem SMALL_FLAG_ITEM = registerItem("small_flag", SMALL_FLAG);
    public static final BlockItem MEDIUM_FLAG_ITEM = registerItem("medium_flag", MEDIUM_FLAG);
    public static final BlockItem LARGE_FLAG_ITEM = registerItem("large_flag", LARGE_FLAG);

    public static final BlockEntityType<FlagBlockEntity> FLAG_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("flag"),
            new BlockEntityType<>(FlagBlockEntity::new, Set.of(SMALL_FLAG, MEDIUM_FLAG, LARGE_FLAG)));

    public static final RecipeSerializer<FlagRecipe> FLAG_RECIPE = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER, id("flag"), FlagRecipe.SERIALIZER);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static FlagBlock registerBlock(String name, int size) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, new FlagBlock(size, BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOL)
                .noCollision()
                .noOcclusion()
                .strength(0.8F)
                .sound(SoundType.WOOL)
                .ignitedByLava()
                .setId(key)));
    }

    private static BlockItem registerItem(String name, Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties()
                .stacksTo(16)
                .component(DataComponents.BASE_COLOR, DyeColor.WHITE)
                .setId(key)
                .useBlockDescriptionPrefix()));
    }

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("flags"),
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.wavingflags.flags"))
                        .icon(() -> withColor(LARGE_FLAG_ITEM, DyeColor.RED))
                        .displayItems((context, output) -> {
                            for (BlockItem flag : new BlockItem[] {SMALL_FLAG_ITEM, MEDIUM_FLAG_ITEM, LARGE_FLAG_ITEM}) {
                                for (DyeColor color : DyeColor.values()) {
                                    output.accept(withColor(flag, color));
                                }
                            }
                        })
                        .build());
    }

    public static ItemStack withColor(Item flag, DyeColor color) {
        ItemStack stack = new ItemStack(flag);
        stack.set(DataComponents.BASE_COLOR, color);
        return stack;
    }
}
