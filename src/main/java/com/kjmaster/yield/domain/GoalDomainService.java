package com.kjmaster.yield.domain;

import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoalDomainService {

    public YieldProject addGoal(YieldProject project, ProjectGoal goal) {
        List<ProjectGoal> newGoals = new ArrayList<>(project.goals());

        boolean merged = false;
        for (int i = 0; i < newGoals.size(); i++) {
            ProjectGoal existing = newGoals.get(i);

            if (shouldMerge(existing, goal)) {
                // Preserving existing strictness prevents "Erasure of Strict Mode"
                // We prioritize the user's saved configuration over the transient "Quick Track" state.
                ProjectGoal mergedGoal = new ProjectGoal(
                        existing.id(),
                        existing.item(),
                        existing.targetAmount() + goal.targetAmount(),
                        existing.strict(), // ALWAYS use existing.strict()
                        existing.components(), // ALWAYS use existing.components()
                        existing.targetTag(),
                        existing.ignoredComponents()
                );
                newGoals.set(i, mergedGoal);
                merged = true;
                break;
            }
        }

        if (!merged) {
            newGoals.add(goal);
        }
        return new YieldProject(project.name(), project.id(), Collections.unmodifiableList(newGoals), project.trackXp());
    }

    public YieldProject removeGoal(YieldProject project, ProjectGoal goal) {
        List<ProjectGoal> newGoals = new ArrayList<>(project.goals());
        newGoals.removeIf(g -> g.id().equals(goal.id()));
        return new YieldProject(project.name(), project.id(), Collections.unmodifiableList(newGoals), project.trackXp());
    }

    public YieldProject updateGoal(YieldProject project, ProjectGoal newGoalData) {
        List<ProjectGoal> newGoals = new ArrayList<>(project.goals());
        for (int i = 0; i < newGoals.size(); i++) {
            if (newGoals.get(i).id().equals(newGoalData.id())) {
                newGoals.set(i, newGoalData);
                break;
            }
        }
        return new YieldProject(project.name(), project.id(), Collections.unmodifiableList(newGoals), project.trackXp());
    }

    /**
     * Determines if a new goal should be merged into an existing one.
     */
    private boolean shouldMerge(ProjectGoal existing, ProjectGoal newGoal) {
        // 1. Basic Item Match
        if (existing.item() != newGoal.item()) return false;

        // 2. If the user is manually adding a Strict goal, only merge if the existing one is also strict and matches.
        if (newGoal.strict()) {
            return existing.strict() && existing.components().equals(newGoal.components());
        }

        // 3. If the new goal is Fuzzy (Quick Track), always merge into ANY existing goal for this item.
        // This prevents "Quick Track" from creating a duplicate loose goal alongside a strict one.
        return true;
    }
}