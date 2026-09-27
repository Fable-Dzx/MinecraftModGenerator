# Blocks

Blocks are essential to the Minecraft world. They make up all the terrain, structures, and machines.

## Creating Blocks

As discussed before, we start by creating our `DeferredRegister.Blocks`:

```java
public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("yourmodid");
```

### Basic Blocks

For simple blocks which need no special functionality, the `Block` class can be used directly. To do so, during registration, instantiate `Block` with a `BlockBehaviour.Properties` parameter. This `BlockBehaviour.Properties` parameter can be created using `BlockBehaviour.Properties#of`, and it can be customized by calling its methods. The most important methods for this are:

- `destroyTime` - Determines the time the block needs to be destroyed.
- `explosionResistance` - Determines the explosion resistance of the block.
- `sound` - Sets the sound the block makes when it is punched, broken, or placed. The default value is `SoundType.STONE`.
- `lightLevel` - Sets the light emission of the block. Accepts a function with a `BlockState` parameter that returns a value between 0 and 15.

So for example, a simple implementation would look something like this:

```java
public static final DeferredBlock<Block> MY_BLOCK = BLOCKS.register(
        "my_block",
        () -> new Block(BlockBehaviour.Properties.of()
                //highlight-start
                .destroyTime(2.0f)
                .explosionResistance(10.0f)
                //highlight-end
        ));
```

:::note
It is important to understand that a block in the world is not the same thing as in an inventory. What looks like a block in an inventory is actually a `BlockItem`. A `BlockItem` must be registered separately from the block.
:::

### `DeferredRegister.Blocks` helpers

We already discussed how to create a `DeferredRegister.Blocks` and that it returns `DeferredBlock`s. Let's start with `#registerBlock`:

```java
public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerBlock(
        "example_block",
        Block::new, // The factory that the properties will be passed into.
        BlockBehaviour.Properties.of() // The properties to use.
);
```

If you want to use `Block::new`, you can leave out the factory entirely:

```java
public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock(
        "example_block",
        BlockBehaviour.Properties.of() // The properties to use.
);
```
