package dev.miko.vrscreen;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Cursor mode: free mouse pointer to click, scroll and type on the floating screen.
 * While the pointer is NOT over the screen, WASD/jump/sneak still move you.
 */
public class PanelScreen extends Screen {
    private final Panel panel;
    private boolean over;

    public PanelScreen(Panel panel) { super(Text.literal("Minis VR World")); this.panel = panel; }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(net.minecraft.client.gui.DrawContext c, int mx, int my, float d) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        double[] p = panel.toPx(mx, my);
        panel.setHover(p);
        over = panel.overBrowser(p);
        ctx.drawCenteredTextWithShadow(textRenderer, "Esc = back to walking", width / 2, height - 24, 0xFFFFFFFF);
    }

    @Override
    public void tick() {
        MinecraftClient c = MinecraftClient.getInstance();
        GameOptions o = c.options;
        boolean walk = !over;
        KeyBinding[] keys = {o.forwardKey, o.backKey, o.leftKey, o.rightKey, o.jumpKey, o.sprintKey, o.sneakKey};
        for (KeyBinding kb : keys) {
            InputUtil.Key k = KeyBindingHelper.getBoundKeyOf(kb);
            boolean down = walk && k.getCategory() == InputUtil.Type.KEYSYM
                    && InputUtil.isKeyPressed(c.getWindow().getHandle(), k.getCode());
            kb.setPressed(down);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        panel.click(panel.toPx(mx, my), button, true);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        panel.click(panel.toPx(mx, my), button, false);
        return true;
    }

    @Override public void mouseMoved(double mx, double my) { panel.move(panel.toPx(mx, my)); }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        panel.move(panel.toPx(mx, my));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        panel.scroll(panel.toPx(mx, my), v);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (over) panel.key(key, scan, mods, true);
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (over) panel.key(key, scan, mods, false);
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (over) panel.typed(c, mods);
        return true;
    }
}
