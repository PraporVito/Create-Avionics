package com.praporvito.createaeronauticsflaps.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
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

public class LavaEngineBlockEntity extends GeneratingKineticBlockEntity implements IHaveGoggleInformation {

    public static final int MAX_WATER = 16000;
    public static final int MAX_LAVA = 8000;

    private final ItemStack[] frequencies = {ItemStack.EMPTY, ItemStack.EMPTY};
    private LinkBehaviour linkThrottle;
    private ScrollValueBehaviour reversed;

    private int waterAmount = 0;
    private int lavaAmount = 0;

    private int throttle = 0;
    private boolean isRunning = false;
    private int damage = 0;
    private int overheatTicks = 0;

    public LavaEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAVA_ENGINE_BE.get(), pos, state);
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
        if (linkThrottle != null) linkThrottle.setFrequency(first, copy);
        setChanged();
        notifyUpdate();
    }

    public ItemStack getFrequency(boolean first) {
        return this.frequencies[first ? 0 : 1];
    }

    public boolean addWater(int amount) {
        if (waterAmount + amount <= MAX_WATER) {
            waterAmount += amount; setChanged(); notifyUpdate(); return true;
        }
        return false;
    }

    public boolean addLava(int amount) {
        if (lavaAmount + amount <= MAX_LAVA) {
            lavaAmount += amount; setChanged(); notifyUpdate(); return true;
        }
        return false;
    }

    public void tryStart(Player player) {
        if (damage >= 100) {
            player.displayClientMessage(Component.translatable("message.engine.broken"), true); return;
        }
        if (lavaAmount < 100) {
            player.displayClientMessage(Component.translatable("message.engine.no_fuel"), true); return;
        }
        if (waterAmount < 100) {
            player.displayClientMessage(Component.translatable("message.engine.no_water"), true); return;
        }

        isRunning = true;
        player.displayClientMessage(Component.translatable("message.lava_engine.started"), true);
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0f, 0.5f);
        }
        updateGeneratedRotation();
        setChanged();
        notifyUpdate();
    }

    public boolean isRunning() { return isRunning; }

    public void stopEngine() {
        if (!isRunning) return;
        isRunning = false;
        overheatTicks = 0;
        updateGeneratedRotation();
        setChanged();
        notifyUpdate();
    }

    public void repair(Player player) {
        if (damage <= 0) return;
        damage = 0;
        player.displayClientMessage(Component.translatable("message.engine.repaired"), true);
        setChanged();
        notifyUpdate();
    }

    public int getDamage() { return damage; }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;

        if (level.isClientSide) {
            if (isRunning) {
                Direction facing = getBlockState().getValue(LavaEngineBlock.FACING);
                // Направление "назад" от FACING
                double backX = -facing.getStepX();
                double backZ = -facing.getStepZ();

                // СМЕЩЕНИЕ К ЛЮКУ (0.8 - это примерно середина 2-блочной модели)
                double smokeOffset = 0.8;

                // Позиция спавна (центр блока + смещение к люку)
                double spawnX = worldPosition.getX() + 0.5 + backX * smokeOffset;
                double spawnY = worldPosition.getY() + 0.8; // Чуть выше, чтобы вылетало из люка
                double spawnZ = worldPosition.getZ() + 0.5 + backZ * smokeOffset;

                int particleCount = 2 + (throttle / 4); // Чем больше тяга, тем гуще дым
                for (int i = 0; i < particleCount; i++) {
                    // Импульс частиц: немного назад и вверх
                    double velX = backX * (0.05 + level.random.nextDouble() * 0.1);
                    double velY = 0.1 + level.random.nextDouble() * 0.1;
                    double velZ = backZ * (0.05 + level.random.nextDouble() * 0.1);

                    level.addParticle(ParticleTypes.LARGE_SMOKE, spawnX, spawnY, spawnZ, velX, velY, velZ);

                    // ПЕРДЕЖ ОГНЕМ ПРИ ТЯГЕ > 13
                    if (throttle > 13) {
                        if (level.random.nextFloat() < 0.3f) {
                            level.addParticle(ParticleTypes.FLAME, spawnX, spawnY, spawnZ, velX, velY, velZ);
                        }
                        if (level.random.nextFloat() < 0.1f) {
                            level.addParticle(ParticleTypes.LAVA, spawnX, spawnY, spawnZ, velX, velY, velZ);
                        }
                    }
                }

                // Дым/лава при перегреве (если кончилась вода)
                if (overheatTicks > 0) {
                    level.addParticle(ParticleTypes.LAVA, spawnX, spawnY, spawnZ, 0, 0.1, 0);
                    if (level.random.nextFloat() < 0.5f) {
                        level.addParticle(ParticleTypes.LARGE_SMOKE, spawnX, spawnY, spawnZ, 0, 0.2, 0);
                    }
                }

                playRunningSounds();
            }
            return;
        }

        boolean throttleChanged = false;
        if (this.frequencies[0].isEmpty() && this.frequencies[1].isEmpty()) {
            int redstone = level.getBestNeighborSignal(worldPosition);
            if (this.throttle != redstone) {
                this.throttle = redstone;
                throttleChanged = true;
            }
        }

        if (!isRunning) return;

        if (lavaAmount <= 0) {
            stopEngine();
            return;
        }

        if (waterAmount > 0) {
            waterAmount--;
            overheatTicks = 0;
        } else {
            overheatTicks++;
            if (overheatTicks % 20 == 0) {
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.5f);
            }
            if (overheatTicks >= 100) {
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 4.0f, Level.ExplosionInteraction.BLOCK);
                level.removeBlock(worldPosition, false);
                return;
            }
        }

        if (level.getGameTime() % 2 == 0) {
            lavaAmount--;
        }

        if (level.getGameTime() % 5 == 0) {
            setChanged();
            notifyUpdate();
        }

        if (level.getGameTime() % 1200 == 0) {
            damage += 1;
            if (damage >= 100) stopEngine();
        }

        if (throttleChanged || getSpeed() != getGeneratedSpeed()) {
            updateGeneratedRotation();
        }
    }

    private void playRunningSounds() {
        long gameTime = level.getGameTime();
        float speed = Math.abs(getGeneratedSpeed());
        if (speed <= 0) return;

        int pistonInterval = Math.max(2, 20 - ((int)speed / 10));
        if (gameTime % pistonInterval == 0) {
            float pitch = 0.4f + (speed / 192.0f) * 0.2f + level.random.nextFloat() * 0.1f;
            level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8f, pitch, false);
        }

        if (gameTime % 20 == 0) {
            float pitch = 0.8f + level.random.nextFloat() * 0.4f;
            level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6f, pitch, false);
        }
    }

    @Override
    public float getGeneratedSpeed() {
        if (!isRunning || damage >= 100) return 0;

        float baseSpeed;
        if (throttle <= 0) baseSpeed = 16.0f;
        else if (throttle >= 15) baseSpeed = 192.0f;
        else baseSpeed = 16.0f + ((float)throttle / 15.0f) * 176.0f;

        return reversed != null && reversed.getValue() == 1 ? -baseSpeed : baseSpeed;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float speed = Math.abs(getGeneratedSpeed());
        if (speed <= 0) return 0;
        return 8192.0f / speed;
    }

    @Override
    public boolean isSource() { return true; }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.translatable("tooltip.lava_engine.stats").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.engine.lava", lavaAmount, MAX_LAVA).withStyle(ChatFormatting.GOLD));

        ChatFormatting waterColor = waterAmount > 1000 ? ChatFormatting.AQUA : waterAmount > 0 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        tooltip.add(Component.translatable("tooltip.engine.water", waterAmount, MAX_WATER).withStyle(waterColor));

        if (overheatTicks > 0) {
            tooltip.add(Component.translatable("tooltip.engine.overheating"));
        }

        ChatFormatting damageColor = damage > 70 ? ChatFormatting.RED : damage > 30 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
        tooltip.add(Component.translatable("tooltip.engine.damage", damage).withStyle(damageColor));

        tooltip.add(Component.translatable("tooltip.engine.status", Component.translatable(isRunning ? "tooltip.engine.status_running" : "tooltip.engine.status_stopped")).withStyle(isRunning ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.engine.throttle", throttle).withStyle(ChatFormatting.YELLOW));

        int speed = (int)getGeneratedSpeed();
        tooltip.add(Component.translatable("tooltip.engine.speed", Math.abs(speed), speed < 0 ? Component.translatable("tooltip.engine.reversed") : "").withStyle(ChatFormatting.GREEN));
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Water", waterAmount);
        tag.putInt("Lava", lavaAmount);
        tag.putBoolean("Running", isRunning);
        tag.putInt("Damage", damage);
        tag.putInt("OverheatTicks", overheatTicks);
        tag.putInt("Throttle", throttle);
        tag.put("Freq1", frequencies[0].saveOptional(registries));
        tag.put("Freq2", frequencies[1].saveOptional(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        waterAmount = tag.getInt("Water");
        lavaAmount = tag.getInt("Lava");
        isRunning = tag.getBoolean("Running");
        damage = tag.getInt("Damage");
        overheatTicks = tag.getInt("OverheatTicks");
        throttle = tag.getInt("Throttle");
        frequencies[0] = tag.contains("Freq1") ? ItemStack.parseOptional(registries, tag.getCompound("Freq1")) : ItemStack.EMPTY;
        frequencies[1] = tag.contains("Freq2") ? ItemStack.parseOptional(registries, tag.getCompound("Freq2")) : ItemStack.EMPTY;
    }

    private static class EmptyBox extends ValueBoxTransform {
        @Override public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) { return new Vec3(0, -100, 0); }
        @Override public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {}
        @Override public float getScale() { return 0f; }
    }

    public static class TopValueBoxTransform extends ValueBoxTransform {
        @Override public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) { return new Vec3(0.5, 1.2, 0.5); }
        @Override public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) { ms.mulPose(Axis.XP.rotationDegrees(90)); }
        @Override public float getScale() { return 0.5f; }
    }
}