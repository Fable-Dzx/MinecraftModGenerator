# Items

Along with blocks, items are a key component of Minecraft. While blocks make up the world around you, items exist within inventories.

## Creating an Item

For basic items that need no special functionality, the `Item` class can be used directly. To do so, during registration, instantiate `Item` with a `Item.Properties` parameter. This `Item.Properties` parameter can be created using `Item.Properties#of`, and it can be customized by calling its methods:

- `stacksTo` - Sets the max stack size of this item. Defaults to 64. Used e.g. by ender pearls or other items that only stack to 16.
- `durability` - Sets the durability of this item and the initial damage to 0. Defaults to 0, which means "no durability".
- `fireResistant` - Makes item entities that use this item immune to fire and lava.
- `rarity` - Sets the rarity of this item. Currently, this simply changes the item's color.

### `DeferredRegister.Items`

All registries use `DeferredRegister` to register their contents, and items are no exceptions. NeoForge provides the `DeferredRegister.Items` helper class that extends `DeferredRegister<Item>` and provides some item-specific helpers:

```java
public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ExampleMod.MOD_ID);

public static final Supplier<Item> EXAMPLE_ITEM = ITEMS.registerItem(
    "example_item",
    Item::new, // The factory that the properties will be passed into.
    new Item.Properties() // The properties to use.
);
```

If you want to use `Item::new`, you can leave out the factory entirely and use the `simple` method variant:

```java
public static final Supplier<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem(
    "example_item",
    new Item.Properties() // The properties to use.
);
```

:::note
If you keep your registered blocks in a separate class, you should classload your blocks class before your items class.
:::
