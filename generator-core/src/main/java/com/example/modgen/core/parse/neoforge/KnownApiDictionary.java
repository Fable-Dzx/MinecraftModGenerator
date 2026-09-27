package com.example.modgen.core.parse.neoforge;

import java.util.Map;
import java.util.Optional;

/**
 * Built-in dictionary that resolves the simple class names used in NeoForge documentation
 * to fully qualified names. QDox-parsed classes take precedence over this dictionary during
 * the merge phase; this is the fallback for types that are not part of the parsed sources
 * (Minecraft classes, Mojang codecs, etc.).
 */
public final class KnownApiDictionary {

    private static final Map<String, String> ENTRIES = Map.ofEntries(
            // NeoForge registration API
            Map.entry("DeferredRegister", "net.neoforged.neoforge.registries.DeferredRegister"),
            Map.entry("RegistryObject", "net.neoforged.neoforge.registries.RegistryObject"),
            Map.entry("DeferredHolder", "net.neoforged.neoforge.registries.DeferredHolder"),
            Map.entry("DeferredBlock", "net.neoforged.neoforge.registries.DeferredBlock"),
            Map.entry("DeferredItem", "net.neoforged.neoforge.registries.DeferredItem"),
            Map.entry("RegisterEvent", "net.neoforged.neoforge.registries.RegisterEvent"),
            Map.entry("NewRegistryEvent", "net.neoforged.neoforge.registries.NewRegistryEvent"),
            Map.entry("RegistryBuilder", "net.neoforged.neoforge.registries.RegistryBuilder"),
            Map.entry("NeoForgeRegistries", "net.neoforged.neoforge.registries.NeoForgeRegistries"),
            Map.entry("DataPackRegistryEvent", "net.neoforged.neoforge.registries.datapacks.DataPackRegistryEvent"),
            // Minecraft core types
            Map.entry("Block", "net.minecraft.world.level.block.Block"),
            Map.entry("BlockBehaviour", "net.minecraft.world.level.block.state.BlockBehaviour"),
            Map.entry("BlockState", "net.minecraft.world.level.block.state.BlockState"),
            Map.entry("SoundType", "net.minecraft.world.level.block.SoundType"),
            Map.entry("Item", "net.minecraft.world.item.Item"),
            Map.entry("BlockItem", "net.minecraft.world.item.BlockItem"),
            Map.entry("ItemStack", "net.minecraft.world.item.ItemStack"),
            Map.entry("CreativeModeTab", "net.minecraft.world.item.CreativeModeTab"),
            Map.entry("CreativeModeTabs", "net.minecraft.world.item.CreativeModeTabs"),
            Map.entry("ItemLike", "net.minecraft.world.level.ItemLike"),
            Map.entry("FoodProperties", "net.minecraft.world.food.FoodProperties"),
            Map.entry("MobEffectInstance", "net.minecraft.world.effect.MobEffectInstance"),
            Map.entry("Component", "net.minecraft.network.chat.Component"),
            Map.entry("Level", "net.minecraft.world.level.Level"),
            Map.entry("ServerLevel", "net.minecraft.server.level.ServerLevel"),
            Map.entry("BlockPos", "net.minecraft.core.BlockPos"),
            Map.entry("ResourceLocation", "net.minecraft.resources.ResourceLocation"),
            Map.entry("ResourceKey", "net.minecraft.resources.ResourceKey"),
            Map.entry("Registry", "net.minecraft.core.Registry"),
            Map.entry("RegistryAccess", "net.minecraft.core.RegistryAccess"),
            Map.entry("Holder", "net.minecraft.core.Holder"),
            Map.entry("HolderGetter", "net.minecraft.resources.HolderGetter"),
            Map.entry("BuiltInRegistries", "net.minecraft.core.registries.BuiltInRegistries"),
            Map.entry("Registries", "net.minecraft.core.registries.Registries"),
            Map.entry("RegistrySetBuilder", "net.minecraft.data.registries.RegistrySetBuilder"),
            Map.entry("BootstrapContext", "net.minecraft.data.worldgen.BootstrapContext"),
            Map.entry("DatapackBuiltinEntriesProvider", "net.minecraft.data.registries.DatapackBuiltinEntriesProvider"),
            Map.entry("ConfiguredFeature", "net.minecraft.world.level.levelgen.feature.ConfiguredFeature"),
            Map.entry("PlacedFeature", "net.minecraft.world.level.levelgen.placement.PlacedFeature"),
            Map.entry("DataComponents", "net.minecraft.core.component.DataComponents"),
            // Mojang serialization
            Map.entry("Codec", "com.mojang.serialization.Codec"),
            Map.entry("MapCodec", "com.mojang.serialization.MapCodec"),
            Map.entry("RecordCodecBuilder", "com.mojang.serialization.codecs.RecordCodecBuilder"),
            // NeoForge events and extensions
            Map.entry("EventBus", "net.neoforged.bus.api.EventBus"),
            Map.entry("IEventBus", "net.neoforged.bus.api.IEventBus"),
            Map.entry("BuildCreativeModeTabContentsEvent", "net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent"),
            Map.entry("GatherDataEvent", "net.neoforged.neoforge.data.event.GatherDataEvent"),
            Map.entry("InputEvent", "net.neoforged.neoforge.client.event.InputEvent"),
            Map.entry("PlayerInteractEvent", "net.neoforged.neoforge.event.entity.player.PlayerInteractEvent"),
            Map.entry("BlockEvent", "net.neoforged.neoforge.event.level.BlockEvent"),
            Map.entry("PlayerEvent", "net.neoforged.neoforge.event.entity.player.PlayerEvent"),
            Map.entry("IBlockExtension", "net.neoforged.neoforge.extensions.IBlockExtension"),
            Map.entry("IItemExtension", "net.neoforged.neoforge.extensions.IItemExtension"),
            // JDK
            Map.entry("Supplier", "java.util.function.Supplier"));

    private KnownApiDictionary() {
    }

    public static Optional<String> resolve(String simpleName) {
        return Optional.ofNullable(ENTRIES.get(simpleName));
    }
}
