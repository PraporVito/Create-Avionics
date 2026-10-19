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
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SmallEngineBlock extends DirectionalKineticBlock implements IBE<SmallEngineBlockEntity> {

    public SmallEngineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof SmallEngineBlockEntity engine) {
                engine.tick();
            }
        };
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SmallEngineBlockEntity engine) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), engine.getFuel());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {

        if (level.getBlockEntity(pos) instanceof SmallEngineBlockEntity be) {

            if (stack.is(Items.WATER_BUCKET)) {
                if (be.getWaterAmount() < SmallEngineBlockEntity.MAX_WATER) {
                    if (!level.isClientSide) {
                        be.addWater(1000);
                        if (!player.isCreative()) {
                            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                        }
                        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                        player.displayClientMessage(Component.translatable("message.small_engine.water_added"), true);
                    }
                    return ItemInteractionResult.SUCCESS;
                } else {
                    if (!level.isClientSide) {
                        player.displayClientMessage(Component.translatable("message.small_engine.water_full"), true);
                    }
                    return ItemInteractionResult.SUCCESS;
                }
            }

            if (be.getDamage() > 0) {
                if (stack.is(Items.IRON_INGOT) || stack.getItem() instanceof WrenchItem) {
                    if (!level.isClientSide()) {
                        be.repair(player);
                        if (!player.isCreative() && stack.is(Items.IRON_INGOT)) {
                            stack.shrink(1);
                        }
                    }
                    return ItemInteractionResult.SUCCESS;
                } else if (be.getDamage() >= 100) {
                    if (!level.isClientSide()) {
                        player.displayClientMessage(Component.translatable("message.small_engine.needs_iron"), true);
                    }
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }

        if (stack.getItem() instanceof WrenchItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof SmallEngineBlockEntity be) {
                if (player.isShiftKeyDown()) {
                    if (be.isRunning()) {
                        be.stopEngine();
                        player.displayClientMessage(Component.translatable("message.engine.stopped"), true);
                    } else {
                        be.tryStart(player);
                    }
                } else {
                    serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inv, p) -> new SmallEngineMenu(id, inv, be),
                            Component.translatable("gui.createaeronauticsflaps.small_engine")
                    ), buf -> buf.writeBlockPos(pos));
                }
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof SmallEngineBlockEntity be) {
                if (player.isShiftKeyDown()) {
                    if (be.isRunning()) {
                        be.stopEngine();
                        player.displayClientMessage(Component.translatable("message.engine.stopped"), true);
                    } else {
                        be.tryStart(player);
                    }
                } else {
                    serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inv, p) -> new SmallEngineMenu(id, inv, be),
                            Component.translatable("gui.createaeronauticsflaps.small_engine")
                    ), buf -> buf.writeBlockPos(pos));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<SmallEngineBlockEntity> getBlockEntityClass() {
        return SmallEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SmallEngineBlockEntity> getBlockEntityType() {
        return ModBlockEntities.SMALL_ENGINE_BE.get();
    }
}