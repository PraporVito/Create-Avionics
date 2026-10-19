package com.praporvito.createaeronauticsflaps.block;

import com.praporvito.createaeronauticsflaps.ModBlocks;
import com.praporvito.createaeronauticsflaps.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class SmallEngineMenu extends AbstractContainerMenu {
    private final Container frequencies = new SimpleContainer(2);
    private final Container fuelContainer;
    private final ContainerLevelAccess access;
    @Nullable private final SmallEngineBlockEntity blockEntity;

    public SmallEngineMenu(int id, Inventory inv, SmallEngineBlockEntity be) {
        super(ModMenus.SMALL_ENGINE_MENU.get(), id);
        this.blockEntity = be;
        Level level = be.getLevel();
        this.access = level == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(level, be.getBlockPos());

        this.frequencies.setItem(0, be.getFrequency(true).copy());
        this.frequencies.setItem(1, be.getFrequency(false).copy());

        this.fuelContainer = new Container() {
            @Override public int getContainerSize() { return 1; }
            @Override public boolean isEmpty() { return be.getFuel().isEmpty(); }
            @Override public ItemStack getItem(int slot) { return be.getFuel(); }
            @Override public ItemStack removeItem(int slot, int amount) {
                ItemStack stack = be.getFuel().copy();
                ItemStack split = stack.split(amount);
                be.setFuel(stack);
                return split;
            }
            @Override public ItemStack removeItemNoUpdate(int slot) {
                ItemStack stack = be.getFuel().copy();
                be.setFuel(ItemStack.EMPTY);
                return stack;
            }
            @Override public void setItem(int slot, ItemStack stack) { be.setFuel(stack); }
            @Override public void setChanged() { be.setChanged(); }
            @Override public boolean stillValid(Player player) { return true; }
            @Override public void clearContent() { be.setFuel(ItemStack.EMPTY); }
        };

        // ТОЧНЫЕ КООРДИНАТЫ ИЗ ТВОЕЙ ТЕКСТУРЫ
        addSlot(new Slot(frequencies, 0, 65, 16)); // Частота 1
        addSlot(new Slot(frequencies, 1, 65, 52)); // Частота 2

        addSlot(new Slot(fuelContainer, 0, 101, 34) { // Топливо
            @Override public boolean mayPlace(ItemStack stack) { return stack.getBurnTime(null) > 0; }
        });

        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 98 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 156));
    }

    public SmallEngineMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.SMALL_ENGINE_MENU.get(), id);
        Level level = inv.player.level();
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = level.getBlockEntity(pos);
        this.blockEntity = be instanceof SmallEngineBlockEntity engine ? engine : null;
        this.access = ContainerLevelAccess.create(level, pos);

        if (blockEntity != null) {
            this.frequencies.setItem(0, blockEntity.getFrequency(true));
            this.frequencies.setItem(1, blockEntity.getFrequency(false));
        }

        this.fuelContainer = new Container() {
            @Override public int getContainerSize() { return 1; }
            @Override public boolean isEmpty() { return blockEntity == null || blockEntity.getFuel().isEmpty(); }
            @Override public ItemStack getItem(int slot) { return blockEntity == null ? ItemStack.EMPTY : blockEntity.getFuel(); }
            @Override public ItemStack removeItem(int slot, int amount) {
                if (blockEntity == null) return ItemStack.EMPTY;
                ItemStack stack = blockEntity.getFuel().copy();
                ItemStack split = stack.split(amount);
                blockEntity.setFuel(stack);
                return split;
            }
            @Override public ItemStack removeItemNoUpdate(int slot) {
                if (blockEntity == null) return ItemStack.EMPTY;
                ItemStack stack = blockEntity.getFuel().copy();
                blockEntity.setFuel(ItemStack.EMPTY);
                return stack;
            }
            @Override public void setItem(int slot, ItemStack stack) { if (blockEntity != null) blockEntity.setFuel(stack); }
            @Override public void setChanged() { if (blockEntity != null) blockEntity.setChanged(); }
            @Override public boolean stillValid(Player player) { return true; }
            @Override public void clearContent() { if (blockEntity != null) blockEntity.setFuel(ItemStack.EMPTY); }
        };

        addSlot(new Slot(frequencies, 0, 66, 17));
        addSlot(new Slot(frequencies, 1, 66, 53));
        addSlot(new Slot(fuelContainer, 0, 102, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getBurnTime(null) > 0; }
        });

// Инвентарь игрока (3 ряда)
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 12 + col * 18, 84 + row * 18));

// Хотбар (1 ряд внизу)
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 12 + col * 18, 142));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 2) {
            if (clickType != ClickType.PICKUP) return;
            ItemStack carried = getCarried();
            ItemStack assigned = button == 1 || carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
            frequencies.setItem(slotId, assigned);
            if (blockEntity != null && !player.level().isClientSide) {
                blockEntity.setFrequency(slotId == 0, assigned);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 2) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index > 2) {
            if (stack.getBurnTime(null) <= 0 || !moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SMALL_ENGINE.get());
    }
}