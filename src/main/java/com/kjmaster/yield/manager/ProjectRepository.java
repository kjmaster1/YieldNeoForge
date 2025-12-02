package com.kjmaster.yield.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.yield.Yield;
import com.kjmaster.yield.project.YieldProject;
import com.mojang.serialization.JsonOps;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIR_NAME = "projects";

    /**
     * Loads all projects from the project directory.
     */
    public List<YieldProject> loadAll() {
        File dir = getStorageDirectory();
        List<YieldProject> projects = new ArrayList<>();

        if (!dir.exists() || !dir.isDirectory()) return projects;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return projects;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                JsonElement json = GSON.fromJson(reader, JsonElement.class);
                if (json != null) {
                    YieldProject.CODEC.parse(JsonOps.INSTANCE, json)
                            .resultOrPartial(err -> Yield.LOGGER.error("Failed to parse project {}: {}", file.getName(), err))
                            .ifPresent(projects::add);
                }
            } catch (IOException e) {
                Yield.LOGGER.error("Could not load project file: " + file.getName(), e);
            }
        }
        return projects;
    }

    /**
     * Saves a specific project to its own file.
     */
    public boolean saveProject(YieldProject project) {
        File dir = getStorageDirectory();
        if (!dir.exists() && !dir.mkdirs()) {
            Yield.LOGGER.error("Could not create project directory: {}", dir.getAbsolutePath());
            return false;
        }

        File file = new File(dir, project.id().toString() + ".json");

        return YieldProject.CODEC.encodeStart(JsonOps.INSTANCE, project)
                .resultOrPartial(err -> Yield.LOGGER.error("Serialization error for project {}: {}", project.name(), err))
                .map(json -> {
                    try (FileWriter writer = new FileWriter(file)) {
                        GSON.toJson(json, writer);
                        return true;
                    } catch (IOException e) {
                        Yield.LOGGER.error("Could not save project: " + project.name(), e);
                        return false;
                    }
                }).orElse(false);
    }

    public void deleteProject(YieldProject project) {
        File dir = getStorageDirectory();
        File file = new File(dir, project.id().toString() + ".json");
        if (file.exists()) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                Yield.LOGGER.error("Failed to delete project file: " + file.getName(), e);
            }
        }
    }

    public File getStorageDirectory() {
        Minecraft mc = Minecraft.getInstance();
        String folderName;
        Path storageDir;

        if (mc.getSingleplayerServer() != null) {
            MinecraftServer server = mc.getSingleplayerServer();
            storageDir = server.getWorldPath(new LevelResource("yield"));
        } else {
            ServerData serverData = mc.getCurrentServer();
            if (serverData != null) {
                String rawId = serverData.ip + "_" + serverData.name;
                folderName = DigestUtils.sha256Hex(rawId);
            } else {
                folderName = "local_fallback";
            }

            storageDir = FMLPaths.CONFIGDIR.get()
                    .resolve("yield")
                    .resolve("saves")
                    .resolve(folderName);
        }

        return storageDir.resolve(DIR_NAME).toFile();
    }

    // Legacy method stub for compatibility during transition if needed
    public File getStorageFile() {
        return getStorageDirectory();
    }
}