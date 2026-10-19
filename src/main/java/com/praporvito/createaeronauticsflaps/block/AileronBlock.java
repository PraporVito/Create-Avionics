package com.praporvito.createaeronauticsflaps.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Locale;

public class AileronBlock extends DirectionalBlock implements EntityBlock, IWrenchable, BlockSubLevelLiftProvider {

    public static final MapCodec<AileronBlock> CODEC = simpleCodec(AileronBlock::new);
    public static final EnumProperty<AileronSide> SIDE = EnumProperty.create("side", AileronSide.class);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final IntegerProperty ANGLE = IntegerProperty.create("angle", 0, 60);

    public AileronBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SIDE, AileronSide.RIGHT)
                .setValue(POWERED, false)
                .setValue(ANGLE, 30));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE, POWERED, ANGLE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public static Direction getExtensionDirection(BlockState state) {
        Direction facing = state.getValue(FACING);
        AileronSide side = state.getValue(SIDE);
        return side == AileronSide.LEFT ? facing.getCounterClockWise() : facing.getClockWise();
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

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof WrenchItem) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AileronBlockEntity be) return be.handleInteraction(player, stack);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getPlayer() != null && !context.getLevel().isClientSide) {
            if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof AileronBlockEntity be) {
                if (be.getWear() > 0.1f || be.getStress() > 0.1f || be.getStuckTicks() > 0) {
                    be.repair(context.getPlayer());
                } else {
                    be.toggleSetup(context.getPlayer());
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            AileronSide newSide = state.getValue(SIDE) == AileronSide.LEFT ? AileronSide.RIGHT : AileronSide.LEFT;
            context.getLevel().setBlockAndUpdate(context.getClickedPos(), state.setValue(SIDE, newSide));
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AileronBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> { if (be instanceof AileronBlockEntity aileron) aileron.tick(); };
    }

    @Override
    public @NotNull Direction sable$getNormal(@NotNull BlockState state) { return state.getValue(FACING); }

    @Override
    public float sable$getLiftScalar() { return 0.0f; }

    @Override
    public float sable$getParallelDragScalar() { return 0.4375f; }

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
        float angleRad = (float) -Math.toRadians(ctx.state().getValue(ANGLE) - 30);
        if (Math.abs(angleRad) < 0.001f) return;

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

        float k2 = 0.08610252826f;
        Vector3d directionlessDrag = new Vector3d(localAirflow)
                .mul(-k2 * airPressure * timeStep);

        Vector3d totalForce = normalDrag.add(directionlessDrag);
        linearImpulse.sub(totalForce);

        Vector3d moment = new Vector3d(relativePos).cross(totalForce);
        angularImpulse.sub(moment);
    }

    public enum AileronSide implements StringRepresentable {
        LEFT, RIGHT;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
}