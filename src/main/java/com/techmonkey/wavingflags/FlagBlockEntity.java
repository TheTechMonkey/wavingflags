package com.techmonkey.wavingflags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds a flag's design: the banner base color and loom patterns it was made from. */
public class FlagBlockEntity extends BlockEntity {
    private DyeColor baseColor = DyeColor.WHITE;
    private BannerPatternLayers patterns = BannerPatternLayers.EMPTY;

    public FlagBlockEntity(BlockPos pos, BlockState state) {
        super(WavingFlags.FLAG_BLOCK_ENTITY, pos, state);
    }

    public DyeColor getBaseColor() {
        return baseColor;
    }

    public BannerPatternLayers getPatterns() {
        return patterns;
    }

    public int getSize() {
        return getBlockState().getBlock() instanceof FlagBlock flag ? flag.getSize() : 1;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("base_color", DyeColor.CODEC, baseColor);
        if (!patterns.equals(BannerPatternLayers.EMPTY)) {
            output.store("patterns", BannerPatternLayers.CODEC, patterns);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        baseColor = input.read("base_color", DyeColor.CODEC).orElse(DyeColor.WHITE);
        patterns = input.read("patterns", BannerPatternLayers.CODEC).orElse(BannerPatternLayers.EMPTY);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        baseColor = components.getOrDefault(DataComponents.BASE_COLOR, DyeColor.WHITE);
        patterns = components.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.BASE_COLOR, baseColor);
        components.set(DataComponents.BANNER_PATTERNS, patterns);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("base_color");
        output.discard("patterns");
    }
}
