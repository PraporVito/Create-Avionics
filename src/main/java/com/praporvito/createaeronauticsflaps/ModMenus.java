package com.praporvito.createaeronauticsflaps;

import com.praporvito.createaeronauticsflaps.block.LavaEngineMenu;
import com.praporvito.createaeronauticsflaps.block.SmallEngineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CreateAeronauticsFlaps.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<SmallEngineMenu>> SMALL_ENGINE_MENU =
            MENUS.register("small_engine_menu", () -> IMenuTypeExtension.create(SmallEngineMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<LavaEngineMenu>> LAVA_ENGINE_MENU =
            MENUS.register("lava_engine_menu", () -> IMenuTypeExtension.create(LavaEngineMenu::new));

    public static void register(IEventBus bus) { MENUS.register(bus); }
}