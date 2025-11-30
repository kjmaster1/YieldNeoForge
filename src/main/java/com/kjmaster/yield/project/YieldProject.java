package com.kjmaster.yield.project;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class YieldProject {

    // Helper codec for UUIDs as strings
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<YieldProject> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(YieldProject::getName),
            UUID_CODEC.fieldOf("id").forGetter(YieldProject::getId),
            ProjectGoal.CODEC.listOf().fieldOf("goals").forGetter(YieldProject::getGoals)
    ).apply(instance, YieldProject::new));

    private String name;
    private final UUID id;
    private final List<ProjectGoal> goals;

    // Constructor for deserialization
    public YieldProject(String name, UUID id, List<ProjectGoal> goals) {
        this.name = name;
        this.id = id;
        this.goals = new ArrayList<>(goals); // Ensure mutable list
    }

    // Constructor for new projects
    public YieldProject(String name) {
        this(name, UUID.randomUUID(), new ArrayList<>());
    }

    public void addGoal(ProjectGoal goal) {
        // Merge logic: if item exists, just add to target amount
        for (ProjectGoal existing : goals) {
            if (existing.getItem() == goal.getItem()) {
                existing.setTargetAmount(existing.getTargetAmount() + goal.getTargetAmount());
                return;
            }
        }
        this.goals.add(goal);
    }

    public void removeGoal(ProjectGoal goal) {
        this.goals.remove(goal);
    }

    public List<ProjectGoal> getGoals() {
        return goals;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }
}
