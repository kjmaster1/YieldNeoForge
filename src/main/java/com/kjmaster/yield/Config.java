package com.kjmaster.yield;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // HUD Settings
    public static final ModConfigSpec.BooleanValue OVERLAY_ENABLED;
    public static final ModConfigSpec.IntValue OVERLAY_X;
    public static final ModConfigSpec.IntValue OVERLAY_Y;
    public static final ModConfigSpec.ConfigValue<Integer> OVERLAY_COLOR;

    static {
        BUILDER.push("hud");

        OVERLAY_ENABLED = BUILDER
                .comment("Whether the Yield HUD overlay is visible.")
                .define("overlayEnabled", true);

        OVERLAY_X = BUILDER
                .comment("The X position of the HUD on the screen.")
                .defineInRange("overlayX", 10, 0, Integer.MAX_VALUE);

        OVERLAY_Y = BUILDER
                .comment("The Y position of the HUD on the screen.")
                .defineInRange("overlayY", 10, 0, Integer.MAX_VALUE);

        OVERLAY_COLOR = BUILDER
                .comment("The background color of the HUD (ARGB Hex). Default is semi-transparent black.")
                .define("overlayColor", 0x90000000);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static final ModConfigSpec SPEC;
}