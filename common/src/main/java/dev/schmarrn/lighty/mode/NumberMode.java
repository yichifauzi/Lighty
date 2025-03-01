// Copyright 2022-2023 The Lighty contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dev.schmarrn.lighty.mode;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.schmarrn.lighty.Lighty;
import dev.schmarrn.lighty.api.LightyColors;
import dev.schmarrn.lighty.api.LightyHelper;
import dev.schmarrn.lighty.api.LightyMode;
import dev.schmarrn.lighty.api.ModeManager;
import dev.schmarrn.lighty.config.Config;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class NumberMode extends LightyMode {
    private static final float PXL = 1/16f;
    private static final float dx = 0.25f;
    private static final float dz = 0.25f;

    private static void renderDigit(BufferBuilder builder, int digit, float x, float y, float z, int color, int lightmap) {
        float startU = (0b11 & digit) / 4f;
        float startV = ((digit >> 2) & 0b11) / 4f;

        builder.addVertex(x, y, z)
                .setColor(color)
                .setUv(startU,startV)
                .setLight(lightmap)
                .setNormal(0f, 1f, 0f);
        builder.addVertex(x, y, z + dz)
                .setColor(color)
                .setUv(startU, startV + 0.25f)
                .setLight(lightmap)
                .setNormal(0f, 1f, 0f);
        builder.addVertex(x + dx, y, z + dz)
                .setColor(color)
                .setUv(startU + 0.25f, startV + 0.25f)
                .setLight(lightmap)
                .setNormal(0f, 1f, 0f);
        builder.addVertex(x + dx, y, z)
                .setColor(color)
                .setUv(startU + 0.25f, startV)
                .setLight(lightmap)
                .setNormal(0f, 1f, 0f);
    }

    private static void renderNumber(BufferBuilder builder, int number, float x, float y, float z, int color, int lightmap) {
        int oneDigit = number % 10;
        int tenDigit = number / 10;

        if (tenDigit > 0) {
            renderDigit(builder, tenDigit, x, y, z, color, lightmap);
            renderDigit(builder, oneDigit, x + dx - PXL, y, z, color, lightmap);
        } else {
            renderDigit(builder, oneDigit, x + (dx - PXL)/2f, y, z, color, lightmap);
        }
    }

    @Override
    public void compute(ClientLevel world, BlockPos pos, BufferBuilder builder) {
        BlockPos posUp = pos.above();
        BlockState blockState = world.getBlockState(pos);
        if (LightyHelper.isBlocked(blockState, pos, world)) {
            return;
        }

        int blockLightLevel = world.getBrightness(LightLayer.BLOCK, posUp);
        int skyLightLevel = world.getBrightness(LightLayer.SKY, posUp);

        if (LightyHelper.isSafe(blockLightLevel) && !Config.SHOW_SAFE.getValue()) {
            return;
        }

        int color = LightyColors.getARGB(blockLightLevel, skyLightLevel);

        float offset = LightyHelper.getOffset(blockState, pos, world);
        if (offset == -1f) {
            return;
        }

        float x1 = pos.getX() + PXL * 5.25f;
        float y = pos.getY() + 1f + 0.005f + offset;
        float z1 = pos.getZ() + PXL * 4f;

        int overlayBrightness = Config.OVERLAY_BRIGHTNESS.getValue();
        int lightmap = LightTexture.pack(overlayBrightness, overlayBrightness);

        if (Config.SHOW_SKYLIGHT_LEVEL.getValue()) {
            renderNumber(builder, blockLightLevel, x1, y, z1, color, lightmap);
            renderNumber(builder, skyLightLevel, x1, y, z1 + PXL * 6f, color, lightmap);
        }
        else
            renderNumber(builder, blockLightLevel, x1, y, z1 + PXL * 2f, color, lightmap);
    }

    @Override
    public void beforeRendering() {
        RenderType.cutout().setupRenderState();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath(Lighty.MOD_ID, "textures/block/numbers.png"));
    }

    @Override
    public void afterRendering() {
        RenderType.cutout().clearRenderState();
        RenderSystem.disableDepthTest();
    }

    @Override
    public ResourceLocation getResourceLocation() {
        return ResourceLocation.fromNamespaceAndPath(Lighty.MOD_ID, "number_mode");
    }

    public static void init() {
        NumberMode mode = new NumberMode();
        ModeManager.registerMode(mode.getResourceLocation(), mode);
    }
}
