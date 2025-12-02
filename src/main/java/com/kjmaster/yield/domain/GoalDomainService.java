package com.kjmaster.yield.domain;

import com.kjmaster.yield.project.ProjectGoal;
import com.kjmaster.yield.project.YieldProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain Service encapsulating business logic for goal manipulation.
 */
public class GoalDomainService {

    public YieldProject addGoal(YieldProject project, ProjectGoal goal) {
        List<ProjectGoal> newGoals = new ArrayList<>(project.goals());

        // Merge Logic: If item matches, replace it with updated amount
        boolean merged = false;
        for (int i = 0; i < newGoals.size(); i++) {
            ProjectGoal existing = newGoals.get(i);
            // Simple fuzzy merge logic for new additions
            if (isFuzzyMatch(existing, goal)) {
                ProjectGoal mergedGoal = new ProjectGoal(
                        existing.id(), // Keep original ID to preserve tracking!
                        existing.item(),
                        existing.targetAmount() + goal.targetAmount(),
                        existing.strict(),
                        existing.components(),
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

    private boolean isFuzzyMatch(ProjectGoal existing, ProjectGoal newGoal) {
        boolean basicMatch = existing.item() == newGoal.item()
                && existing.strict() == newGoal.strict()
                && existing.targetTag().equals(newGoal.targetTag());

        if (!basicMatch) return false;

        // Fix: Check components if strict mode is active
        if (existing.strict()) {
            return existing.components().equals(newGoal.components());
        }

        return true;
    }
}