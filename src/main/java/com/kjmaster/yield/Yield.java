package com.kjmaster.yield;

import com.kjmaster.yield.client.YieldOverlay;
import com.kjmaster.yield.domain.GoalDomainService;
import com.kjmaster.yield.event.YieldInputHandler;
import com.kjmaster.yield.event.YieldLogicHandler;
import com.kjmaster.yield.event.internal.YieldEventBus;
import com.kjmaster.yield.manager.ProjectManager;
import com.kjmaster.yield.manager.ProjectRepository;
import com.kjmaster.yield.tracker.SessionTracker;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(value = Yield.MODID, dist = Dist.CLIENT)
public class Yield {
    public static final String MODID = "yield";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static Yield instance;

    // Service Registry
    private final YieldServices services;

    public Yield(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        // 1. Instantiate Infrastructure
        YieldEventBus eventBus = new YieldEventBus();

        // 2. Instantiate Domain Services
        GoalDomainService goalDomainService = new GoalDomainService();

        // 3. Instantiate Core Services
        ProjectRepository repository = new ProjectRepository();
        ProjectManager projectManager = new ProjectManager(repository, eventBus);
        SessionTracker sessionTracker = new SessionTracker(projectManager, eventBus);

        // 4. Wrap in Registry
        this.services = new YieldServices(projectManager, sessionTracker, eventBus, goalDomainService);

        // Register Lifecycle Events
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::registerGuiLayers);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // 5. Wire Handlers using the Registry
        YieldLogicHandler logicHandler = new YieldLogicHandler(services);
        YieldInputHandler inputHandler = new YieldInputHandler(services);

        NeoForge.EVENT_BUS.register(logicHandler);
        NeoForge.EVENT_BUS.register(inputHandler);

        // 6. Initial Data Load
        services.projectManager().load();

        LOGGER.info("Yield Services Initialized and Wired.");
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.EFFECTS,
                ResourceLocation.fromNamespaceAndPath(Yield.MODID, "yield_hud"),
                new YieldOverlay(services.projectProvider(), services.sessionStatus())
        );
    }

    public static Yield getInstance() {
        return instance;
    }

    public YieldServices getServices() {
        return services;
    }
}