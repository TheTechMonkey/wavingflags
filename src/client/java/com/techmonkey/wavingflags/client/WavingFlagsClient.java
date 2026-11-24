package com.techmonkey.wavingflags.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import net.fabricmc.api.ClientModInitializer;

import com.techmonkey.wavingflags.WavingFlags;

public class WavingFlagsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(WavingFlags.FLAG_BLOCK_ENTITY, FlagRenderer::new);
    }
}
