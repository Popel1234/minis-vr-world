package dev.miko.vrscreen;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/** AirPods animation, then a Spotify web player (home / search) in an embedded Chromium. */
public class AirPodsScreen extends Screen {
    private static final String HOME = "https://open.spotify.com";
    private static final String SEARCH = "https://open.spotify.com/search";
    private static final int ANIM_MS = 1700;

    private final long start = Util.getMeasuringTimeMs();
    private long shownSince = -1;
    private boolean animDone, chimed;
    private MCEFBrowser browser;
    private int px, py, pw, ph, bx, by, bw, bh;

    public AirPodsScreen() { super(Text.literal("Spotify")); }

    @Override public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        pw = (int) (width * 0.82); ph = (int) (height * 0.82);
        px = (width - pw) / 2; py = (height - ph) / 2;
        bx = px + 10; by = py + 34; bw = pw - 20; bh = ph - 44;
    }

    // ---------- rendering ----------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = Util.getMeasuringTimeMs();
        if (!animDone) {
            float p = Math.min(1f, (now - start) / (float) ANIM_MS);
            renderAnimation(ctx, p);
            if (p > 0.55f && !chimed) {
                chimed = true;
                MinecraftClient.getInstance().getSoundManager()
                        .play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.6f));
            }
            if (p >= 1f) { animDone = true; shownSince = now; startBrowser(); }
            return;
        }
        float fade = Math.min(1f, (now - shownSince) / 300f);
        ctx.fill(0, 0, width, height, argb(0xF2, 0x07, 0x0A, 0x08));
        Draw.roundRect(ctx, px - 2, py - 2, pw + 4, ph + 4, 18, argb((int) (60 * fade), 0x1D, 0xB9, 0x54));
        Draw.roundRect(ctx, px, py, pw, ph, 16, argb((int) (255 * fade), 0x12, 0x12, 0x12));
        renderBrowser(ctx, mx, my);
    }

    private void renderAnimation(DrawContext ctx, float p) {
        ctx.fill(0, 0, width, height, argb((int) (110 * Math.min(1f, p * 3)), 0, 0, 0));
        float t = ease(Math.min(1f, p / 0.55f));
        float y = lerp(height + 50, height / 2f, t);
        drawBud(ctx, lerp(width * 0.38f, 44f, t), y, false);
        drawBud(ctx, lerp(width * 0.62f, width - 44f, t), y, true);
        if (p > 0.55f) {
            float a = Math.min(1f, (p - 0.55f) / 0.2f);
            int ca = (int) (255 * a);
            Draw.roundRect(ctx, width / 2 - 70, height / 2 - 14, 140, 28, 14, argb((int) (230 * a), 0x1D, 0xB9, 0x54));
            ctx.drawCenteredTextWithShadow(textRenderer, "AirPods connected", width / 2, height / 2 - 4, argb(ca, 255, 255, 255));
        }
        if (p > 0.8f) ctx.fill(0, 0, width, height, argb((int) (255 * (p - 0.8f) / 0.2f), 0x07, 0x0A, 0x08));
    }

    private void drawBud(DrawContext ctx, float cx, float cy, boolean right) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy, 0);
        m.scale(1.6f, 1.6f, 1f);
        int sx = right ? 2 : -10;
        Draw.roundRect(ctx, sx, 0, 8, 30, 4, 0xFFF2F2F2);                    // stem
        Draw.roundRect(ctx, right ? -8 : -14, -14, 22, 22, 11, 0xFFFFFFFF);  // head
        Draw.roundRect(ctx, right ? -2 : -8, -8, 10, 10, 5, 0xFFD9D9DE);     // speaker mesh
        m.pop();
    }

    private void renderBrowser(DrawContext ctx, int mx, int my) {
        button(ctx, mx, my, px + 10, py + 8, 44, 18, "Home");
        button(ctx, mx, my, px + 58, py + 8, 44, 18, "Back");
        button(ctx, mx, my, px + 106, py + 8, 54, 18, "Search");
        int lx = px + pw - 28;                                              // Spotify logo
        Draw.roundRect(ctx, lx, py + 6, 20, 20, 10, 0xFF1DB954);
        ctx.fill(lx + 5, py + 12, lx + 15, py + 14, 0xFF000000);
        ctx.fill(lx + 6, py + 16, lx + 14, py + 18, 0xFF000000);
        ctx.fill(lx + 7, py + 20, lx + 13, py + 22, 0xFF000000);
        if (!MCEF.isInitialized())
            ctx.drawCenteredTextWithShadow(textRenderer, "Chromium is still downloading, please wait...", width / 2, height / 2, 0xFFFFC857);
        if (browser == null || browser.getRenderer() == null) return;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, browser.getRenderer().getTextureID());
        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(mat, bx, by + bh, 0).texture(0f, 1f).color(255, 255, 255, 255);
        b.vertex(mat, bx + bw, by + bh, 0).texture(1f, 1f).color(255, 255, 255, 255);
        b.vertex(mat, bx + bw, by, 0).texture(1f, 0f).color(255, 255, 255, 255);
        b.vertex(mat, bx, by, 0).texture(0f, 0f).color(255, 255, 255, 255);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private void button(DrawContext ctx, int mx, int my, int x, int y, int w, int h, String label) {
        Draw.roundRect(ctx, x, y, w, h, 9, in(mx, my, x, y, w, h) ? 0xFF2F2F2F : 0xFF222222);
        ctx.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + 5, 0xFFFFFFFF);
    }

    // ---------- browser ----------
    private void startBrowser() {
        if (!MCEF.isInitialized() || browser != null) return;
        int s = scale();
        browser = MCEF.createBrowser(HOME, false);
        browser.resize(bw * s, bh * s);
        browser.setFocus(true);
    }

    @Override public void removed() { if (browser != null) { browser.close(); browser = null; } }

    private int scale() { return (int) MinecraftClient.getInstance().getWindow().getScaleFactor(); }
    private int bmx(double x) { return (int) ((x - bx) * scale()); }
    private int bmy(double y) { return (int) ((y - by) * scale()); }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!animDone) return true;
        if (browser == null) startBrowser();
        if (in(mx, my, px + 10, py + 8, 44, 18)) { if (browser != null) browser.loadURL(HOME); return true; }
        if (in(mx, my, px + 58, py + 8, 44, 18)) { if (browser != null) browser.goBack(); return true; }
        if (in(mx, my, px + 106, py + 8, 54, 18)) { if (browser != null) browser.loadURL(SEARCH); return true; }
        if (browser != null && in(mx, my, bx, by, bw, bh)) {
            browser.setFocus(true);
            browser.sendMousePress(bmx(mx), bmy(my), button);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (browser != null) { browser.sendMouseRelease(bmx(mx), bmy(my), button); return true; }
        return false;
    }
    @Override public void mouseMoved(double mx, double my) { if (browser != null) browser.sendMouseMove(bmx(mx), bmy(my)); }
    @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        if (browser != null) { browser.sendMouseMove(bmx(mx), bmy(my)); return true; }
        return false;
    }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (browser != null) { browser.sendMouseWheel(bmx(mx), bmy(my), v, 0); return true; }
        return false;
    }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (browser != null) { browser.sendKeyPress(key, scan, mods); return true; }
        return false;
    }
    @Override public boolean keyReleased(int key, int scan, int mods) {
        if (browser != null) { browser.sendKeyRelease(key, scan, mods); return true; }
        return false;
    }
    @Override public boolean charTyped(char c, int mods) {
        if (browser != null) { browser.sendKeyTyped(c, mods); return true; }
        return false;
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private static int argb(int a, int r, int g, int b) { return (a & 255) << 24 | r << 16 | g << 8 | b; }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float ease(float t) { return 1f - (1f - t) * (1f - t) * (1f - t); }
}
