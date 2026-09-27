package net.neoforged.neoforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * A handle to a registered object, obtained from {@code DeferredRegister#register}.
 *
 * @param <T> the type of the registered object
 */
public class RegistryObject<T> implements Supplier<T> {

    /**
     * Returns the registered object. Throws if the object is not (or no longer) present.
     *
     * @return the registered object
     */
    @Override
    public T get() {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Returns this handle as a plain supplier.
     *
     * @return a supplier of the registered object
     */
    public Supplier<T> asSupplier() {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Returns the registry name of the registered object.
     *
     * @return the registry name
     */
    public ResourceLocation getId() {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Returns the registry key of the registered object.
     *
     * @return the registry key
     */
    public ResourceKey<T> getKey() {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Returns whether the registered object is currently present.
     *
     * @return {@code true} if present
     */
    public boolean isPresent() {
        throw new UnsupportedOperationException("stub");
    }

    public static <T> RegistryObject<T> create(ResourceLocation id, ResourceKey<Registry<T>> registryKey) {
        throw new UnsupportedOperationException("stub");
    }

    public static <T> RegistryObject<T> create(ResourceKey<T> key, Registry<T> registry) {
        throw new UnsupportedOperationException("stub");
    }

    public static <T> RegistryObject<T> of(ResourceKey<T> key) {
        throw new UnsupportedOperationException("stub");
    }

    public static <T> RegistryObject<T> empty() {
        throw new UnsupportedOperationException("stub");
    }
}
