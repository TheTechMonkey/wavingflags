package com.techmonkey.wavingflags;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * Identical banners (same color and loom design) plus one stick, anywhere in the grid:
 * 2 banners = small flag, 4 banners = medium flag, 6 banners = large flag. The flag keeps
 * the banners' colors and patterns.
 *
 * <p>Plain wool works the same way for a solid-color flag: 2, 4 or 6 wool of one color plus
 * one stick. (Six wool above a centered stick is still the vanilla banner recipe.)
 */
public class FlagRecipe extends CustomRecipe {
    public static final FlagRecipe INSTANCE = new FlagRecipe();
    public static final RecipeSerializer<FlagRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE), StreamCodec.<RegistryFriendlyByteBuf, FlagRecipe>unit(INSTANCE));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return flagFor(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        BlockItem flag = flagFor(input);
        ItemStack first = firstMaterial(input);
        if (flag != null && first != null && woolColor(first) != null) {
            return WavingFlags.withColor(flag, woolColor(first));
        }
        ItemStack banner = first;
        if (flag == null || banner == null || !(banner.getItem() instanceof BannerItem bannerItem)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = WavingFlags.withColor(flag, bannerItem.getColor());
        result.set(DataComponents.BANNER_PATTERNS, banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY));
        if (banner.has(DataComponents.CUSTOM_NAME)) {
            result.set(DataComponents.CUSTOM_NAME, banner.get(DataComponents.CUSTOM_NAME));
        }
        return result;
    }

    /** The flag this grid makes, or null: one stick plus 2, 4 or 6 matching banners or wool. */
    private static BlockItem flagFor(CraftingInput input) {
        if (isVanillaBannerLayout(input)) {
            return null;
        }
        ItemStack first = null;
        int pieces = 0;
        int sticks = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.STICK)) {
                sticks++;
            } else if (stack.getItem() instanceof BannerItem || woolColor(stack) != null) {
                if (first == null) {
                    first = stack;
                } else if (!sameDesign(first, stack)) {
                    return null;
                }
                pieces++;
            } else {
                return null;
            }
        }
        if (sticks != 1) {
            return null;
        }
        return switch (pieces) {
            case 2 -> WavingFlags.SMALL_FLAG_ITEM;
            case 4 -> WavingFlags.MEDIUM_FLAG_ITEM;
            case 6 -> WavingFlags.LARGE_FLAG_ITEM;
            default -> null;
        };
    }

    /** Six wool filling the top two rows with the stick under the middle: leave that to vanilla. */
    private static boolean isVanillaBannerLayout(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 2; y++) {
                if (woolColor(input.getItem(x, y)) == null) {
                    return false;
                }
            }
        }
        return input.getItem(1, 2).is(Items.STICK) && input.getItem(0, 2).isEmpty() && input.getItem(2, 2).isEmpty();
    }

    /** The dye color of a wool block item, or null if the stack isn't wool. */
    private static DyeColor woolColor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("minecraft") || !id.getPath().endsWith("_wool")) {
            return null;
        }
        return DyeColor.byName(id.getPath().substring(0, id.getPath().length() - "_wool".length()), null);
    }

    private static boolean sameDesign(ItemStack first, ItemStack other) {
        return first.getItem() == other.getItem()
                && first.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                        .equals(other.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY));
    }

    private static ItemStack firstMaterial(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.getItem() instanceof BannerItem || woolColor(stack) != null) {
                return stack;
            }
        }
        return null;
    }

    @Override
    public RecipeSerializer<FlagRecipe> getSerializer() {
        return SERIALIZER;
    }
}
