package com.techmonkey.wavingflags;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * Any banner (with any loom design) plus sticks, anywhere in the crafting grid:
 * 1 stick = small flag, 2 sticks = medium flag, 3 sticks = large flag. The flag keeps the
 * banner's colors and patterns.
 */
public class FlagRecipe extends CustomRecipe {
    public static final FlagRecipe INSTANCE = new FlagRecipe();
    public static final RecipeSerializer<FlagRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE), StreamCodec.<RegistryFriendlyByteBuf, FlagRecipe>unit(INSTANCE));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return countSticks(input) > 0 && findBanner(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack banner = findBanner(input);
        BlockItem flag = WavingFlags.flagForSticks(countSticks(input));
        if (banner == null || flag == null || !(banner.getItem() instanceof BannerItem bannerItem)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = WavingFlags.withColor(flag, bannerItem.getColor());
        result.set(DataComponents.BANNER_PATTERNS, banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY));
        if (banner.has(DataComponents.CUSTOM_NAME)) {
            result.set(DataComponents.CUSTOM_NAME, banner.get(DataComponents.CUSTOM_NAME));
        }
        return result;
    }

    /** Number of sticks if the grid holds only sticks and banners, else 0. */
    private static int countSticks(CraftingInput input) {
        int sticks = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || stack.getItem() instanceof BannerItem) {
                continue;
            }
            if (!stack.is(Items.STICK)) {
                return 0;
            }
            sticks++;
        }
        return sticks <= 3 ? sticks : 0;
    }

    /** The single banner in the grid, or null if there are none or several. */
    private static ItemStack findBanner(CraftingInput input) {
        ItemStack found = null;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.getItem() instanceof BannerItem) {
                if (found != null) {
                    return null;
                }
                found = stack;
            }
        }
        return found;
    }

    @Override
    public RecipeSerializer<FlagRecipe> getSerializer() {
        return SERIALIZER;
    }
}
