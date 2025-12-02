package com.kjmaster.yield.project;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record YieldProject(
        String name,
        UUID id,
        List<ProjectGoal> goals,
        boolean trackXp
) {
    public static final Codec<YieldProject> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(YieldProject::name),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("id").forGetter(YieldProject::id),
            ProjectGoal.CODEC.listOf().fieldOf("goals").forGetter(YieldProject::goals),
            Codec.BOOL.optionalFieldOf("track_xp", false).forGetter(YieldProject::trackXp)
    ).apply(instance, YieldProject::new));

    // New Project Constructor
    public YieldProject(String name) {
        this(name, UUID.randomUUID(), Collections.emptyList(), false);
    }

    // --- Immutable Modifiers ---

    public YieldProject withName(String newName) {
        return new YieldProject(newName, id, goals, trackXp);
    }

    public YieldProject withTrackXp(boolean newTrackXp) {
        return new YieldProject(name, id, goals, newTrackXp);
    }

    public YieldProject addGoal(ProjectGoal goal) {
        List<ProjectGoal> newGoals = new ArrayList<>(goals);

        // Merge Logic: If item matches, replace it with updated amount
        // Note: We use ID matching for updates, or fuzzy matching for new stacks
        boolean merged = false;
        for (int i = 0; i < newGoals.size(); i++) {
            ProjectGoal existing = newGoals.get(i);
            // Simple fuzzy merge logic for new additions
            if (existing.item() == goal.item() && existing.strict() == goal.strict() && existing.targetTag().equals(goal.targetTag())) {
                ProjectGoal mergedGoal = new ProjectGoal(
                        existing.id(), // Keep original ID to preserve tracking!
                        existing.item(),
                        existing.targetAmount() + goal.targetAmount(),
                        existing.strict(),
                        existing.components(),
                        existing.targetTag()
                );
                newGoals.set(i, mergedGoal);
                merged = true;
                break;
            }
        }

        if (!merged) {
            newGoals.add(goal);
        }
        return new YieldProject(name, id, Collections.unmodifiableList(newGoals), trackXp);
    }

    public YieldProject removeGoal(ProjectGoal goal) {
        List<ProjectGoal> newGoals = new ArrayList<>(goals);
        newGoals.removeIf(g -> g.id().equals(goal.id()));
        return new YieldProject(name, id, Collections.unmodifiableList(newGoals), trackXp);
    }

    public YieldProject updateGoal(ProjectGoal newGoalData) {
        List<ProjectGoal> newGoals = new ArrayList<>(goals);
        for (int i = 0; i < newGoals.size(); i++) {
            if (newGoals.get(i).id().equals(newGoalData.id())) {
                newGoals.set(i, newGoalData);
                break;
            }
        }
        return new YieldProject(name, id, Collections.unmodifiableList(newGoals), trackXp);
    }
}