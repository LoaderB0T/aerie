package com.loaderb0t.aerie;

import net.minecraft.resources.ResourceLocation;
import vazkii.patchouli.api.IMultiblock;
import vazkii.patchouli.api.PatchouliAPI;

/** Patchouli structure definitions: validation in world and ghost previews in the guide book. */
public final class AerieMultiblocks {
    public static IMultiblock SHRINE;
    public static IMultiblock RELAY;

    private AerieMultiblocks() {}

    public static void register() {
        var api = PatchouliAPI.get();
        // Layers top to bottom; '_' is any block (Patchouli's ' ' means air), '0' is the centre.
        // Unchecked cells stay free so players can decorate inside the shrine.
        SHRINE = api.registerMultiblock(id("shrine"), api.makeMultiblock(new String[][]{
                        {"F___F", "_____", "_____", "_____", "F___F"},
                        {"K___K", "_____", "__0__", "_____", "K___K"},
                        {"P___P", "_____", "__P__", "_____", "P___P"}},
                'F', AerieRegistry.FOCUS.get(), 'K', AerieRegistry.COLUMN.get(),
                'P', AerieRegistry.PLINTH.get(), '0', AerieRegistry.CORE.get()).setSymmetrical(true));
        RELAY = api.registerMultiblock(id("relay"), api.makeMultiblock(new String[][]{{"0"}, {"K"}, {"P"}},
                'K', AerieRegistry.COLUMN.get(), 'P', AerieRegistry.PLINTH.get(),
                '0', AerieRegistry.RELAY_CROWN.get()).setSymmetrical(true));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AerieMod.MOD_ID, path);
    }
}
