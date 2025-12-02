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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ProjectRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "projects.json";

    public List<YieldProject> load() {
        File file = getStorageFile();
        List<YieldProject> projects = new ArrayList<>();

        if (!file.exists()) return projects;

        try (FileReader reader = new FileReader(file)) {
            JsonElement json = GSON.fromJson(reader, JsonElement.class);

            // Fix: Check for null if file is empty
            if (json != null) {
                YieldProject.CODEC.listOf()
                        .parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> Yield.LOGGER.error("Failed to parse projects: {}", err))
                        .ifPresent(projects::addAll);
            }
        } catch (IOException e) {
            Yield.LOGGER.error("Could not load Yield projects", e);
        }
        return projects;
    }

    // Refactored: Accept file parameter to support "Capture-then-Write" (Race Condition Fix)
    public void save(List<YieldProject> projects, File targetFile) {
        var result = YieldProject.CODEC.listOf().encodeStart(JsonOps.INSTANCE, projects);

        result.resultOrPartial(Yield.LOGGER::error).ifPresent(json -> {
            try {
                // Use passed targetFile instead of re-calculating (which might fail if world closed)
                File tempFile = new File(targetFile.getParentFile(), FILE_NAME + ".tmp");

                try (FileWriter writer = new FileWriter(tempFile)) {
                    GSON.toJson(json, writer);
                }

                Files.move(
                        tempFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (IOException e) {
                Yield.LOGGER.error("Could not save Yield projects", e);
            }
        });
    }

    // Made public so ProjectManager can capture the path synchronously
    public File getStorageFile() {
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

        File folder = storageDir.toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return new File(folder, FILE_NAME);
    }
}