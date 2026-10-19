package com.praporvito.createaeronauticsflaps;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateAeronauticsFlaps.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AVIONICS_TAB = CREATIVE_MODE_TABS.register("avionics_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createaeronauticsflaps.avionics"))
                    .icon(() -> new ItemStack(ModBlocks.LAVA_ENGINE.get())) // Иконка вкладки - Лава-Двигатель
                    .displayItems((parameters, output) -> {
                        // Добавляем сюда все твои блоки
                        output.accept(ModBlocks.AILERON.get());
                        output.accept(ModBlocks.AILERON_EXTENSION.get());
                        output.accept(ModBlocks.SMALL_ENGINE.get());
                        output.accept(ModBlocks.LAVA_ENGINE.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}