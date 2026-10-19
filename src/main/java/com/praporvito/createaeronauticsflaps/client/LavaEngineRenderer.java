package com.praporvito.createaeronauticsflaps.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.praporvito.createaeronauticsflaps.block.LavaEngineBlock;
import com.praporvito.createaeronauticsflaps.block.LavaEngineBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class LavaEngineRenderer extends SmartBlockEntityRenderer<LavaEngineBlockEntity> {

    public LavaEngineRenderer(BlockEntityRendererProvider.Context context) { super(context); }

    @Override
    protected void renderSafe(LavaEngineBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        int calculatedLight = packedLight;
        if (be.getLevel() != null) {
            int blockLight = be.getLevel().getBrightness(LightLayer.BLOCK, be.getBlockPos());
            int skyLight = be.getLevel().getBrightness(LightLayer.SKY, be.getBlockPos());
            calculatedLight = LightTexture.pack(blockLight, skyLight);
        }

        poseStack.pushPose();

        Direction facing = state.getValue(LavaEngineBlock.FACING);
        // Крутим модель, чтобы она смотрела в правильную сторону. Никаких смещений!
        if (facing == Direction.SOUTH) {
            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-0.5, 0, -0.5);
        } else if (facing == Direction.EAST) {
            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(270));
            poseStack.translate(-0.5, 0, -0.5);
        } else if (facing == Direction.WEST) {
            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
            poseStack.translate(-0.5, 0, -0.5);
        }

        dispatcher.getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                state,
                dispatcher.getBlockModel(state.getBlock().defaultBlockState()),
                1.0F, 1.0F, 1.0F,
                calculatedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );

        poseStack.popPose();
    }
}