package com.kjmaster.yield.event.internal;

import com.kjmaster.yield.project.YieldProject;

public class YieldEvents {
    // Project Events
    public record ProjectUpdated(YieldProject project) {
    }

    public record ActiveProjectChanged(YieldProject newActiveProject) {
    }

    public record ProjectListChanged() {
    } // Fired on Create/Delete

    // Session Events
    public record SessionStarted() {
    }

    public record SessionStopped() {
    }
}