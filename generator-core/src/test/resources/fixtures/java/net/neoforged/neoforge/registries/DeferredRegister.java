package net.neoforged.neoforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Manages registration of objects to a registry.
 *
 * <p>This is the recommended way to register blocks, items and other registry contents.
 * The {@code DeferredRegister} wraps the {@code RegisterEvent} mechanism and defers
 * registration until the corresponding registry event fires.</p>
 *
 * @param <T> the type of the objects this register manages
 */
public class DeferredRegister<T> {

    protected DeferredRegister(ResourceKey<? extends Registry<T>> registryKey) {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Creates a {@code DeferredRegister} for the given registry key.
     *
     * @param registryKey the key of the registry to register to
     * @param modid       the mod id used as namespace for the registry names
     * @param <T>         the registry entry type
     * @return the new deferred register
     */
    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String modid) {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Creates a block-specific {@code DeferredRegister.Blocks} for the block registry.
     *
     * @param modid the mod id used as namespace
     * @return the new block deferred register
     */
    public static DeferredRegister.Blocks createBlocks(String modid) {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Creates an item-specific {@code DeferredRegister.Items} for the item registry.
     *
     * @param modid the mod id used as namespace
     * @return the new item deferred register
     */
    public static DeferredRegister.Items createItems(String modid) {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Registers an object under the given name.
     *
     * @param name     the registry name (relative to the mod id namespace)
     * @param supplier the supplier of the object to register
     * @return the resulting registry object handle
     */
    public RegistryObject<T> register(String name, Supplier<? extends T> supplier) {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Block-specific helper register with extra convenience methods.
     */
    public static class Blocks extends DeferredRegister<Block> {

        private Blocks() {
            super(null);
        }

        /**
         * Registers a block, applying the properties to the given factory.
         *
         * @param name       the registry name
         * @param factory    block constructor taking the properties
         * @param properties the block properties to apply
         * @param <B>        the concrete block type
         * @return the resulting deferred block
         */
        public <B extends Block> DeferredHolder<Block, B> registerBlock(
                String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
            throw new UnsupportedOperationException("stub");
        }

        /**
         * Registers a block using {@code Block::new} as factory.
         *
         * @param name       the registry name
         * @param properties the block properties to apply
         * @return the resulting deferred block
         */
        public DeferredHolder<Block, Block> registerSimpleBlock(String name, BlockBehaviour.Properties properties) {
            throw new UnsupportedOperationException("stub");
        }
    }

    /**
     * Item-specific helper register with extra convenience methods.
     */
    public static class Items extends DeferredRegister<Item> {

        private Items() {
            super(null);
        }

        /**
         * Registers an item, applying the properties to the given factory.
         *
         * @param name       the registry name
         * @param factory    item constructor taking the properties
         * @param properties the item properties to apply
         * @param <I>        the concrete item type
         * @return the resulting registry object
         */
        public <I extends Item> RegistryObject<I> registerItem(
                String name, Function<Item.Properties, I> factory, Item.Properties properties) {
            throw new UnsupportedOperationException("stub");
        }

        /**
         * Registers an item using {@code Item::new} as factory.
         *
         * @param name       the registry name
         * @param properties the item properties to apply
         * @return the resulting registry object
         */
        public RegistryObject<Item> registerSimpleItem(String name, Item.Properties properties) {
            throw new UnsupportedOperationException("stub");
        }

        /**
         * Registers a block item for the given block.
         *
         * @param name       the registry name
         * @param block      the deferred block to create the item for
         * @param properties the item properties to apply
         * @return the resulting registry object
         */
        public RegistryObject<BlockItem> registerSimpleBlockItem(
                String name, DeferredHolder<Block, ? extends Block> block, Item.Properties properties) {
            throw new UnsupportedOperationException("stub");
        }
    }
}
