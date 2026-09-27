---
sidebar_position: 1
---
# Registries

Registration is the process of taking the objects of a mod (such as [items][item], [blocks][block]) and making them known to the game. Registering things is important, as without registration the game will simply not know about these objects.

A registry is a wrapper around a map that maps registry names to registered objects. Registry names must be unique within the same registry.

## Methods for Registering

NeoForge offers two ways to register objects: the `DeferredRegister` class, and the `RegisterEvent`. The former is a wrapper around the latter, and is recommended in order to prevent mistakes.

### `DeferredRegister`

We begin by creating our `DeferredRegister`:

```java
public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
        // The registry we want to use.
        BuiltInRegistries.BLOCK,
        // Our mod id.
        ExampleMod.MOD_ID
);
```

We can then add our registry entries as static final fields:

```java
public static final DeferredHolder<Block, Block> EXAMPLE_BLOCK = BLOCKS.register(
        "example_block", // Our registry name.
        () -> new Block(...) // A supplier of the object we want to register.
);
```

`DeferredHolder<R, T extends R>` is a subclass of `Supplier<T>`. To get our registered object when we need it, we can call `DeferredHolder#get()`.

:::danger
Query operations are only safe to use after registration has finished. **DO NOT QUERY REGISTRIES WHILE REGISTRATION IS STILL ONGOING!**
:::

### `RegisterEvent`

`RegisterEvent` is the second way to register objects. It is fired for each registry on the mod event bus.

```java
@SubscribeEvent // on the mod event bus
public static void register(RegisterEvent event) {
    event.register(
            BuiltInRegistries.BLOCK,
            registry -> {
                registry.register(ResourceLocation.fromNamespaceAndPath(MODID, "example_block_1"), new Block(...));
            }
    );
}
```

## Querying Registries

Sometimes, you will find yourself in situations where you want to get a registered object by a given id. Since registries are basically reversible maps, both operations work:

| Operation | Description |
| --- | --- |
| `containsKey` | Checks whether the registry contains the given id |
| `getKey` | Returns the `ResourceLocation` of a registered object |

For the full API surface, see the [DeferredRegister javadoc](https://docs.neoforged.net/javadoc/net/neoforged/neoforge/registries/DeferredRegister.html).

[item]: ../items/index.md
[block]: ../blocks/index.md
