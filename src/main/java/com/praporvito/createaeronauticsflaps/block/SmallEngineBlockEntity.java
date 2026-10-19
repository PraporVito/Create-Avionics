package com.praporvito.createaeronauticsflaps.block;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.praporvito.createaeronauticsflaps.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class SmallEngineBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {

    private final ItemStack[] frequencies = {ItemStack.EMPTY, ItemStack.EMPTY};
    private LinkBehaviour linkThrottle;
    private ScrollValueBehaviour reversed;

    private ItemStack fuel = ItemStack.EMPTY;
    private int burnTime = 0;
    private int burnDuration = 0;
    private int throttle = 0;
    private boolean isRunning = false;

    private int damage = 0;
    private int misfireTicks = 0;
    private int lastThrottle = 0;

    public static final int MAX_WATER = 4000;
    private int waterAmount = 0;
    private int overheatTicks = 0;

    public SmallEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMALL_ENGINE_BE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        Pair<ValueBoxTransform, ValueBoxTransform> slots = Pair.of(new EmptyBox(), new EmptyBox());
        linkThrottle = LinkBehaviour.receiver(this, slots, this::setThrottleFromLink);
        behaviours.add(linkThrottle);

        reversed = new ScrollValueBehaviour(
                Component.translatable("gui.createaeronauticsflaps.small_engine.reverse"),
                this,
                new TopValueBoxTransform()
        );
        reversed.between(0, 1);
        reversed.value = 0;
        reversed.withCallback(this::onReversed);
        behaviours.add(reversed);
    }

    private void onReversed(Integer state) {
        updateGeneratedRotation();
        setChanged();
        notifyUpdate();
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, state == 1 ? 1.2f : 0.8f);
        }
    }

    private void setThrottleFromLink(int power) {
        if (this.throttle == power) return;
        this.throttle = power;
        setChanged();
        notifyUpdate();
    }

    public void setFrequency(boolean first, ItemStack stack) {
        int index = first ? 0 : 1;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        if (ItemStack.isSameItemSameComponents(this.frequencies[index], copy)) return;
        this.frequencies[index] = copy;
        if (linkThrottle != null) {
            linkThrottle.setFrequency(first, copy);
        }
        setChanged();
        notifyUpdate();
    }

    public ItemStack getFrequency(boolean first) {
        return this.frequencies[first ? 0 : 1];
    }

    public void addWater(int amount) {
        this.waterAmount = Math.min(MAX_WATER, this.waterAmount + amount);
        setChanged();
        notifyUpdate();
    }

    public int getWaterAmount() { return waterAmount; }

    public void tryStart(Player player) {
        if (damage >= 100) {
            player.displayClientMessage(Component.translatable("message.engine.broken"), true);
            return;
        }
        if (isRunning) return;
        if (fuel.isEmpty() && burnTime <= 0) {
            player.displayClientMessage(Component.translatable("message.engine.no_fuel"), true);
            return;
        }
        if (waterAmount <= 0) {
            player.displayClientMessage(Component.translatable("message.engine.no_water"), true);
            return;
        }

        float startChance = 0.8f - (damage / 200.0f);
        if (level.random.nextFloat() < startChance) {
            isRunning = true;
            level.playSound(null, worldPosition, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0f, 1.2f);
            level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8f, 0.8f);
            player.displayClientMessage(Component.translatable("message.engine.started"), true);
            if (burnTime <= 0) {
                burnTime = fuel.getBurnTime(null);
                burnDuration = burnTime;
                fuel.shrink(1);
            }
            updateGeneratedRotation();
        } else {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5f, 1.5f);
            player.displayClientMessage(Component.translatable("message.engine.misfire"), true);
        }
        setChanged();
        notifyUpdate();
    }

    public boolean isRunning() { return isRunning; }

    public void stopEngine() {
        if (!isRunning) return;
        isRunning = false;
        overheatTicks = 0;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.8f);
            level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0f, 0.5f);
        }
        updateGeneratedRotation();
        setChanged();
        notifyUpdate();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;

        if (level.isClientSide) {
            if (isRunning) {
                Direction facing = getBlockState().getValue(SmallEngineBlock.FACING);
                // Вектор направления "назад" (противоположно FACING)
                double backX = -facing.getStepX();
                double backZ = -facing.getStepZ();

                // Спавним частицы чуть позади центра блока
                double spawnX = worldPosition.getX() + 0.5 + backX * 0.6;
                double spawnY = worldPosition.getY() + 0.5;
                double spawnZ = worldPosition.getZ() + 0.5 + backZ * 0.6;

                int particleCount = 1 + (throttle / 5); // Чем больше тяга, тем больше дыма
                for (int i = 0; i < particleCount; i++) {
                    // Импульс назад + легкий разброс
                    double velX = backX * (0.05 + level.random.nextDouble() * 0.1);
                    double velY = 0.02 + level.random.nextDouble() * 0.05;
                    double velZ = backZ * (0.05 + level.random.nextDouble() * 0.1);

                    if (misfireTicks > 0) {
                        // При троении - густой дым и искры
                        level.addParticle(ParticleTypes.LARGE_SMOKE, spawnX, spawnY, spawnZ, velX, velY, velZ);
                        if (level.random.nextFloat() < 0.3f) {
                            level.addParticle(ParticleTypes.FLAME, spawnX, spawnY, spawnZ, velX, velY, velZ);
                        }
                    } else {
                        // Обычный дым
                        level.addParticle(ParticleTypes.SMOKE, spawnX, spawnY, spawnZ, velX, velY, velZ);
                    }
                }

                // Дым при перегреве (нет воды)
                if (overheatTicks > 0 && level.random.nextFloat() < 0.3f) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 0, 0.2, 0);
                }

                playRunningSounds();
            }
            return;
        }

        if (isRunning) {
            if (waterAmount <= 0) {
                overheatTicks++;
                if (overheatTicks % 10 == 0) {
                    level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.5f);
                }
                if (overheatTicks >= 60) {
                    level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 1.5f, Level.ExplosionInteraction.BLOCK);
                    level.removeBlock(worldPosition, false);
                    return;
                }
            } else {
                overheatTicks = 0;
                if (level.getGameTime() % 4 == 0) {
                    waterAmount--;
                    if (waterAmount % 100 == 0) {
                        setChanged();
                        notifyUpdate();
                    }
                }
            }
        }

        if (misfireTicks > 0) {
            misfireTicks--;
            if (level.getGameTime() % 10 == 0) {
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 0.5f);
            }
        }

        boolean throttleChanged = false;
        if (this.frequencies[0].isEmpty() && this.frequencies[1].isEmpty()) {
            int redstone = level.getBestNeighborSignal(worldPosition);
            if (this.throttle != redstone) {
                this.throttle = redstone;
                throttleChanged = true;
            }
        }

        if (isRunning) {
            if (burnTime > 0) {
                int consumption = 1 + (throttle / 5);
                burnTime -= consumption;
                if (burnTime % 20 == 0) notifyUpdate();
                if (burnTime <= 0) {
                    burnTime = 0;
                    if (!fuel.isEmpty()) {
                        int duration = fuel.getBurnTime(null);
                        if (duration > 0) {
                            burnTime = duration;
                            burnDuration = duration;
                            fuel.shrink(1);
                            setChanged();
                            notifyUpdate();
                        } else {
                            triggerMisfire();
                            damage += 10;
                            stopEngine();
                        }
                    } else {
                        triggerMisfire();
                        damage += 10;
                        stopEngine();
                    }
                }
            } else if (!fuel.isEmpty()) {
                int duration = fuel.getBurnTime(null);
                if (duration > 0) {
                    burnTime = duration;
                    burnDuration = duration;
                    fuel.shrink(1);
                    setChanged();
                    notifyUpdate();
                }
            } else {
                triggerMisfire();
                damage += 10;
                stopEngine();
            }

            if (level.getGameTime() % 20 == 0) {
                if (throttleChanged && Math.abs(throttle - lastThrottle) > 6) { triggerMisfire(); damage += 2; }
                if (throttle >= 14 && level.random.nextFloat() < 0.01f) { damage += 1; }
                if (damage >= 100) {
                    isRunning = false;
                    misfireTicks = 0;
                    level.playSound(null, worldPosition, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 1.0f, 0.5f);
                    level.explode(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 1.0f, Level.ExplosionInteraction.NONE);
                }
            }
            lastThrottle = throttle;
        }

        if (throttleChanged || getSpeed() != getGeneratedSpeed()) {
            updateGeneratedRotation();
        }
    }

    private void triggerMisfire() {
        if (misfireTicks > 0) return;
        misfireTicks = 40;
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.5f);
    }

    private void playRunningSounds() {
        long gameTime = level.getGameTime();
        float speed = Math.abs(getGeneratedSpeed());
        int rpm = (int) speed;

        int pistonInterval = Math.max(2, 20 - (rpm / 7));
        if (gameTime % pistonInterval == 0) {
            float pitch = 0.6f + (rpm / 128.0f) * 0.6f + level.random.nextFloat() * 0.1f;
            level.playLocalSound(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.2f, pitch, false);
        }

        if (gameTime % 20 == 0) {
            float pitch = 0.8f + level.random.nextFloat() * 0.4f;
            level.playLocalSound(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.4f, pitch, false);
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (!isRunning || damage >= 100) return 0;

        float baseSpeed;
        if (throttle <= 0) baseSpeed = 16.0f;
        else if (throttle >= 15) baseSpeed = 128.0f;
        else baseSpeed = 16.0f + ((float)throttle / 15.0f) * 112.0f;

        if (misfireTicks > 0) {
            baseSpeed *= 0.3f;
        }

        return reversed != null && reversed.getValue() == 1 ? -baseSpeed : baseSpeed;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float speed = Math.abs(getGeneratedSpeed());
        if (speed <= 0) return 0;
        return 4096.0f / speed;
    }

    @Override
    public boolean isSource() {
        return true;
    }

    public boolean repair(Player player) {
        if (damage <= 0) return false;
        damage = 0;
        misfireTicks = 0;
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable("message.engine.repaired"), true);
        setChanged();
        notifyUpdate();
        return true;
    }

    public void addFuel(ItemStack stack) {
        if (stack.getBurnTime(null) > 0) {
            if (fuel.isEmpty()) {
                fuel = stack.copyWithCount(1);
            } else if (ItemStack.isSameItemSameComponents(fuel, stack) && fuel.getCount() < 64) {
                fuel.grow(1);
            }
            setChanged();
            notifyUpdate();
        }
    }

    public void setFuel(ItemStack stack) {
        this.fuel = stack;
        setChanged();
        notifyUpdate();
    }

    public ItemStack getFuel() { return fuel; }
    public int getBurnTime() { return burnTime; }
    public int getBurnDuration() { return burnDuration; }
    public int getDamage() { return damage; }
    public int getMisfireTicks() { return misfireTicks; }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.translatable("tooltip.engine.stats").withStyle(ChatFormatting.GOLD));

        ChatFormatting waterColor = waterAmount > 1000 ? ChatFormatting.AQUA : waterAmount > 0 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        tooltip.add(Component.translatable("tooltip.engine.water", waterAmount, MAX_WATER).withStyle(waterColor));
        if (overheatTicks > 0) {
            tooltip.add(Component.translatable("tooltip.engine.overheating"));
        }

        tooltip.add(Component.translatable("tooltip.engine.fuel", fuel.isEmpty() ? Component.translatable("tooltip.engine.empty") : fuel.getHoverName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.engine.time", burnTime / 20).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.engine.stress", 4096).withStyle(ChatFormatting.AQUA));

        ChatFormatting damageColor = damage > 70 ? ChatFormatting.RED : damage > 30 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
        tooltip.add(Component.translatable("tooltip.engine.damage", damage).withStyle(damageColor));

        if (misfireTicks > 0) {
            tooltip.add(Component.translatable("tooltip.engine.status", Component.translatable("tooltip.engine.status_misfiring")).withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.engine.status", Component.translatable(isRunning ? "tooltip.engine.status_running" : "tooltip.engine.status_stopped")).withStyle(isRunning ? ChatFormatting.GREEN : ChatFormatting.RED));
        }

        tooltip.add(Component.translatable("tooltip.engine.signal", throttle).withStyle(ChatFormatting.YELLOW));
        int speed = (int)getGeneratedSpeed();
        tooltip.add(Component.translatable("tooltip.engine.speed", Math.abs(speed), speed < 0 ? Component.translatable("tooltip.engine.reversed") : "").withStyle(ChatFormatting.GREEN));
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Fuel", fuel.saveOptional(registries));
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        tag.putBoolean("IsRunning", isRunning);
        tag.putInt("Throttle", throttle);
        tag.putInt("Damage", damage);
        tag.putInt("MisfireTicks", misfireTicks);
        tag.put("Freq1", frequencies[0].saveOptional(registries));
        tag.put("Freq2", frequencies[1].saveOptional(registries));
        tag.putInt("WaterAmount", waterAmount);
        tag.putInt("OverheatTicks", overheatTicks);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        fuel = tag.contains("Fuel") ? ItemStack.parseOptional(registries, tag.getCompound("Fuel")) : ItemStack.EMPTY;
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
        isRunning = tag.getBoolean("IsRunning");
        throttle = tag.getInt("Throttle");
        damage = tag.getInt("Damage");
        misfireTicks = tag.getInt("MisfireTicks");
        frequencies[0] = tag.contains("Freq1") ? ItemStack.parseOptional(registries, tag.getCompound("Freq1")) : ItemStack.EMPTY;
        frequencies[1] = tag.contains("Freq2") ? ItemStack.parseOptional(registries, tag.getCompound("Freq2")) : ItemStack.EMPTY;
        waterAmount = tag.getInt("WaterAmount");
        overheatTicks = tag.getInt("OverheatTicks");
    }

    private static class EmptyBox extends ValueBoxTransform {
        @Override public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) { return new Vec3(0, -100, 0); }
        @Override public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {}
        @Override public float getScale() { return 0f; }
    }

    public static class TopValueBoxTransform extends ValueBoxTransform {
        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            return new Vec3(0.5, 1.2, 0.5);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            ms.mulPose(Axis.XP.rotationDegrees(90));
        }

        @Override
        public float getScale() {
            return 0.5f;
        }
    }
}