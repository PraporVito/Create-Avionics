package com.praporvito.createaeronauticsflaps.block;

import com.praporvito.createaeronauticsflaps.ModBlockEntities;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class AileronExtensionBlockEntity extends SmartBlockEntity {
    private float currentAngle = 0.0f;
    private float prevAngle = 0.0f;

    public AileronExtensionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AILERON_EXTENSION_BE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void tick() {
        super.tick();
        prevAngle = currentAngle;

        if (level == null || level.isClientSide) return;

        AileronBlockEntity parent = findParent(level, worldPosition);
        float targetAngle = parent != null ? parent.getCurrentAngle() : 0.0f;

        if (Math.abs(currentAngle - targetAngle) > 0.01f) {
            currentAngle = targetAngle;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static AileronBlockEntity findParent(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AileronExtensionBlock)) return null;

        Direction facing = state.getValue(AileronExtensionBlock.FACING);

        AileronBlockEntity parent = searchInDirection(level, pos, facing.getCounterClockWise(), facing);
        if (parent != null) return parent;

        return searchInDirection(level, pos, facing.getClockWise(), facing);
    }

    private static AileronBlockEntity searchInDirection(Level level, BlockPos startPos, Direction dir, Direction baseFacing) {
        BlockPos.MutableBlockPos checkPos = startPos.mutable();
        for (int i = 0; i < 32; i++) {
            checkPos.move(dir);
            BlockState state = level.getBlockState(checkPos);

            if (state.getBlock() instanceof AileronBlock) {
                if (state.getValue(AileronBlock.FACING) == baseFacing) {
                    BlockEntity be = level.getBlockEntity(checkPos);
                    if (be instanceof AileronBlockEntity aileron) {
                        if (AileronBlock.getExtensionDirection(state) == dir.getOpposite()) {
                            return aileron;
                        }
                    }
                }
                break;
            } else if (!(state.getBlock() instanceof AileronExtensionBlock)) {
                break;
            }
        }
        return null;
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putFloat("Angle", currentAngle);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        currentAngle = compound.getFloat("Angle");
    }

    public float getCurrentAngle() { return currentAngle; }
    public float getPrevAngle() { return prevAngle; }
}