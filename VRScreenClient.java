package dev.miko.vrscreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class VRScreenClient implements ClientModInitializer {
    public static KeyBinding OPEN_VR;
    public static KeyBinding OPEN_AIRPODS;
    private static Panel panel;
    private static Intro intro;
    private static Outro outro;

    @Override
    public void onInitializeClient() {
        System.setProperty("java.awt.headless", "true");
        // Both keys are rebindable in Options > Controls > Key Binds > "Minis VR World".
        OPEN_VR = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vrscreen.open_vr", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.vrscreen"));
        OPEN_AIRPODS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vrscreen.open_airpods", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "key.categories.vrscreen"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_VR.wasPressed()) trigger(client, Panel.Type.VR);
            while (OPEN_AIRPODS.wasPressed()) trigger(client, Panel.Type.MUSIC);
            if (panel != null) panel.tick();
        });
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            if (intro != null && intro.render(ctx)) intro = null;
            if (outro != null && outro.render(ctx)) outro = null;
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(c -> {
            if (panel != null && c.matrixStack() != null)
                panel.render(c.matrixStack().peek().getPositionMatrix(), c.camera().getPos());
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { intro = null; outro = null; closePanel(); });
    }

    private static void trigger(MinecraftClient client, Panel.Type type) {
        if (client.player == null || client.currentScreen != null || intro != null || outro != null) return;
        if (panel != null && panel.type == type) {      // second press = cursor mode on the existing screen
            client.setScreen(new PanelScreen(panel));
            return;
        }
        closePanel();
        intro = new Intro(type, client.player.getEyePos(), client.player.getYaw());
    }

    static void spawn(Panel.Type type, net.minecraft.util.math.Vec3d eye, float yaw) {
        closePanel();
        panel = new Panel(type, eye, yaw);
    }

    /** Close with the "take off headset / AirPods" animation. */
    static void closeAnimated() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (panel == null || outro != null) return;
        if (c.currentScreen instanceof PanelScreen) c.setScreen(null);
        outro = new Outro(panel.type);
    }

    public static void closePanel() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.currentScreen instanceof PanelScreen) c.setScreen(null);
        if (panel != null) { panel.dispose(); panel = null; }
    }
}
