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
            YieldProject.CODEC.listOf()
                    .parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> Yield.LOGGER.error("Failed to parse projects: {}", err))
                    .ifPresent(projects::addAll);
        } catch (IOException e) {
            Yield.LOGGER.error("Could not load Yield projects", e);
        }
        return projects;
    }

    public void save(List<YieldProject> projects) {
        // 1. Serialize State
        // Create a deep copy or use the list if thread-safe.
        // Serialization should happen on the calling thread or safely.
        // Here we rely on the Codec to create the JsonElement.
        var result = YieldProject.CODEC.listOf().encodeStart(JsonOps.INSTANCE, projects);

        result.resultOrPartial(Yield.LOGGER::error).ifPresent(json -> {
            try {
                // 2. Prepare Files
                File targetFile = getStorageFile();
                File tempFile = new File(targetFile.getParentFile(), FILE_NAME + ".tmp");

                // 3. Write to Temp File
                try (FileWriter writer = new FileWriter(tempFile)) {
                    GSON.toJson(json, writer);
                }

                // 4. Atomic Move (Replace original with Temp)
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

    private File getStorageFile() {
        Minecraft mc = Minecraft.getInstance();
        String folderName;
        Path storageDir;

        if (mc.getSingleplayerServer() != null) {
            // Singleplayer: Use World Name (Level Name)
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

            // Sanitize for safety
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
}