package com.kjmaster.yield;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // HUD Settings
    public static final ModConfigSpec.BooleanValue OVERLAY_ENABLED;
    // Changed to Double for Normalized Position (0.0 to 1.0)
    public static final ModConfigSpec.DoubleValue OVERLAY_X;
    public static final ModConfigSpec.DoubleValue OVERLAY_Y;
    public static final ModConfigSpec.ConfigValue<Integer> OVERLAY_COLOR;

    // Tracker Settings
    public static final ModConfigSpec.IntValue RATE_WINDOW;

    static {
        BUILDER.push("hud");

        OVERLAY_ENABLED = BUILDER
                .comment("Whether the Yield HUD overlay is visible.")
                .define("overlayEnabled", true);

        OVERLAY_X = BUILDER
                .comment("The X position of the HUD (0.0 to 1.0).")
                .defineInRange("overlayX", 0.05, 0.0, 1.0);

        OVERLAY_Y = BUILDER
                .comment("The Y position of the HUD (0.0 to 1.0).")
                .defineInRange("overlayY", 0.05, 0.0, 1.0);

        OVERLAY_COLOR = BUILDER
                .comment("The background color of the HUD (ARGB Hex). Default is semi-transparent black.")
                .define("overlayColor", 0x90000000);

        BUILDER.pop();

        BUILDER.push("general");

        RATE_WINDOW = BUILDER
                .comment("The time window (in seconds) for rate calculation. Lower values update faster but fluctuate more.")
                .defineInRange("rateWindow", 15, 1, 300);

        SPEC = BUILDER.build();
    }

    public static final ModConfigSpec SPEC;
}