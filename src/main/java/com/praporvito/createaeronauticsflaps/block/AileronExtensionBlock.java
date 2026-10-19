package com.praporvito.createaeronauticsflaps.block;

import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class AileronExtensionBlock extends DirectionalBlock implements EntityBlock, BlockSubLevelLiftProvider {

    public static final MapCodec<AileronExtensionBlock> CODEC = simpleCodec(AileronExtensionBlock::new);

    public AileronExtensionBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        for (Direction horizontalDir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(horizontalDir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.getBlock() instanceof AileronBlock) {
                Direction aileronFacing = neighborState.getValue(AileronBlock.FACING);
                if (horizontalDir.getAxis() != aileronFacing.getAxis()) {
                    return this.defaultBlockState().setValue(FACING, aileronFacing);
                }
            } else if (neighborState.getBlock() instanceof AileronExtensionBlock) {
                Direction extFacing = neighborState.getValue(FACING);
                if (horizontalDir.getAxis() != extFacing.getAxis()) {
                    return this.defaultBlockState().setValue(FACING, extFacing);
                }
            }
        }

        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 0; }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) { return true; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.box(0.0, 0.25, 0.0, 1.0, 0.75, 1.0);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AileronExtensionBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (lvl, pos, st, be) -> {
            if (be instanceof AileronExtensionBlockEntity ext) {
                ext.tick();
            }
        };
    }

    // ==========================================
    // ФИЗИКА SABLE — SYMMETRIC SAIL (+25% усиление)
    // ==========================================

    @Override
    public @NotNull Direction sable$getNormal(@NotNull BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public float sable$getLiftScalar() {
        return 0.0f;
    }

    @Override
    public float sable$getParallelDragScalar() {
        return 0.4375f; // 0.35f + 25%
    }

    private static Vector3d getHingeAxis(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vector3d( 1, 0,  0);
            case SOUTH -> new Vector3d(-1, 0,  0);
            case EAST  -> new Vector3d( 0, 0,  1);
            case WEST  -> new Vector3d( 0, 0, -1);
            default    -> new Vector3d( 1, 0,  0);
        };
    }

    @Override
    public void sable$contributeLiftAndDrag(
            @NotNull LiftProviderContext ctx,
            @NotNull ServerSubLevel subLevel,
            @Nullable Pose3d localPose,
            double timeStep,
            @NotNull Vector3dc linearVelocity,
            @NotNull Vector3dc angularVelocity,
            @NotNull Vector3d linearImpulse,
            @NotNull Vector3d angularImpulse,
            @Nullable LiftProviderGroup group
    ) {
        if (subLevel.getLevel() == null) return;

        if (!(subLevel.getLevel().getBlockEntity(ctx.pos()) instanceof AileronExtensionBlockEntity ext)) return;

        float currentAngle = ext.getCurrentAngle();
        if (Math.abs(currentAngle) < 0.5f) return;

        float angleRad = (float) -Math.toRadians(currentAngle);

        Direction facing = ctx.state().getValue(FACING);
        Vector3d hingeAxis = getHingeAxis(facing);

        Vector3d normal;
        if (facing.getAxis() == Direction.Axis.Z) {
            normal = new Vector3d(0, 1, 0);
        } else {
            normal = new Vector3d(facing.getStepX(), 0, 0);
        }
        normal.rotateAxis(angleRad, hingeAxis.x, hingeAxis.y, hingeAxis.z);
        normal.normalize();

        Vector3d blockCenter = new Vector3d(
                ctx.pos().getX() + 0.5,
                ctx.pos().getY() + 0.5,
                ctx.pos().getZ() + 0.5
        );

        if (localPose != null) {
            localPose.transformNormal(normal);
            localPose.transformPosition(blockCenter);
        }

        double airPressure = DimensionPhysicsData.getAirPressure(
                subLevel.getLevel(),
                subLevel.logicalPose().transformPosition(blockCenter, new Vector3d())
        );

        Vector3d com = new Vector3d(subLevel.getMassTracker().getCenterOfMass());
        Vector3d relativePos = new Vector3d(blockCenter).sub(com);
        Vector3d localAirflow = new Vector3d(linearVelocity)
                .add(new Vector3d(angularVelocity).cross(relativePos));

        double nDotV = normal.dot(localAirflow);
        Vector3d normalDrag = new Vector3d(normal)
                .mul(-sable$getParallelDragScalar() * nDotV * airPressure * timeStep);

        float k2 = 0.08610252826f; // 0.06888202261f + 25%
        Vector3d directionlessDrag = new Vector3d(localAirflow)
                .mul(-k2 * airPressure * timeStep);

        Vector3d totalForce = normalDrag.add(directionlessDrag);
        linearImpulse.sub(totalForce);

        Vector3d moment = new Vector3d(relativePos).cross(totalForce);
        angularImpulse.sub(moment);
    }
}