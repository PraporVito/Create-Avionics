package com.praporvito.createaeronauticsflaps.block;

import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.praporvito.createaeronauticsflaps.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LavaEngineBlock extends DirectionalKineticBlock implements IBE<LavaEngineBlockEntity> {

    public LavaEngineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case NORTH -> Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 2.0);
            case SOUTH -> Shapes.box(0.0, 0.0, -1.0, 1.0, 1.0, 1.0);
            case EAST -> Shapes.box(-1.0, 0.0, 0.0, 1.0, 1.0, 1.0);
            case WEST -> Shapes.box(0.0, 0.0, 0.0, 2.0, 1.0, 1.0);
            default -> Shapes.block();
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getShape(state, level, pos, CollisionContext.empty());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof LavaEngineBlockEntity engine) engine.tick();
        };
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof LavaEngineBlockEntity be) {

            if (player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    if (be.isRunning()) {
                        be.stopEngine();
                        player.displayClientMessage(Component.translatable("message.engine.stopped"), true);
                    } else {
                        be.tryStart(player);
                    }
                }
                return ItemInteractionResult.SUCCESS;
            }

            if (stack.is(Items.LAVA_BUCKET)) {
                if (be.addLava(1000)) {
                    if (!level.isClientSide) {
                        if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.displayClientMessage(Component.translatable("message.lava_engine.lava_added"), true);
                    }
                    return ItemInteractionResult.SUCCESS;
                } else {
                    if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.lava_engine.lava_full"), true);
                    return ItemInteractionResult.SUCCESS;
                }
            }

            if (stack.is(Items.WATER_BUCKET)) {
                if (be.addWater(1000)) {
                    if (!level.isClientSide) {
                        if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.displayClientMessage(Component.translatable("message.lava_engine.water_added"), true);
                    }
                    return ItemInteractionResult.SUCCESS;
                } else {
                    if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.lava_engine.water_full"), true);
                    return ItemInteractionResult.SUCCESS;
                }
            }

            if (be.getDamage() > 0 && (stack.is(Items.IRON_INGOT) || stack.getItem() instanceof WrenchItem)) {
                if (!level.isClientSide()) {
                    be.repair(player);
                    if (!player.isCreative() && stack.is(Items.IRON_INGOT)) stack.shrink(1);
                }
                return ItemInteractionResult.SUCCESS;
            }

            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new LavaEngineMenu(id, inv, be),
                        Component.translatable("gui.createaeronauticsflaps.lava_engine")
                ), buf -> buf.writeBlockPos(pos));
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (stack.getItem() instanceof WrenchItem) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof LavaEngineBlockEntity be) {
                if (player.isShiftKeyDown()) {
                    if (be.isRunning()) {
                        be.stopEngine();
                        player.displayClientMessage(Component.translatable("message.engine.stopped"), true);
                    } else {
                        be.tryStart(player);
                    }
                } else {
                    serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inv, p) -> new LavaEngineMenu(id, inv, be),
                            Component.translatable("gui.createaeronauticsflaps.lava_engine")
                    ), buf -> buf.writeBlockPos(pos));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<LavaEngineBlockEntity> getBlockEntityClass() { return LavaEngineBlockEntity.class; }

    @Override
    public BlockEntityType<? extends LavaEngineBlockEntity> getBlockEntityType() { return ModBlockEntities.LAVA_ENGINE_BE.get(); }
}