package dev.miko.vrscreen;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/** Vector artwork drawn once with Java2D (anti-aliased), then uploaded as smooth textures. */
public final class Sprites {
    private Sprites() {}
    private static Gfx.Tex headset, pod, pill, pillOff, vignette;

    public static Graphics2D g2(BufferedImage im) {
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    private static final Color SKIN = new Color(0xF0BE9A), SKIN_D = new Color(0xD9946F), SLEEVE = new Color(0x2E3247);

    private static void line(Graphics2D g, float w, Color c, float x1, float y1, float x2, float y2) {
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(c);
        g.draw(new Line2D.Float(x1, y1, x2, y2));
    }

    // ---------------- VR headset in two hands (view from the wearer: lenses facing you) ----------------
    public static Gfx.Tex headset() {
        if (headset != null) return headset;
        BufferedImage im = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g2(im);
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new RoundRectangle2D.Float(236, 232, 760, 360, 150, 150));
        g.setPaint(new GradientPaint(0, 190, new Color(0xFAFAFC), 0, 550, new Color(0xC9CDD9)));
        g.fill(new RoundRectangle2D.Float(220, 190, 760, 360, 150, 150));
        g.setColor(new Color(0x15161B));
        g.fill(new RoundRectangle2D.Float(262, 230, 676, 280, 120, 120));
        g.setStroke(new BasicStroke(6));
        g.setColor(new Color(0x2A2C35));
        g.draw(new RoundRectangle2D.Float(270, 238, 660, 264, 112, 112));
        g.setComposite(AlphaComposite.Clear);
        g.fill(new Ellipse2D.Float(505, 470, 190, 130));
        g.setComposite(AlphaComposite.SrcOver);
        lens(g, 430, 365, 125);
        lens(g, 770, 365, 125);
        hand(g);
        g.translate(1200, 0);
        g.scale(-1, 1);
        hand(g);
        g.dispose();
        return headset = Gfx.Tex.create(im);
    }

    private static void lens(Graphics2D g, int cx, int cy, int r) {
        g.setColor(new Color(0x08080B));
        g.fill(new Ellipse2D.Float(cx - r - 14, cy - r - 14, 2 * r + 28, 2 * r + 28));
        g.setPaint(new RadialGradientPaint(cx - 20f, cy - 25f, r, new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(0x9FB0FF), new Color(0x3A3F9A), new Color(0x0C0D22)}));
        g.fill(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(255, 255, 255, 26));
        for (int rr = 28; rr < r; rr += 17) g.draw(new Ellipse2D.Float(cx - rr, cy - rr, 2 * rr, 2 * rr));
        g.setColor(new Color(255, 255, 255, 70));
        g.fill(new Ellipse2D.Float(cx - r * 0.62f, cy - r * 0.7f, r * 0.55f, r * 0.32f));
    }

    private static void hand(Graphics2D g) {
        line(g, 190, SLEEVE, 110, 1000, 250, 640);
        g.setPaint(new GradientPaint(150, 420, SKIN, 320, 640, SKIN_D));
        g.fill(new Ellipse2D.Float(150, 400, 200, 270));
        for (int i = 0; i < 4; i++) {
            float y = 400 + i * 58;
            line(g, 46, SKIN, 300, y + 10, 352 + (i == 1 ? 18 : 0) - (i == 3 ? 14 : 0), y - 6);
        }
        line(g, 58, SKIN, 260, 440, 330, 330);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(0, 0, 0, 28));
        g.draw(new Arc2D.Float(200, 430, 90, 60, 20, 140, Arc2D.OPEN));
    }

    // ---------------- AirPod pinched between thumb and finger ----------------
    public static Gfx.Tex pod() {
        if (pod != null) return pod;
        BufferedImage im = new BufferedImage(500, 1000, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g2(im);
        line(g, 210, SLEEVE, 250, 1120, 250, 800);
        g.setPaint(new GradientPaint(120, 700, SKIN, 380, 980, SKIN_D));
        g.fill(new Ellipse2D.Float(120, 690, 260, 300));
        g.setPaint(new GradientPaint(222, 300, Color.WHITE, 278, 620, new Color(0xDADDE6)));
        g.fill(new RoundRectangle2D.Float(222, 300, 56, 320, 56, 56));
        g.setPaint(new GradientPaint(160, 150, Color.WHITE, 340, 350, new Color(0xD5D8E2)));
        g.fill(new Ellipse2D.Float(160, 140, 180, 200));
        g.setColor(new Color(0x2A2A30));
        g.fill(new Ellipse2D.Float(228, 262, 44, 44));
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(new Ellipse2D.Float(188, 160, 50, 34));
        line(g, 60, SKIN, 150, 720, 236, 560);
        line(g, 52, SKIN, 360, 700, 268, 590);
        g.dispose();
        return pod = Gfx.Tex.create(im);
    }

    // ---------------- "AirPods connected / disconnected" pill ----------------
    public static Gfx.Tex pill() {
        if (pill == null) pill = makePill("Connected", true);
        return pill;
    }

    public static Gfx.Tex pillOff() {
        if (pillOff == null) pillOff = makePill("Disconnected", false);
        return pillOff;
    }

    private static Gfx.Tex makePill(String sub, boolean ok) {
        BufferedImage im = new BufferedImage(760, 170, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g2(im);
        g.setColor(Color.BLACK);
        g.fill(new RoundRectangle2D.Float(0, 0, 760, 170, 170, 170));
        for (int i = 0; i < 2; i++) {
            float x = 70 + i * 54;
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(x + 12, 70, 14, 52, 14, 14));
            g.fill(new Ellipse2D.Float(x, 38, 38, 44));
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
        g.setColor(Color.WHITE);
        g.drawString("AirPods", 210, 80);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 30));
        g.setColor(new Color(0xA0A0A8));
        g.drawString(sub, 210, 125);
        g.setColor(ok ? new Color(0x30D158) : new Color(0x8E8E93));
        g.fill(new Ellipse2D.Float(630, 55, 60, 60));
        g.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.BLACK);
        if (ok) {
            g.draw(new Line2D.Float(647, 86, 657, 96));
            g.draw(new Line2D.Float(657, 96, 675, 74));
        } else {
            g.draw(new Line2D.Float(649, 74, 671, 96));
            g.draw(new Line2D.Float(671, 74, 649, 96));
        }
        g.dispose();
        return im == null ? null : Gfx.Tex.create(im);
    }

    public static Gfx.Tex vignette() {
        if (vignette != null) return vignette;
        BufferedImage im = new BufferedImage(512, 288, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g2(im);
        g.setPaint(new RadialGradientPaint(256f, 144f, 300f, new float[]{0.45f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 255)}));
        g.fillRect(0, 0, 512, 288);
        g.dispose();
        return vignette = Gfx.Tex.create(im);
    }

    // ---------------- app logos (own drawings, no official artwork) ----------------
    public static void logo(Graphics2D g, int which, float x, float y, float s) {
        Color bg = switch (which) {
            case 0 -> new Color(0xFF0033);
            case 1 -> new Color(0x9146FF);
            case 2 -> new Color(0x101010);
            case 4 -> Color.WHITE;
            default -> new Color(0x1DB954);
        };
        g.setColor(bg);
        if (which == 3) g.fill(new Ellipse2D.Float(x, y, s, s));
        else g.fill(new RoundRectangle2D.Float(x, y, s, s, s * 0.28f, s * 0.28f));
        switch (which) {
            case 0 -> {
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Float(x + s * .14f, y + s * .27f, s * .72f, s * .46f, s * .2f, s * .2f));
                Path2D t = new Path2D.Float();
                t.moveTo(x + s * .43f, y + s * .38f); t.lineTo(x + s * .43f, y + s * .62f); t.lineTo(x + s * .64f, y + s * .5f);
                t.closePath();
                g.setColor(new Color(0xFF0033));
                g.fill(t);
            }
            case 1 -> {
                Path2D b = new Path2D.Float();
                b.moveTo(x + s * .28f, y + s * .22f); b.lineTo(x + s * .74f, y + s * .22f); b.lineTo(x + s * .74f, y + s * .58f);
                b.lineTo(x + s * .6f, y + s * .72f); b.lineTo(x + s * .5f, y + s * .72f); b.lineTo(x + s * .4f, y + s * .82f);
                b.lineTo(x + s * .4f, y + s * .72f); b.lineTo(x + s * .28f, y + s * .72f); b.closePath();
                g.setColor(Color.WHITE);
                g.fill(b);
                g.setColor(new Color(0x9146FF));
                g.fill(new Rectangle2D.Float(x + s * .46f, y + s * .34f, s * .055f, s * .17f));
                g.fill(new Rectangle2D.Float(x + s * .60f, y + s * .34f, s * .055f, s * .17f));
            }
            case 2 -> {
                note(g, x - s * .022f, y - s * .018f, s, new Color(0x25F4EE));
                note(g, x + s * .022f, y + s * .018f, s, new Color(0xFE2C55));
                note(g, x, y, s, Color.WHITE);
            }
            case 4 -> {
                float cx = x + s * .5f, cy = y + s * .5f, r = s * .27f;
                g.setStroke(new BasicStroke(s * .17f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(0xEA4335));
                g.draw(new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, 45, 90, Arc2D.OPEN));
                g.setColor(new Color(0xFBBC05));
                g.draw(new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, 135, 85, Arc2D.OPEN));
                g.setColor(new Color(0x34A853));
                g.draw(new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, 220, 100, Arc2D.OPEN));
                g.setColor(new Color(0x4285F4));
                g.draw(new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, 320, 45, Arc2D.OPEN));
                g.fill(new Rectangle2D.Float(cx, cy - s * .085f, r + s * .085f, s * .17f));
            }
            default -> {
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(s * .075f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 3; i++) {
                    float yy = y + s * (.36f + .13f * i);
                    float inset = s * (.2f + .035f * i);
                    Path2D p = new Path2D.Float();
                    p.moveTo(x + inset, yy + s * .03f);
                    p.quadTo(x + s * .5f, yy - s * (.1f - .015f * i), x + s - inset, yy + s * .035f);
                    g.draw(p);
                }
            }
        }
    }

    private static void note(Graphics2D g, float x, float y, float s, Color c) {
        g.setColor(c);
        g.fill(new Rectangle2D.Float(x + s * .46f, y + s * .22f, s * .07f, s * .42f));
        g.fill(new Ellipse2D.Float(x + s * .31f, y + s * .56f, s * .23f, s * .2f));
        Path2D f = new Path2D.Float();
        f.moveTo(x + s * .53f, y + s * .22f);
        f.curveTo(x + s * .55f, y + s * .34f, x + s * .64f, y + s * .38f, x + s * .73f, y + s * .4f);
        f.lineTo(x + s * .73f, y + s * .31f);
        f.curveTo(x + s * .65f, y + s * .29f, x + s * .6f, y + s * .26f, x + s * .6f, y + s * .22f);
        f.closePath();
        g.fill(f);
    }
}
