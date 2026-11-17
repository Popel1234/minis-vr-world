package dev.miko.vrscreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class VRScreenClient implements ClientModInitializer {
    public static KeyBinding OPEN_VR;
    public static KeyBinding OPEN_AIRPODS;

    @Override
    public void onInitializeClient() {
        // Both keys are rebindable in Options > Controls > Key Binds > "VR Screen".
        OPEN_VR = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vrscreen.open_vr", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.vrscreen"));
        OPEN_AIRPODS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vrscreen.open_airpods", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "key.categories.vrscreen"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_VR.wasPressed()) {
                if (client.currentScreen == null && client.player != null) client.setScreen(new VRHeadsetScreen());
            }
            while (OPEN_AIRPODS.wasPressed()) {
                if (client.currentScreen == null && client.player != null) client.setScreen(new AirPodsScreen());
            }
        });
    }
}
