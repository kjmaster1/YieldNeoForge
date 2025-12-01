package com.kjmaster.yield.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyMapping OPEN_DASHBOARD = new KeyMapping(
            "key.yield.open_dashboard", // Lang key
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y, // Default to 'Y'
            "key.categories.yield" // Category
    );

    public static final KeyMapping QUICK_TRACK = new KeyMapping(
            "key.yield.quick_track",
            KeyConflictContext.GUI, // Only active in GUIs
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL, // Default to Ctrl
            "key.categories.yield"
    );
}
