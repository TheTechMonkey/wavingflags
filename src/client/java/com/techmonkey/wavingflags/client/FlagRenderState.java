package com.techmonkey.wavingflags.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class FlagRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public int size = 1;
    public DyeColor baseColor = DyeColor.WHITE;
    public BannerPatternLayers patterns = BannerPatternLayers.EMPTY;
    public float time;
    public float seed;
}
