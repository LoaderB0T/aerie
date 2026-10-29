package com.loaderb0t.aerie;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(AerieMod.MOD_ID)
public final class AerieMod {
    public static final String MOD_ID = "aerie";

    public AerieMod(IEventBus modBus, ModContainer container) {
        AerieRegistry.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, AerieConfig.SPEC);
        if (FMLEnvironment.dist.isClient()) AerieClient.init(container);
    }
}
