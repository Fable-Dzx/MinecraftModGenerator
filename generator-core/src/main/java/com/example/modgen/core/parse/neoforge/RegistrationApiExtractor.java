package com.example.modgen.core.parse.neoforge;

import com.example.modgen.core.model.ApiClass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assigns QDox-parsed registration classes to their feature modules and builds the
 * {@code registration} module index (the shared registration knowledge both the block
 * and item features depend on).
 */
public final class RegistrationApiExtractor {

    private static final String REGISTRATION = "registration";
    private static final String BLOCKS = "blocks";
    private static final String ITEMS = "items";

    private RegistrationApiExtractor() {
    }

    /**
     * Groups parsed classes by module based on their canonical FQN.
     * <ul>
     *   <li>{@code DeferredRegister.Blocks}, {@code DeferredBlock} -> blocks</li>
     *   <li>{@code DeferredRegister.Items}, {@code DeferredItem} -> items</li>
     *   <li>{@code DeferredRegister}, {@code RegistryObject}, {@code DeferredHolder},
     *       {@code RegisterEvent}, ... -> registration</li>
     * </ul>
     */
    public static Map<String, List<ApiClass>> groupByModule(List<ApiClass> classes) {
        Map<String, List<ApiClass>> grouped = new LinkedHashMap<>();
        for (ApiClass apiClass : classes) {
            grouped.computeIfAbsent(moduleOf(apiClass), k -> new ArrayList<>()).add(apiClass);
        }
        Map<String, List<ApiClass>> immutable = new LinkedHashMap<>();
        grouped.forEach((module, list) -> immutable.put(module, List.copyOf(list)));
        return immutable;
    }

    private static String moduleOf(ApiClass apiClass) {
        String fqn = apiClass.fqn();
        if (fqn.endsWith("DeferredRegister.Blocks") || fqn.endsWith("DeferredBlock")) {
            return BLOCKS;
        }
        if (fqn.endsWith("DeferredRegister.Items") || fqn.endsWith("DeferredItem")) {
            return ITEMS;
        }
        return REGISTRATION;
    }
}
