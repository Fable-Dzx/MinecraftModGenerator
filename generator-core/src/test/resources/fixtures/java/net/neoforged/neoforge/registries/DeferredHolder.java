package net.neoforged.neoforge.registries;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Holds a registered object, with separate type parameters for the registry entry type
 * and the concrete registered type.
 *
 * @param <R> the registry entry type
 * @param <T> the concrete registered type, a subtype of {@code R}
 */
public class DeferredHolder<R, T extends R> implements Supplier<T> {

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
    public ResourceKey<R> getKey() {
        throw new UnsupportedOperationException("stub");
    }

    /**
     * Returns this holder as a plain supplier.
     *
     * @return a supplier of the registered object
     */
    public Supplier<T> asSupplier() {
        throw new UnsupportedOperationException("stub");
    }
}
