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

public class LavaEngineMenu extends AbstractContainerMenu {
    private final Container frequencies = new SimpleContainer(2);
    private final ContainerLevelAccess access;
    @Nullable private final LavaEngineBlockEntity blockEntity;

    public LavaEngineMenu(int id, Inventory inv, LavaEngineBlockEntity be) {
        super(ModMenus.LAVA_ENGINE_MENU.get(), id);
        this.blockEntity = be;
        Level level = be.getLevel();
        this.access = level == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(level, be.getBlockPos());

        this.frequencies.setItem(0, be.getFrequency(true).copy());
        this.frequencies.setItem(1, be.getFrequency(false).copy());

        // ТОЛЬКО СЛОТЫ ЧАСТОТ
        addSlot(new Slot(frequencies, 0, 65, 16));
        addSlot(new Slot(frequencies, 1, 65, 52));

        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 98 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 156));
    }

    public LavaEngineMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.LAVA_ENGINE_MENU.get(), id);
        Level level = inv.player.level();
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = level.getBlockEntity(pos);
        this.blockEntity = be instanceof LavaEngineBlockEntity engine ? engine : null;
        this.access = ContainerLevelAccess.create(level, pos);

        if (blockEntity != null) {
            this.frequencies.setItem(0, blockEntity.getFrequency(true));
            this.frequencies.setItem(1, blockEntity.getFrequency(false));
        }

        addSlot(new Slot(frequencies, 0, 66, 17));
        addSlot(new Slot(frequencies, 1, 66, 53));

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
        return ItemStack.EMPTY; // Упрощенно
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.LAVA_ENGINE.get());
    }
}