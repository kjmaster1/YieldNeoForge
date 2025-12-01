package com.kjmaster.yield.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.yield.Yield;
import com.kjmaster.yield.project.YieldProject;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ProjectManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "projects.json";

    private static ProjectManager INSTANCE;
    private final List<YieldProject> projects = new ArrayList<>();
    private YieldProject activeProject;

    private ProjectManager() {}

    public static synchronized ProjectManager get() {
        if (INSTANCE == null) {
            INSTANCE = new ProjectManager();
        }
        return INSTANCE;
    }

    public void createProject(String name) {
        YieldProject project = new YieldProject(name);
        projects.add(project);
        save();
    }

    public void deleteProject(YieldProject project) {
        projects.remove(project);
        if (activeProject == project) activeProject = null;
        save();
    }

    public void setActiveProject(YieldProject project) { this.activeProject = project; }
    public Optional<YieldProject> getActiveProject() { return Optional.ofNullable(activeProject); }
    public List<YieldProject> getProjects() { return projects; }

    public void clear() {
        this.projects.clear();
        this.activeProject = null;
    }

    // --- IO Logic ---

    private File getStorageFile() {
        Minecraft mc = Minecraft.getInstance();
        String folderName;
        Path storageDir;

        if (mc.getSingleplayerServer() != null) {
            // Singleplayer: Use World Name (Level Name)
            // This is safer than IP and unique per save.
            MinecraftServer server = mc.getSingleplayerServer();
            storageDir = server.getWorldPath(new LevelResource("yield"));
        } else {
            // Multiplayer: Hash the IP to ensure safe filename
            ServerData serverData = mc.getCurrentServer();
            if (serverData != null) {
                folderName = DigestUtils.sha256Hex(serverData.ip);
            } else {
                folderName = "local_fallback";
            }

            // Sanitize for safety (though hash is safe, world name might not be entirely)
            folderName = folderName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

            storageDir = FMLPaths.CONFIGDIR.get()
                    .resolve("yield")
                    .resolve("saves")
                    .resolve(folderName);
        }

        File folder = storageDir.toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return new File(folder, FILE_NAME);
    }

    public void save() {
        // 1. Prepare File and Data on Main Thread
        // We resolve the file here to ensure we get the current context (World/Server)
        File file = getStorageFile();

        // Serialize to JSON on Main Thread to ensure thread safety with Registries/Items
        YieldProject.CODEC.listOf()
                .encodeStart(JsonOps.INSTANCE, projects)
                .resultOrPartial(err -> Yield.LOGGER.error("Failed to encode projects: " + err))
                .ifPresent(json -> {
                    // 2. Write to Disk on Background Thread
                    CompletableFuture.runAsync(() -> {
                        try (FileWriter writer = new FileWriter(file)) {
                            GSON.toJson(json, writer);
                        } catch (IOException e) {
                            Yield.LOGGER.error("Could not save Yield projects", e);
                        }
                    }, Util.ioPool()); // Use Minecraft's IO Pool
                });
    }

    public void load() {
        File file = getStorageFile();
        this.projects.clear();
        this.activeProject = null;

        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            JsonElement json = GSON.fromJson(reader, JsonElement.class);
            YieldProject.CODEC.listOf()
                    .parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> Yield.LOGGER.error("Failed to parse projects: {}", err))
                    .ifPresent(this.projects::addAll);
        } catch (IOException e) {
            Yield.LOGGER.error("Could not load Yield projects", e);
        }
    }
}