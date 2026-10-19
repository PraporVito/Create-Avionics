package com.praporvito.createaeronauticsflaps;

import com.praporvito.createaeronauticsflaps.block.AileronBlockEntity;
import com.praporvito.createaeronauticsflaps.block.AileronExtensionBlockEntity;
import com.praporvito.createaeronauticsflaps.block.LavaEngineBlockEntity;
import com.praporvito.createaeronauticsflaps.block.SmallEngineBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateAeronauticsFlaps.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AileronBlockEntity>> AILERON_BE =
            BLOCK_ENTITIES.register("aileron_be", () -> BlockEntityType.Builder.of(AileronBlockEntity::new, ModBlocks.AILERON.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AileronExtensionBlockEntity>> AILERON_EXTENSION_BE =
            BLOCK_ENTITIES.register("aileron_extension_be", () -> BlockEntityType.Builder.of(AileronExtensionBlockEntity::new, ModBlocks.AILERON_EXTENSION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmallEngineBlockEntity>> SMALL_ENGINE_BE =
            BLOCK_ENTITIES.register("small_engine_be", () -> BlockEntityType.Builder.of(SmallEngineBlockEntity::new, ModBlocks.SMALL_ENGINE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LavaEngineBlockEntity>> LAVA_ENGINE_BE =
            BLOCK_ENTITIES.register("lava_engine_be", () -> BlockEntityType.Builder.of(LavaEngineBlockEntity::new, ModBlocks.LAVA_ENGINE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}