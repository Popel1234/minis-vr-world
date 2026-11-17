package dev.miko.vrscreen;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class VRHeadsetScreen extends Screen {
    private enum Phase { ANIM, HOME, BROWSER }

    private record App(String name, String url, int color) {}
    private static final App[] APPS = {
            new App("YouTube", "https://www.youtube.com", 0xFFFF0033),
            new App("Twitch", "https://www.twitch.tv", 0xFF9146FF),
            new App("TikTok", "https://www.tiktok.com", 0xFF111111),
    };
    private static final String SHORTS_URL = "https://www.youtube.com/shorts";

    private static final int ANIM_MS = 1800;
    private final long start = Util.getMeasuringTimeMs();
    private long homeSince = -1;
    private Phase phase = Phase.ANIM;

    private MCEFBrowser browser;
    private App current;
    private int px, py, pw, ph, bx, by, bw, bh;

    public VRHeadsetScreen() { super(Text.literal("Minis VR World")); }

    @Override public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        pw = (int) (width * 0.82);
        ph = (int) (height * 0.82);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        bx = px + 10; by = py + 34; bw = pw - 20; bh = ph - 44;
    }

    // ---------- rendering ----------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = Util.getMeasuringTimeMs();
        if (phase == Phase.ANIM) {
            float p = Math.min(1f, (now - start) / (float) ANIM_MS);
            renderAnimation(ctx, p);
            if (p >= 1f) { phase = Phase.HOME; homeSince = now; }
            return;
        }
        float fade = Math.min(1f, (now - homeSince) / 300f);
        ctx.fill(0, 0, width, height, argb(0xF2, 0x07, 0x07, 0x0C));
        Draw.roundRect(ctx, px - 2, py - 2, pw + 4, ph + 4, 18, argb((int) (60 * fade), 0x8A, 0x7B, 0xFF));
        Draw.roundRect(ctx, px, py, pw, ph, 16, argb((int) (255 * fade), 0x14, 0x14, 0x1E));
        if (phase == Phase.HOME) renderHome(ctx, mx, my, fade); else renderBrowser(ctx, mx, my);
    }

    private void renderAnimation(DrawContext ctx, float p) {
        ctx.fill(0, 0, width, height, argb((int) (110 * Math.min(1f, p * 3)), 0, 0, 0));
        float rise = ease(Math.min(1f, p / 0.5f));
        float zoom = p > 0.5f ? 1f + ease((p - 0.5f) / 0.5f) * 9f : 1f;
        float cy = lerp(height * 1.2f, height / 2f, rise);
        var m = ctx.getMatrices();
        m.push();
        m.translate(width / 2f, cy, 0);
        m.scale(zoom, zoom, 1f);
        int w = 150, h = 64;
        Draw.roundRect(ctx, -w / 2 - 30, -8, 8, 16, 4, 0xFF2A2A33);   // strap ends
        Draw.roundRect(ctx, w / 2 + 22, -8, 8, 16, 4, 0xFF2A2A33);
        Draw.roundRect(ctx, -w / 2, -h / 2, w, h, 18, 0xFF1C1C24);     // body
        Draw.roundRect(ctx, -w / 2 + 4, -h / 2 + 4, w - 8, h - 8, 15, 0xFF24242E);
        int glow = p > 0.45f ? 0xFF8A7BFF : 0xFF3A3A48;                // lenses light up
        Draw.roundRect(ctx, -w / 2 + 14, -h / 2 + 12, 52, 40, 20, glow);
        Draw.roundRect(ctx, w / 2 - 66, -h / 2 + 12, 52, 40, 20, glow);
        m.pop();
        if (p > 0.55f) {
            int a = (int) (255 * Math.min(1f, (p - 0.55f) / 0.4f));
            ctx.fill(0, 0, width, height, argb(a, 0x07, 0x07, 0x0C));
        }
    }

    private void renderHome(DrawContext ctx, int mx, int my, float fade) {
        ctx.drawCenteredTextWithShadow(textRenderer, "Home", width / 2, py + 14, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "Choose an app", width / 2, py + 28, 0xFF8A8AA0);
        for (int i = 0; i < APPS.length; i++) {
            int[] r = tile(i);
            boolean hover = in(mx, my, r[0], r[1], r[2], r[3] + 16);
            int grow = hover ? 4 : 0;
            Draw.roundRect(ctx, r[0] - grow, r[1] - grow, r[2] + grow * 2, r[3] + grow * 2, 22,
                    hover ? 0xFF2B2B3C : 0xFF1F1F2C);
            drawLogo(ctx, i, r[0] + 14, r[1] + 14, r[2] - 28);
            ctx.drawCenteredTextWithShadow(textRenderer, APPS[i].name(), r[0] + r[2] / 2, r[1] + r[3] + 6, 0xFFFFFFFF);
        }
        if (!MCEF.isInitialized()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "Chromium is still downloading, please wait...",
                    width / 2, py + ph - 20, 0xFFFFC857);
        }
        ctx.drawCenteredTextWithShadow(textRenderer, "Esc = take off headset", width / 2, py + ph - 8, 0xFF5A5A70);
    }

    private void drawLogo(DrawContext ctx, int i, int x, int y, int s) {
        Draw.roundRect(ctx, x, y, s, s, 18, APPS[i].color());
        int cx = x + s / 2, cy = y + s / 2;
        switch (i) {
            case 0 -> { // YouTube
                Draw.roundRect(ctx, x + 10, cy - s / 5, s - 20, s * 2 / 5, 10, 0xFFFFFFFF);
                Draw.play(ctx, cx, cy, s / 4, 0xFFFF0033);
            }
            case 1 -> { // Twitch
                Draw.roundRect(ctx, cx - s / 4, cy - s / 4, s / 2, s / 2 - 2, 4, 0xFFFFFFFF);
                ctx.fill(cx - 7, cy - s / 7, cx - 4, cy + 2, 0xFF9146FF);
                ctx.fill(cx + 4, cy - s / 7, cx + 7, cy + 2, 0xFF9146FF);
            }
            default -> { // TikTok
                var m = ctx.getMatrices();
                m.push();
                m.translate(cx, cy, 0);
                m.scale(3.2f, 3.2f, 1f);
                ctx.drawText(textRenderer, "\u266A", -3, -5, 0xFF25F4EE, false);
                ctx.drawText(textRenderer, "\u266A", -1, -4, 0xFFFE2C55, false);
                ctx.drawText(textRenderer, "\u266A", -2, -4, 0xFFFFFFFF, false);
                m.pop();
            }
        }
    }

    private void renderBrowser(DrawContext ctx, int mx, int my) {
        button(ctx, mx, my, px + 10, py + 8, 44, 18, "Home");
        button(ctx, mx, my, px + 58, py + 8, 44, 18, "Back");
        if (current != null && current.name().equals("YouTube")) {
            int[] sb = shortsButton();
            Draw.roundRect(ctx, sb[0], sb[1], sb[2], sb[3], 9, in(mx, my, sb[0], sb[1], sb[2], sb[3]) ? 0xFFFF3355 : 0xFFD0002B);
            ctx.drawCenteredTextWithShadow(textRenderer, "Shorts", sb[0] + sb[2] / 2, sb[1] + 5, 0xFFFFFFFF);
        }
        if (current != null) ctx.drawTextWithShadow(textRenderer, current.name(), px + pw - 14 - textRenderer.getWidth(current.name()), py + 13, 0xFFB5B5C8);

        if (browser == null || browser.getRenderer() == null) {
            ctx.drawCenteredTextWithShadow(textRenderer, "Loading...", width / 2, height / 2, 0xFFB5B5C8);
            return;
        }
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
        Draw.roundRect(ctx, x, y, w, h, 9, in(mx, my, x, y, w, h) ? 0xFF3A3A50 : 0xFF26263A);
        ctx.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + 5, 0xFFFFFFFF);
    }

    // ---------- app / browser handling ----------
    private void openApp(String url, App app) {
        if (!MCEF.isInitialized()) return;
        current = app;
        closeBrowser();
        int s = scale();
        browser = MCEF.createBrowser(url, false);
        browser.resize(bw * s, bh * s);
        browser.setFocus(true);
        phase = Phase.BROWSER;
    }

    private void closeBrowser() {
        if (browser != null) { browser.close(); browser = null; }
    }

    private void goHome() { closeBrowser(); current = null; phase = Phase.HOME; }

    @Override
    public void removed() { closeBrowser(); }

    private int scale() { return (int) MinecraftClient.getInstance().getWindow().getScaleFactor(); }
    private int bmx(double x) { return (int) ((x - bx) * scale()); }
    private int bmy(double y) { return (int) ((y - by) * scale()); }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase == Phase.HOME) {
            for (int i = 0; i < APPS.length; i++) {
                int[] r = tile(i);
                if (in(mx, my, r[0], r[1], r[2], r[3] + 16)) { openApp(APPS[i].url(), APPS[i]); return true; }
            }
        } else if (phase == Phase.BROWSER) {
            if (in(mx, my, px + 10, py + 8, 44, 18)) { goHome(); return true; }
            if (in(mx, my, px + 58, py + 8, 44, 18)) { if (browser != null) browser.goBack(); return true; }
            int[] sb = shortsButton();
            if (current != null && current.name().equals("YouTube") && in(mx, my, sb[0], sb[1], sb[2], sb[3])) {
                if (browser != null) browser.loadURL(SHORTS_URL);
                return true;
            }
            if (browser != null && in(mx, my, bx, by, bw, bh)) {
                browser.setFocus(true);
                browser.sendMousePress(bmx(mx), bmy(my), button);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (phase == Phase.BROWSER && browser != null) { browser.sendMouseRelease(bmx(mx), bmy(my), button); return true; }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (phase == Phase.BROWSER && browser != null) browser.sendMouseMove(bmx(mx), bmy(my));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (phase == Phase.BROWSER && browser != null) { browser.sendMouseMove(bmx(mx), bmy(my)); return true; }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (phase == Phase.BROWSER && browser != null) { browser.sendMouseWheel(bmx(mx), bmy(my), v, 0); return true; }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (phase == Phase.BROWSER) goHome(); else close();
            return true;
        }
        if (phase == Phase.BROWSER && browser != null) { browser.sendKeyPress(key, scan, mods); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (phase == Phase.BROWSER && browser != null) { browser.sendKeyRelease(key, scan, mods); return true; }
        return super.keyReleased(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (phase == Phase.BROWSER && browser != null) { browser.sendKeyTyped(c, mods); return true; }
        return super.charTyped(c, mods);
    }

    // ---------- layout helpers ----------
    private int[] tile(int i) {
        int s = 84, gap = 40;
        int total = APPS.length * s + (APPS.length - 1) * gap;
        int x = (width - total) / 2 + i * (s + gap);
        return new int[]{x, height / 2 - s / 2 - 8, s, s};
    }

    private int[] shortsButton() { return new int[]{px + 108, py + 8, 54, 18}; }
    private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private static int argb(int a, int r, int g, int b) { return (a & 255) << 24 | r << 16 | g << 8 | b; }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float ease(float t) { return 1f - (1f - t) * (1f - t) * (1f - t); }
}
