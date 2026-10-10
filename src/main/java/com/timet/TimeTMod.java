package com.timet;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TimeTMod.MOD_ID)
public class TimeTMod {

    public static final String MOD_ID = "timet";
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeTMod.class);

    public TimeTMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[TimeT] Initialisation du mod TimeT (Synchronisation de l'heure PC)...");

        // Chargement de la configuration
        TimeConfig.get();

        // Enregistrement des événements serveur et commandes
        NeoForge.EVENT_BUS.register(TimeManager.class);
        NeoForge.EVENT_BUS.register(TimeCommand.class);

        LOGGER.info("[TimeT] TimeT 1.21.1 chargé avec succès.");
    }
}
