package com.kjmaster.yield.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kjmaster.yield.Yield;
import com.kjmaster.yield.project.YieldProject;
import com.mojang.serialization.JsonOps;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER_NAME = "yield";
    private static final String FILE_NAME = "projects.json";

    private static ProjectManager INSTANCE;
    private final List<YieldProject> projects = new ArrayList<>();
    private YieldProject activeProject;

    private ProjectManager() {
        load();
    }

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
        if (activeProject == project) {
            activeProject = null;
        }
        save();
    }

    public void setActiveProject(YieldProject project) {
        this.activeProject = project;
    }

    public Optional<YieldProject> getActiveProject() {
        return Optional.ofNullable(activeProject);
    }

    public List<YieldProject> getProjects() {
        return projects;
    }

    // --- IO Logic ---

    public void save() {
        Path configDir = FMLPaths.CONFIGDIR.get(); // Or GAMEDIR depending on preference
        File folder = configDir.resolve(FOLDER_NAME).toFile();

        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, FILE_NAME);

        try (FileWriter writer = new FileWriter(file)) {
            // Encode List<YieldProject> using our Codec
            YieldProject.CODEC.listOf()
                    .encodeStart(JsonOps.INSTANCE, projects)
                    .resultOrPartial(err -> Yield.LOGGER.error("Failed to encode projects: " + err))
                    .ifPresent(json -> GSON.toJson(json, writer));
        } catch (IOException e) {
            Yield.LOGGER.error("Could not save Yield projects", e);
        }
    }

    public void load() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        File file = configDir.resolve(FOLDER_NAME).resolve(FILE_NAME).toFile();

        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            JsonElement json = GSON.fromJson(reader, JsonElement.class);

            YieldProject.CODEC.listOf()
                    .parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> Yield.LOGGER.error("Failed to parse projects: {}", err))
                    .ifPresent(loadedProjects -> {
                        this.projects.clear();
                        this.projects.addAll(loadedProjects);
                    });

        } catch (IOException e) {
            Yield.LOGGER.error("Could not load Yield projects", e);
        }
    }
}
