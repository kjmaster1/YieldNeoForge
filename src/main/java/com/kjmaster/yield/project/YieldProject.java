package com.kjmaster.yield.project;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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

    // --- Immutable Modifiers (Pure Copy) ---

    public YieldProject withName(String newName) {
        return new YieldProject(newName, id, goals, trackXp);
    }

    public YieldProject withTrackXp(boolean newTrackXp) {
        return new YieldProject(name, id, goals, newTrackXp);
    }
}