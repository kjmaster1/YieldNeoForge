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
import java.util.concurrent.atomic.AtomicBoolean;

public class ProjectRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "projects.json";

    public List<YieldProject> load() {
        File file = getStorageFile();
        List<YieldProject> projects = new ArrayList<>();

        if (!file.exists()) return projects;

        try (FileReader reader = new FileReader(file)) {
            JsonElement json = GSON.fromJson(reader, JsonElement.class);

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

    /**
     * Saves the projects list to disk.
     * @return true if successful, false otherwise.
     */
    public boolean save(List<YieldProject> projects, File targetFile) {
        var result = YieldProject.CODEC.listOf().encodeStart(JsonOps.INSTANCE, projects);
        AtomicBoolean success = new AtomicBoolean(true);

        result.resultOrPartial(err -> {
            Yield.LOGGER.error("Serialization error: {}", err);
            success.set(false);
        }).ifPresent(json -> {
            try {
                File tempFile = new File(targetFile.getParentFile(), FILE_NAME + ".tmp");
                // Ensure parent exists
                if (!tempFile.getParentFile().exists() && !tempFile.getParentFile().mkdirs()) {
                    throw new IOException("Could not create directory: " + tempFile.getParent());
                }

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
                success.set(false);
            }
        });

        // If result was empty (encoding failed completely), success is effectively false,
        // but the AtomicBoolean default was true. However, resultOrPartial handles logging.
        // We need to check if result is present too.
        return success.get() && result.result().isPresent();
    }

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