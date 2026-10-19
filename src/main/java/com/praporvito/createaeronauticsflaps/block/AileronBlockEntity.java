package com.praporvito.createaeronauticsflaps.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.praporvito.createaeronauticsflaps.ModBlockEntities;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.UUID;

public class AileronBlockEntity extends SmartBlockEntity {

    private final ItemStack[] upFreqs   = { ItemStack.EMPTY, ItemStack.EMPTY };
    private final ItemStack[] downFreqs = { ItemStack.EMPTY, ItemStack.EMPTY };
    private boolean needsFrequencySync = false;

    private float currentAngle = 0.0f;
    private float prevAngle = 0.0f;
    private int powerUp = 0;
    private int powerDown = 0;

    private float wear = 0.0f;
    private float stress = 0.0f;
    private int stuckTicks = 0;
    private int lastPowerUp = 0;
    private int lastPowerDown = 0;

    private int maxAngle = 30;
    private ScrollValueBehaviour maxAngleBehaviour;

    public enum SetupPhase { NONE, UP_1, UP_2, DOWN_1, DOWN_2 }
    private SetupPhase phase = SetupPhase.NONE;
    private UUID configuringPlayer = null;

    private LinkBehaviour linkUp;
    private LinkBehaviour linkDown;

    public AileronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AILERON_BE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        Pair<ValueBoxTransform, ValueBoxTransform> slots1 = Pair.of(new EmptyBox(), new EmptyBox());
        Pair<ValueBoxTransform, ValueBoxTransform> slots2 = Pair.of(new EmptyBox(), new EmptyBox());
        linkUp = LinkBehaviour.receiver(this, slots1, this::setPowerUp);
        linkDown = LinkBehaviour.receiver(this, slots2, this::setPowerDown);
        behaviours.add(linkUp);
        behaviours.add(linkDown);

        maxAngleBehaviour = new ScrollValueBehaviour(
                Component.translatable("gui.createaeronauticsflaps.aileron.max_angle"),
                this,
                new SmallEngineBlockEntity.TopValueBoxTransform()
        );
        maxAngleBehaviour.between(10, 45);
        maxAngleBehaviour.value = 30;
        maxAngleBehaviour.withCallback(this::onMaxAngleChanged);
        behaviours.add(maxAngleBehaviour);
    }

    private void onMaxAngleChanged(Integer newAngle) {
        this.maxAngle = newAngle;
        setChanged();
    }

    private void setPowerUp(int power) {
        if (this.powerUp == power) return;
        this.powerUp = power;
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void setPowerDown(int power) {
        if (this.powerDown == power) return;
        this.powerDown = power;
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void toggleSetup(Player player) {
        if (phase != SetupPhase.NONE) {
            if (configuringPlayer != null && configuringPlayer.equals(player.getUUID())) {
                phase = SetupPhase.NONE;
                configuringPlayer = null;
                player.displayClientMessage(Component.translatable("message.aileron.setup_finished"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.aileron.busy"), true);
            }
        } else {
            phase = SetupPhase.UP_1;
            configuringPlayer = player.getUUID();
            player.displayClientMessage(Component.translatable("message.aileron.setup_started"), true);
            player.displayClientMessage(Component.translatable("message.aileron.prompt_up_1"), true);
        }
        setChanged();
    }

    public ItemInteractionResult handleInteraction(Player player, ItemStack stack) {
        if (phase == SetupPhase.NONE) {
            player.displayClientMessage(Component.translatable("message.aileron.info_header"), false);
            player.displayClientMessage(Component.translatable("message.aileron.info_up", freqStr(upFreqs[0]), freqStr(upFreqs[1])), false);
            player.displayClientMessage(Component.translatable("message.aileron.info_down", freqStr(downFreqs[0]), freqStr(downFreqs[1])), false);
            player.displayClientMessage(Component.translatable("message.aileron.info_max_angle", maxAngle), false);
            player.displayClientMessage(Component.translatable("message.aileron.info_wear", String.format("%.1f", wear), String.format("%.1f", stress)), false);
            if (stuckTicks > 0) player.displayClientMessage(Component.translatable("message.aileron.jammed"), false);
            return ItemInteractionResult.SUCCESS;
        }
        if (configuringPlayer == null || !configuringPlayer.equals(player.getUUID()))
            return ItemInteractionResult.SUCCESS;
        if (stack.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.aileron.hold_freq"), true);
            return ItemInteractionResult.SUCCESS;
        }

        switch (phase) {
            case UP_1 -> {
                upFreqs[0] = stack.copy();
                phase = SetupPhase.UP_2;
                player.displayClientMessage(Component.translatable("message.aileron.prompt_up_2"), true);
            }
            case UP_2 -> {
                upFreqs[1] = stack.copy();
                phase = SetupPhase.DOWN_1;
                player.displayClientMessage(Component.translatable("message.aileron.prompt_down_1"), true);
            }
            case DOWN_1 -> {
                downFreqs[0] = stack.copy();
                phase = SetupPhase.DOWN_2;
                player.displayClientMessage(Component.translatable("message.aileron.prompt_down_2"), true);
            }
            case DOWN_2 -> {
                downFreqs[1] = stack.copy();
                phase = SetupPhase.NONE;
                configuringPlayer = null;
                player.displayClientMessage(Component.translatable("message.aileron.setup_finished"), true);
            }
        }
        applyFrequencies();
        setChanged();
        notifyUpdate();
        return ItemInteractionResult.SUCCESS;
    }

    private static String freqStr(ItemStack s) {
        return (s == null || s.isEmpty()) ? Component.translatable("message.aileron.not_set").getString() : s.getHoverName().getString();
    }

    private void applyFrequencies() {
        if (linkUp == null || linkDown == null) return;
        linkUp.setFrequency(true,  upFreqs[0]);
        linkUp.setFrequency(false, upFreqs[1]);
        linkDown.setFrequency(true,  downFreqs[0]);
        linkDown.setFrequency(false, downFreqs[1]);
    }

    private static class EmptyBox extends ValueBoxTransform {
        @Override public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) { return new Vec3(0, -100, 0); }
        @Override public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {}
        @Override public float getScale() { return 0f; }
    }

    public void repair(Player player) {
        this.wear = 0.0f;
        this.stress = 0.0f;
        this.stuckTicks = 0;
        setChanged();
        notifyUpdate();
        player.displayClientMessage(Component.translatable("message.aileron.repaired"), true);
    }

    @Override
    public void tick() {
        super.tick();

        if (needsFrequencySync) {
            needsFrequencySync = false;
            applyFrequencies();
        }

        prevAngle = currentAngle;
        if (level == null || level.isClientSide) return;

        float targetBeforeNoise = getTargetAngleWithoutNoise();
        if (Math.abs(currentAngle - targetBeforeNoise) > 0.1f) {
            wear = Math.min(100.0f, wear + 0.002f);
        }

        boolean suddenChange = (Math.abs(powerUp - lastPowerUp) > 5) || (Math.abs(powerDown - lastPowerDown) > 5);
        if (suddenChange) {
            stress = Math.min(100.0f, stress + 1.5f);
        } else {
            stress = Math.max(0.0f, stress - 0.05f);
        }
        lastPowerUp = powerUp;
        lastPowerDown = powerDown;

        if (stress > 80.0f && stuckTicks <= 0 && level.random.nextFloat() < 0.01f) {
            stuckTicks = 100 + level.random.nextInt(100);
            level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 0.5f);
            stress = 0.0f;
        }

        if (stuckTicks > 0) {
            stuckTicks--;
        } else {
            float targetAngle = getTargetAngleWithNoise();
            if (Math.abs(currentAngle - targetAngle) > 0.01f) {
                currentAngle += Mth.clamp(targetAngle - currentAngle, -2.5f, 2.5f);
            }
        }

        boolean hasSignal = powerUp > 0 || powerDown > 0;
        int angleInt = Math.round(currentAngle) + 30;

        BlockState oldState = getBlockState();
        BlockState newState = oldState;
        if (newState.getValue(AileronBlock.POWERED) != hasSignal) newState = newState.setValue(AileronBlock.POWERED, hasSignal);
        if (newState.getValue(AileronBlock.ANGLE)   != angleInt)  newState = newState.setValue(AileronBlock.ANGLE, angleInt);
        if (newState != oldState) level.setBlock(worldPosition, newState, 2);
    }

    private float getTargetAngleWithoutNoise() {
        if (powerUp > 0 && powerDown == 0) return -(powerUp / 15.0f) * maxAngle;
        if (powerDown > 0 && powerUp == 0) return (powerDown / 15.0f) * maxAngle;
        return 0.0f;
    }

    private float getTargetAngleWithNoise() {
        float base = getTargetAngleWithoutNoise();

        if (powerUp == 0 && powerDown == 0) {
            boolean isRight = getBlockState().getValue(AileronBlock.SIDE) == AileronBlock.AileronSide.RIGHT;
            float sagDirection = isRight ? -1.0f : 1.0f;
            float sagAmount = (wear / 100.0f) * 3.0f;
            base += sagDirection * sagAmount;
        }

        float noise = (level.random.nextFloat() - 0.5f) * (wear / 100.0f) * 1.5f;
        return base + noise;
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider reg, boolean clientPacket) {
        super.write(tag, reg, clientPacket);
        writeStack(tag, "AilUp1", upFreqs[0], reg);
        writeStack(tag, "AilUp2", upFreqs[1], reg);
        writeStack(tag, "AilDn1", downFreqs[0], reg);
        writeStack(tag, "AilDn2", downFreqs[1], reg);
        tag.putInt("PowerUp", powerUp);
        tag.putInt("PowerDown", powerDown);
        tag.putFloat("Angle", currentAngle);
        tag.putInt("MaxAngle", maxAngle);
        tag.putFloat("Wear", wear);
        tag.putFloat("Stress", stress);
        tag.putInt("StuckTicks", stuckTicks);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider reg, boolean clientPacket) {
        super.read(tag, reg, clientPacket);
        upFreqs[0]   = readStack(tag, "AilUp1", reg);
        upFreqs[1]   = readStack(tag, "AilUp2", reg);
        downFreqs[0] = readStack(tag, "AilDn1", reg);
        downFreqs[1] = readStack(tag, "AilDn2", reg);
        needsFrequencySync = true;
        powerUp      = tag.getInt("PowerUp");
        powerDown    = tag.getInt("PowerDown");
        currentAngle = tag.getFloat("Angle");
        if (tag.contains("MaxAngle")) {
            maxAngle = tag.getInt("MaxAngle");
            if (maxAngleBehaviour != null) maxAngleBehaviour.value = maxAngle;
        }
        wear = tag.getFloat("Wear");
        stress = tag.getFloat("Stress");
        stuckTicks = tag.getInt("StuckTicks");
    }

    private static void writeStack(CompoundTag tag, String key, ItemStack stack, HolderLookup.Provider reg) {
        ItemStack.OPTIONAL_CODEC.encodeStart(reg.createSerializationContext(NbtOps.INSTANCE), stack)
                .result().ifPresent(nbt -> tag.put(key, nbt));
    }

    private static ItemStack readStack(CompoundTag tag, String key, HolderLookup.Provider reg) {
        Tag raw = tag.get(key);
        if (raw == null) return ItemStack.EMPTY;
        return ItemStack.OPTIONAL_CODEC.parse(reg.createSerializationContext(NbtOps.INSTANCE), raw)
                .result().orElse(ItemStack.EMPTY);
    }

    public float getCurrentAngle() { return currentAngle; }
    public float getPrevAngle()    { return prevAngle; }
    public int getMaxAngle()       { return maxAngle; }
    public float getWear()         { return wear; }
    public float getStress()       { return stress; }
    public int getStuckTicks()     { return stuckTicks; }
}