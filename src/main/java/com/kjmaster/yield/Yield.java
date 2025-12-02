package com.kjmaster.yield;

import com.kjmaster.yield.client.YieldOverlay;
import com.kjmaster.yield.event.ClientGameEvents;
import com.kjmaster.yield.event.YieldNeoForgeEventHandler;
import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.manager.ProjectRepository;
import com.kjmaster.yield.tracker.InventoryMonitor;
import com.kjmaster.yield.tracker.SessionTracker;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Yield.MODID)
public class Yield {
    public static final String MODID = "yield";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Singleton instance for integrations (JEI) that cannot use DI
    private static Yield instance;

    // Core Services
    private final ProjectManager projectManager;
    private final SessionTracker sessionTracker;

    public Yield(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;

        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        // 1. Instantiate Persistence & Core Services IMMEDIATELY
        // Moving this to the constructor guarantees they exist before ANY event fires.
        ProjectRepository repository = new ProjectRepository();
        this.projectManager = new ProjectManager(repository);
        this.sessionTracker = new SessionTracker(projectManager);

        // Register Lifecycle Events
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerGuiLayers);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // 2. Logic Components (Retrieving parts from the already-instantiated tracker)
        InventoryMonitor monitor = sessionTracker.getMonitor();

        // 3. Event Handlers (The Bridge)
        // Wire up the handlers that listen to the in-game NeoForge bus
        YieldNeoForgeEventHandler forgeEventHandler = new YieldNeoForgeEventHandler(sessionTracker, monitor);
        ClientGameEvents gameInputHandler = new ClientGameEvents(projectManager, projectManager, sessionTracker, sessionTracker);

        // 4. Register to NeoForge Event Bus
        // This is safe to do here as we are setting up client-side game logic
        NeoForge.EVENT_BUS.register(forgeEventHandler);
        NeoForge.EVENT_BUS.register(gameInputHandler);

        // 5. Initial Data Load
        // Load data from disk now that the game is setting up
        projectManager.load();

        LOGGER.info("Yield Services Initialized and Wired.");
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.EFFECTS,
                ResourceLocation.fromNamespaceAndPath(Yield.MODID, "yield_hud"),
                new YieldOverlay(projectManager, sessionTracker)
        );
    }

    /**
     * Static accessor strictly for external integrations (e.g., JEI)
     * that do not support constructor injection.
     */
    public static Yield getInstance() {
        return instance;
    }

    public ProjectManager getProjectManager() {
        return projectManager;
    }
}