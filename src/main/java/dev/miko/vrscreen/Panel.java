package dev.miko.vrscreen;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** A floating screen standing in the world at the spot where it was opened. */
public final class Panel {
    public enum Type { VR, MUSIC }
    private enum Page { HOME, BROWSER }
    private record Btn(String id, int x, int y, int w, int h) {
        boolean hit(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }

    // UI texture is 1280x720; the browser fills the content area below the top bar.
    static final int UW = 1280, UH = 720, CX = 24, CY = 84, CW = 1232, CH = 612;
    static final double W = 1.9, H = W * UH / UW;
    /** Scroll strength. If scrolling feels too fast/slow, change this number (negative = inverted). */
    static final float SCROLL = 100f;

    static final String[] URLS = {"https://www.youtube.com", "https://www.twitch.tv", "https://www.tiktok.com", "https://www.google.com"};
    static final String[] NAMES = {"YouTube", "Twitch", "TikTok", "Browser"};
    static final int[] LOGOS = {0, 1, 2, 4};
    static final String YTM = "https://music.youtube.com";
    static final String YTM_SEARCH = "https://music.youtube.com/search";
    static final String SHORTS = "https://www.youtube.com/shorts";

    public final Type type;
    private final Vec3d center;
    private Vec3d right, normal;
    private double faceAngle;
    private long lastFace;
    private final long born = Util.getMeasuringTimeMs();
    private Page page;
    private String appName = "";
    private String pendingUrl;
    private MCEFBrowser browser;
    private final BufferedImage img = new BufferedImage(UW, UH, BufferedImage.TYPE_INT_ARGB);
    private Gfx.Tex tex;
    private String hover = "";
    private boolean dirty = true, chromiumReady;
    private final List<Btn> btns = new ArrayList<>();

    public Panel(Type type, Vec3d eye, float yaw) {
        this.type = type;
        double r = Math.toRadians(yaw);
        Vec3d f = new Vec3d(-Math.sin(r), 0, Math.cos(r));
        center = eye.add(f.multiply(2.3)).add(0, -0.1, 0);
        right = new Vec3d(-f.z, 0, f.x);
        normal = f.multiply(-1);
        faceAngle = Math.atan2(f.z, f.x);
        lastFace = born;
        if (type == Type.MUSIC) { page = Page.BROWSER; appName = "YouTube Music"; pendingUrl = YTM; }
        else page = Page.HOME;
    }

    // ------------------------------------------------------------------ lifecycle
    public void tick() {
        boolean ready = MCEF.isInitialized();
        if (ready != chromiumReady) { chromiumReady = ready; dirty = true; }
        if (pendingUrl != null && browser == null && ready) {
            browser = MCEF.createBrowser(pendingUrl, false);
            browser.resize(CW, CH);
            browser.setFocus(true);
            pendingUrl = null;
            dirty = true;
        }
        if (dirty) { dirty = false; redraw(); }
    }

    public void dispose() {
        closeBrowser();
        if (tex != null) { tex.delete(); tex = null; }
    }

    private void closeBrowser() {
        if (browser != null) { browser.close(); browser = null; }
    }

    // ------------------------------------------------------------------ input mapping
    /** Maps a mouse position (GUI coordinates) to UI pixels on the panel, or null if the ray misses the plane. */
    public double[] toPx(double mx, double my) {
        MinecraftClient c = MinecraftClient.getInstance();
        Camera cam = c.gameRenderer.getCamera();
        double yaw = Math.toRadians(cam.getYaw()), pit = Math.toRadians(cam.getPitch());
        Vec3d fwd = new Vec3d(-Math.sin(yaw) * Math.cos(pit), -Math.sin(pit), Math.cos(yaw) * Math.cos(pit));
        Vec3d rt = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3d up = rt.crossProduct(fwd);
        double w = c.getWindow().getScaledWidth(), h = c.getWindow().getScaledHeight();
        double tan = Math.tan(Math.toRadians(c.options.getFov().getValue()) / 2.0);
        double nx = (mx / w) * 2 - 1, ny = 1 - (my / h) * 2;
        Vec3d d = fwd.add(rt.multiply(nx * tan * w / h)).add(up.multiply(ny * tan));
        Vec3d o = cam.getPos();
        double denom = d.dotProduct(normal);
        if (Math.abs(denom) < 1e-6) return null;
        double t = center.subtract(o).dotProduct(normal) / denom;
        if (t <= 0) return null;
        Vec3d hit = o.add(d.multiply(t)).subtract(center);
        double u = hit.dotProduct(right), v = hit.y;
        return new double[]{(u / W + 0.5) * UW, (0.5 - v / H) * UH};
    }

    public boolean inside(double[] p) { return p != null && p[0] >= 0 && p[0] < UW && p[1] >= 0 && p[1] < UH; }

    public void setHover(double[] p) {
        String h = "";
        if (inside(p)) for (Btn b : btns) if (b.hit(p[0], p[1])) { h = b.id; break; }
        if (!h.equals(hover)) { hover = h; dirty = true; }
    }

    private boolean inBrowser(double px, double py) {
        return page == Page.BROWSER && browser != null && px >= CX && px < CX + CW && py >= CY && py < CY + CH;
    }

    private int bx(double px) { return (int) (px - CX); }
    private int by(double py) { return (int) (py - CY); }

    public void click(double[] p, int button, boolean down) {
        if (!inside(p)) return;
        if (down) for (Btn b : btns) if (b.hit(p[0], p[1])) { action(b.id); return; }
        if (browser != null && (inBrowser(p[0], p[1]) || !down)) {
            if (down) { browser.setFocus(true); browser.sendMousePress(bx(p[0]), by(p[1]), button); }
            else browser.sendMouseRelease(bx(p[0]), by(p[1]), button);
        }
    }

    public void move(double[] p) { if (inside(p) && browser != null && page == Page.BROWSER) browser.sendMouseMove(bx(p[0]), by(p[1])); }

    public void scroll(double[] p, double amount) {
        if (inside(p) && inBrowser(p[0], p[1])) {
            browser.sendMouseMove(bx(p[0]), by(p[1]));
            browser.sendMouseWheel(bx(p[0]), by(p[1]), amount * SCROLL, 0);
        }
    }

    public boolean overBrowser(double[] p) { return inside(p) && inBrowser(p[0], p[1]); }
    public void key(int key, int scan, int mods, boolean down) {
        if (browser == null) return;
        if (down) browser.sendKeyPress(key, scan, mods); else browser.sendKeyRelease(key, scan, mods);
    }
    public void typed(char c, int mods) { if (browser != null) browser.sendKeyTyped(c, mods); }

    private void action(String id) {
        switch (id) {
            case "close" -> VRScreenClient.closeAnimated();
            case "home" -> {
                if (type == Type.VR) { closeBrowser(); pendingUrl = null; page = Page.HOME; appName = ""; }
                else if (browser != null) browser.loadURL(YTM);
                dirty = true;
            }
            case "back" -> { if (browser != null) browser.goBack(); }
            case "shorts" -> { if (browser != null) browser.loadURL(SHORTS); }
            case "search" -> { if (browser != null) browser.loadURL(YTM_SEARCH); }
            default -> {
                if (id.startsWith("app")) {
                    int i = Integer.parseInt(id.substring(3));
                    closeBrowser();
                    appName = NAMES[i];
                    pendingUrl = URLS[i];
                    page = Page.BROWSER;
                    dirty = true;
                }
            }
        }
    }

    // ------------------------------------------------------------------ UI drawing (Java2D, anti-aliased)
    private void redraw() {
        Graphics2D g = Sprites.g2(img);
        boolean vr = type == Type.VR;
        g.setComposite(AlphaComposite.Src);
        g.setPaint(new GradientPaint(0, 0, vr ? new Color(0x1B1C2E) : new Color(0x2A1218),
                0, UH, vr ? new Color(0x0B0C15) : new Color(0x120709)));
        g.fillRect(0, 0, UW, UH);
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(new Color(255, 255, 255, 14));
        g.fillRect(0, 0, UW, 72);
        btns.clear();

        int x = 24;
        if (vr) {
            if (page == Page.BROWSER) {
                x = button(g, "home", "Home", x, 104, null);
                x = button(g, "back", "Back", x, 104, null);
                if ("YouTube".equals(appName)) button(g, "shorts", "Shorts", x, 124, new Color(0xD0002B));
            }
        } else {
            x = button(g, "home", "Home", x, 104, null);
            x = button(g, "back", "Back", x, 104, null);
            button(g, "search", "Search", x, 120, null);
        }
        String title = vr ? (page == Page.BROWSER ? appName : "Minis VR World") : "YouTube Music";
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        g.setColor(new Color(0xC8CBE0));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, UW / 2 - fm.stringWidth(title) / 2 + 150, 46);

        // close button
        boolean hc = hover.equals("close");
        g.setColor(hc ? new Color(0xE5484D) : new Color(0x2C2E44));
        g.fill(new Ellipse2D.Float(UW - 64, 16, 40, 40));
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(new Line2D.Float(UW - 52, 28, UW - 36, 44));
        g.draw(new Line2D.Float(UW - 36, 28, UW - 52, 44));
        btns.add(new Btn("close", UW - 64, 16, 40, 40));

        if (vr && page == Page.HOME) drawHome(g);
        else drawBackdrop(g);
        g.dispose();

        if (tex == null) tex = Gfx.Tex.create(img); else tex.update(img);
    }

    private int button(Graphics2D g, String id, String label, int x, int w, Color base) {
        boolean h = hover.equals(id);
        Color c = base != null ? (h ? base.brighter() : base) : (h ? new Color(0x42456A) : new Color(0x2C2E44));
        g.setColor(c);
        g.fill(new RoundRectangle2D.Float(x, 16, w, 40, 40, 40));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x + (w - fm.stringWidth(label)) / 2, 43);
        btns.add(new Btn(id, x, 16, w, 40));
        return x + w + 12;
    }

    private void center(Graphics2D g, String s, int y, Font f, Color c) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, UW / 2 - g.getFontMetrics().stringWidth(s) / 2, y);
    }

    private void drawHome(Graphics2D g) {
        center(g, "Minis VR World", 180, new Font(Font.SANS_SERIF, Font.BOLD, 58), Color.WHITE);
        center(g, "Choose an app", 224, new Font(Font.SANS_SERIF, Font.PLAIN, 26), new Color(0x8D90AC));
        for (int i = 0; i < NAMES.length; i++) {
            int cx = 205 + i * 290;
            boolean h = hover.equals("app" + i);
            int grow = h ? 10 : 0;
            g.setColor(h ? new Color(0x34375A) : new Color(0x24263C));
            g.fill(new RoundRectangle2D.Float(cx - 100 - grow, 262 - grow, 200 + 2 * grow, 200 + 2 * grow, 52, 52));
            Sprites.logo(g, LOGOS[i], cx - 65 - grow / 2f, 297 - grow / 2f, 130 + grow);
            center2(g, NAMES[i], cx, 512);
            btns.add(new Btn("app" + i, cx - 100, 262, 200, 280));
        }
        if (!chromiumReady)
            center(g, "Chromium is still downloading, please wait...", 650, new Font(Font.SANS_SERIF, Font.PLAIN, 24), new Color(0xFFC857));
    }

    private void center2(Graphics2D g, String s, int cx, int y) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        g.setColor(Color.WHITE);
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
    }

    private void drawBackdrop(Graphics2D g) {
        g.setColor(new Color(0x0A0A10));
        g.fill(new RoundRectangle2D.Float(CX, CY, CW, CH, 28, 28));
        String msg = chromiumReady ? "Loading..." : "Chromium is still downloading, please wait...";
        center(g, msg, CY + CH / 2, new Font(Font.SANS_SERIF, Font.PLAIN, 30), new Color(0x8D90AC));
    }

    // ------------------------------------------------------------------ world rendering
    /** The screen stays where it was opened but turns (around the vertical axis only) to face you, smoothly. */
    private void updateFacing(Vec3d cam) {
        double dx = center.x - cam.x, dz = center.z - cam.z;
        long now = Util.getMeasuringTimeMs();
        if (dx * dx + dz * dz > 1e-4) {
            double target = Math.atan2(dz, dx);
            double dt = Math.min(0.1, (now - lastFace) / 1000.0);
            double diff = Math.atan2(Math.sin(target - faceAngle), Math.cos(target - faceAngle));
            faceAngle += diff * (1 - Math.exp(-dt * 10));
            Vec3d f = new Vec3d(Math.cos(faceAngle), 0, Math.sin(faceAngle));
            right = new Vec3d(-f.z, 0, f.x);
            normal = f.multiply(-1);
        }
        lastFace = now;
    }

    public void render(Matrix4f m, Vec3d cam) {
        if (tex == null) return;
        updateFacing(cam);
        float e = Gfx.ease((Util.getMeasuringTimeMs() - born) / 380f);
        float sc = 0.82f + 0.18f * e;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();

        int accent = type == Type.VR ? 0x8A7BFF : 0xFF3355;
        int[] grow = {30, 20, 11, 4};
        int[] alpha = {14, 22, 34, 80};
        for (int i = 0; i < grow.length; i++)
            fan(m, cam, -grow[i], -grow[i], UW + 2 * grow[i], UH + 2 * grow[i], 40 + grow[i], -0.012, sc,
                    -1, accent, (int) (alpha[i] * e), 0, 0, 1, 1);
        fan(m, cam, 0, 0, UW, UH, 40, 0, sc, tex.id, 0xFFFFFF, (int) (255 * e), 0, 0, UW, UH);
        if (page == Page.BROWSER && browser != null && browser.getRenderer() != null)
            fan(m, cam, CX, CY, CW, CH, 26, 0.006, sc, browser.getRenderer().getTextureID(), 0xFFFFFF,
                    (int) (255 * e), CX, CY, CW, CH);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void fan(Matrix4f m, Vec3d cam, float x, float y, float w, float h, float r, double off, float sc,
                     int texId, int rgb, int alpha, float mx, float my, float mw, float mh) {
        boolean textured = texId >= 0;
        if (textured) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, texId);
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        }
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN,
                textured ? VertexFormats.POSITION_TEXTURE_COLOR : VertexFormats.POSITION_COLOR);
        float[][] pts = Gfx.roundedPoly(x, y, w, h, r, 10);
        vert(b, m, cam, x + w / 2f, y + h / 2f, off, sc, textured, rgb, alpha, mx, my, mw, mh);
        for (float[] p : pts) vert(b, m, cam, p[0], p[1], off, sc, textured, rgb, alpha, mx, my, mw, mh);
        vert(b, m, cam, pts[0][0], pts[0][1], off, sc, textured, rgb, alpha, mx, my, mw, mh);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private void vert(BufferBuilder b, Matrix4f m, Vec3d cam, float px, float py, double off, float sc,
                      boolean textured, int rgb, int alpha, float mx, float my, float mw, float mh) {
        double u = (px / UW - 0.5) * W * sc, v = (0.5 - py / UH) * H * sc;
        Vec3d p = center.add(right.multiply(u)).add(0, v, 0).add(normal.multiply(off));
        float rx = (float) (p.x - cam.x), ry = (float) (p.y - cam.y), rz = (float) (p.z - cam.z);
        if (textured) b.vertex(m, rx, ry, rz).texture((px - mx) / mw, (py - my) / mh).color(255, 255, 255, alpha);
        else b.vertex(m, rx, ry, rz).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha);
    }
}
