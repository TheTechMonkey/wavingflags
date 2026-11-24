package com.techmonkey.wavingflags.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.phys.Vec3;

import com.techmonkey.wavingflags.FlagBlock;
import com.techmonkey.wavingflags.FlagBlockEntity;

/**
 * Draws a flag as a real rippling sheet of cloth, painted with the vanilla banner textures
 * (base color plus each loom pattern layer, exactly like a banner).
 *
 * <p>The cloth is a grid of small quads. Each column is pushed sideways by a traveling sine
 * wave that is pinned at the pole and grows toward the free end, so it reads as wind.
 */
public class FlagRenderer implements BlockEntityRenderer<FlagBlockEntity, FlagRenderState> {
    /** Front face of the banner cloth in the 64x64 banner textures: x 1..21, y 1..41. */
    private static final float TEX_LEFT = 1.0F / 64.0F;
    private static final float TEX_WIDTH = 20.0F / 64.0F;
    private static final float TEX_TOP = 1.0F / 64.0F;
    private static final float TEX_HEIGHT = 40.0F / 64.0F;
    private static final int MAX_PATTERNS = 16;

    private final SpriteGetter sprites;

    public FlagRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public FlagRenderState createRenderState() {
        return new FlagRenderState();
    }

    @Override
    public void extractRenderState(FlagBlockEntity flag, FlagRenderState state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(flag, state, partialTicks, cameraPosition, breakProgress);
        state.facing = flag.getBlockState().hasProperty(FlagBlock.FACING) ? flag.getBlockState().getValue(FlagBlock.FACING) : Direction.NORTH;
        state.size = flag.getSize();
        state.baseColor = flag.getBaseColor();
        state.patterns = flag.getPatterns();
        state.rotation = flag.getRotation();
        long gameTime = flag.getLevel() != null ? flag.getLevel().getGameTime() : 0L;
        state.time = (gameTime % 240000L) + partialTicks;
        BlockPos pos = flag.getBlockPos();
        // Neighboring flags shouldn't wave in perfect sync.
        state.seed = (float) Math.floorMod(pos.getX() * 7 + pos.getY() * 9 + pos.getZ() * 13, 100) * 0.0628F;
    }

    @Override
    public void submit(FlagRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Cloth cloth = new Cloth(state);

        // Plain cloth underneath (gives the wool texture/shading), then the dyed layers on top.
        submitLayer(collector, 0, poseStack, cloth, Sheets.BANNER_BASE, true, -1, state.lightCoords);
        submitLayer(collector, 1, poseStack, cloth, Sheets.BANNER_PATTERN_BASE, false, state.baseColor.getTextureDiffuseColor(), state.lightCoords);
        for (int i = 0; i < MAX_PATTERNS && i < state.patterns.layers().size(); i++) {
            BannerPatternLayers.Layer layer = state.patterns.layers().get(i);
            submitLayer(collector, i + 2, poseStack, cloth, Sheets.getBannerSprite(layer.pattern()), false,
                    layer.color().getTextureDiffuseColor(), state.lightCoords);
        }
    }

    private void submitLayer(SubmitNodeCollector collector, int order, PoseStack poseStack, Cloth cloth, SpriteId spriteId,
            boolean solid, int color, int light) {
        TextureAtlasSprite sprite = sprites.get(spriteId);
        collector.order(order).submitCustomGeometry(poseStack,
                solid ? spriteId.renderType(RenderTypes::entitySolid) : spriteId.renderType(RenderTypes::bannerPattern),
                (pose, buffer) -> cloth.draw(pose, buffer, sprite, color, light));
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true; // The cloth reaches several blocks past the flag's own block.
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    /** The cloth's shape for one frame, shared by every texture layer. */
    private static final class Cloth {
        private final int columns;
        private final int rows;
        private final float[][] x;
        private final float[][] y;
        private final float[][] z;
        private final float[] normalX;
        private final float[] normalZ;
        private final int rotation;

        Cloth(FlagRenderState state) {
            rotation = state.rotation & 3;
            int size = Math.max(1, state.size);
            float height = size - 0.0625F;
            float length = height * 1.5F;
            columns = Math.max(10, Math.round(length * 10.0F));
            rows = Math.max(2, size * 3);

            Direction fly = state.facing;
            Direction side = fly.getClockWise();
            float flyX = fly.getStepX();
            float flyZ = fly.getStepZ();
            float sideX = side.getStepX();
            float sideZ = side.getStepZ();

            // The pole is the fence post behind this block; hang the cloth from its outer edge.
            float hoistX = 0.5F - flyX * 0.875F;
            float hoistZ = 0.5F - flyZ * 0.875F;
            float top = 1.0F - 0.03125F;

            float wavelength = 1.1F + 0.55F * size;
            float waveNumber = (float) (Math.PI * 2.0) / wavelength;
            float speed = 0.22F;
            float amplitude = 0.11F + 0.08F * size;
            float phase = state.time * speed + state.seed;

            x = new float[columns + 1][rows + 1];
            y = new float[columns + 1][rows + 1];
            z = new float[columns + 1][rows + 1];
            normalX = new float[columns + 1];
            normalZ = new float[columns + 1];

            for (int i = 0; i <= columns; i++) {
                float across = (float) i / columns;
                float along = across * length;
                // Pinned at the pole, freer toward the end.
                float reach = across * (0.35F + 0.65F * across);
                float angle = waveNumber * along - phase;
                float sideways = amplitude * reach * (float) Math.sin(angle);
                float slope = amplitude * reach * waveNumber * (float) Math.cos(angle)
                        + amplitude * (0.35F + 1.3F * across) / length * (float) Math.sin(angle);
                // Normal of the cloth (side direction tilted by the wave's slope).
                float nx = sideX - flyX * slope;
                float nz = sideZ - flyZ * slope;
                float inverse = 1.0F / (float) Math.sqrt(nx * nx + nz * nz);
                normalX[i] = nx * inverse;
                normalZ[i] = nz * inverse;

                for (int j = 0; j <= rows; j++) {
                    float down = (float) j / rows * height;
                    // A smaller ripple running down the cloth, so it isn't perfectly stiff.
                    float flutter = 0.035F * size * across * (float) Math.sin(angle * 1.7F + down * 2.3F);
                    float offset = sideways + flutter;
                    // The free corner sags a little.
                    float sag = 0.04F * size * across * across;
                    x[i][j] = hoistX + flyX * along + sideX * offset;
                    y[i][j] = top - down - sag;
                    z[i][j] = hoistZ + flyZ * along + sideZ * offset;
                }
            }
        }

        void draw(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int light) {
            for (int i = 0; i < columns; i++) {
                for (int j = 0; j < rows; j++) {
                    // Front: top-left, bottom-left, bottom-right, top-right (counter-clockwise seen from the front).
                    vertex(pose, buffer, sprite, i, j, color, light, 1.0F);
                    vertex(pose, buffer, sprite, i, j + 1, color, light, 1.0F);
                    vertex(pose, buffer, sprite, i + 1, j + 1, color, light, 1.0F);
                    vertex(pose, buffer, sprite, i + 1, j, color, light, 1.0F);
                    // Back: the same quad wound the other way, so it's visible from behind too.
                    vertex(pose, buffer, sprite, i + 1, j, color, light, -1.0F);
                    vertex(pose, buffer, sprite, i + 1, j + 1, color, light, -1.0F);
                    vertex(pose, buffer, sprite, i, j + 1, color, light, -1.0F);
                    vertex(pose, buffer, sprite, i, j, color, light, -1.0F);
                }
            }
        }

        private void vertex(PoseStack.Pose pose, VertexConsumer buffer, TextureAtlasSprite sprite, int i, int j, int color, int light, float facing) {
            // Where this point of the cloth is: across from the pole (0-1) and down from the top (0-1).
            float across = (float) i / columns;
            float down = (float) j / rows;
            // Which point of the banner picture lands here, after turning the picture by quarter turns.
            float pictureX;
            float pictureY;
            switch (rotation) {
                case 1 -> {
                    pictureX = down;
                    pictureY = 1.0F - across;
                }
                case 2 -> {
                    pictureX = 1.0F - across;
                    pictureY = 1.0F - down;
                }
                case 3 -> {
                    pictureX = 1.0F - down;
                    pictureY = across;
                }
                default -> {
                    pictureX = across;
                    pictureY = down;
                }
            }
            buffer.addVertex(pose, x[i][j], y[i][j], z[i][j])
                    .setColor(color)
                    .setUv(sprite.getU(TEX_LEFT + TEX_WIDTH * pictureX), sprite.getV(TEX_TOP + TEX_HEIGHT * pictureY))
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, normalX[i] * facing, 0.0F, normalZ[i] * facing);
        }
    }
}
