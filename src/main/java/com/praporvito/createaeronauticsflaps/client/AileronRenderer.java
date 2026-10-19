package com.praporvito.createaeronauticsflaps.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.praporvito.createaeronauticsflaps.block.AileronBlock;
import com.praporvito.createaeronauticsflaps.block.AileronBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class AileronRenderer extends SmartBlockEntityRenderer<AileronBlockEntity> {

    public AileronRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(AileronBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        // Далее рендерим модель элерона (без изменений)
        BlockState state = be.getBlockState();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        int calculatedLight = packedLight;
        if (be.getLevel() != null) {
            int blockLight = be.getLevel().getBrightness(LightLayer.BLOCK, be.getBlockPos());
            int skyLight = be.getLevel().getBrightness(LightLayer.SKY, be.getBlockPos());
            calculatedLight = LightTexture.pack(blockLight, skyLight);
        }

        BakedModel baseModel = dispatcher.getBlockModel(state);
        dispatcher.getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                state,
                baseModel,
                1.0F, 1.0F, 1.0F,
                calculatedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );

        boolean isRight = state.getValue(AileronBlock.SIDE) == AileronBlock.AileronSide.RIGHT;
        String sailName = isRight ? "aileron_inverted" : "aileron";

        ModelResourceLocation sailLoc = new ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath("createaeronauticsflaps", "block/" + sailName),
                "standalone"
        );
        BakedModel sailModel = Minecraft.getInstance().getModelManager().getModel(sailLoc);

        if (sailModel == Minecraft.getInstance().getModelManager().getMissingModel()) {
            return;
        }

        float angle = Mth.lerp(partialTick, be.getPrevAngle(), be.getCurrentAngle());

        poseStack.pushPose();

        Direction facing = state.getValue(AileronBlock.FACING);
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

        double pivotX = (isRight ? 9.375 : 6.375) / 16.0;
        double pivotY = 7.5 / 16.0;
        double pivotZ = 14.75 / 16.0;

        poseStack.translate(pivotX, pivotY, pivotZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(-angle));
        poseStack.translate(-pivotX, -pivotY, -pivotZ);

        dispatcher.getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                state,
                sailModel,
                1.0F, 1.0F, 1.0F,
                calculatedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );

        poseStack.popPose();
    }
}