package com.praporvito.createaeronauticsflaps;

import com.praporvito.createaeronauticsflaps.block.AileronBlock;
import com.praporvito.createaeronauticsflaps.block.AileronExtensionBlock;
import com.praporvito.createaeronauticsflaps.block.LavaEngineBlock;
import com.praporvito.createaeronauticsflaps.block.SmallEngineBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateAeronauticsFlaps.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateAeronauticsFlaps.MOD_ID);

    public static final DeferredBlock<Block> AILERON = registerBlock("aileron",
            () -> new AileronBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion().dynamicShape()));

    public static final DeferredBlock<Block> AILERON_EXTENSION = registerBlock("aileron_extension",
            () -> new AileronExtensionBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion().dynamicShape()));

    public static final DeferredBlock<Block> SMALL_ENGINE = registerBlock("small_engine",
            () -> new SmallEngineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<Block> LAVA_ENGINE = registerBlock("lava_engine",
            () -> new LavaEngineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(5.0F).sound(SoundType.METAL).noOcclusion().dynamicShape()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}