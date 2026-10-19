package com.praporvito.createaeronauticsflaps;

import com.praporvito.createaeronauticsflaps.client.AileronExtensionRenderer;
import com.praporvito.createaeronauticsflaps.client.AileronRenderer;
import com.praporvito.createaeronauticsflaps.client.LavaEngineRenderer;
import com.praporvito.createaeronauticsflaps.client.LavaEngineScreen;
import com.praporvito.createaeronauticsflaps.client.SmallEngineScreen;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(CreateAeronauticsFlaps.MOD_ID)
public class CreateAeronauticsFlaps {
    public static final String MOD_ID = "createaeronauticsflaps";

    public CreateAeronauticsFlaps(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus); // <-- Регистрация вкладки

        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerAdditionalModels);
        modEventBus.addListener(this::registerScreens);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.AILERON_BE.get(), AileronRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AILERON_EXTENSION_BE.get(), AileronExtensionRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SMALL_ENGINE_BE.get(), KineticBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LAVA_ENGINE_BE.get(), LavaEngineRenderer::new);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SMALL_ENGINE_MENU.get(), SmallEngineScreen::new);
        event.register(ModMenus.LAVA_ENGINE_MENU.get(), LavaEngineScreen::new);
    }

    private void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MOD_ID, "block/aileron")));
        event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MOD_ID, "block/aileron_inverted")));
    }
}