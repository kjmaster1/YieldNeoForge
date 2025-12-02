package com.kjmaster.yield;

import com.kjmaster.yield.client.YieldOverlay;
import com.kjmaster.yield.event.YieldInputHandler;
import com.kjmaster.yield.event.YieldLogicHandler;
import com.kjmaster.yield.event.internal.YieldEventBus;
import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.manager.ProjectRepository;
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

    private static Yield instance;

    // Service Registry
    private final YieldServices services;

    public Yield(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;

        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        // 1. Instantiate Infrastructure
        YieldEventBus eventBus = new YieldEventBus();

        // 2. Instantiate Core Services
        ProjectRepository repository = new ProjectRepository();
        ProjectManager projectManager = new ProjectManager(repository, eventBus);
        SessionTracker sessionTracker = new SessionTracker(projectManager, eventBus); // Updated to take bus

        // 3. Wrap in Registry
        this.services = new YieldServices(projectManager, sessionTracker, eventBus);

        // Register Lifecycle Events
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerGuiLayers);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // 4. Wire Handlers using the Registry
        YieldLogicHandler logicHandler = new YieldLogicHandler(services);
        YieldInputHandler inputHandler = new YieldInputHandler(services);

        NeoForge.EVENT_BUS.register(logicHandler);
        NeoForge.EVENT_BUS.register(inputHandler);

        // 5. Initial Data Load
        services.projectManager().load();

        LOGGER.info("Yield Services Initialized and Wired.");
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.EFFECTS,
                ResourceLocation.fromNamespaceAndPath(Yield.MODID, "yield_hud"),
                new YieldOverlay(services)
        );
    }

    public static Yield getInstance() {
        return instance;
    }

    public YieldServices getServices() {
        return services;
    }

    public ProjectManager getProjectManager() {
        return services.projectManager();
    }
}