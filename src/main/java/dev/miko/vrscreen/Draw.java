package dev.miko.vrscreen;

import net.minecraft.client.gui.DrawContext;

/** Small helpers: filled rounded rectangles and procedural app logos (no copyrighted assets bundled). */
public final class Draw {
    private Draw() {}

    public static void roundRect(DrawContext c, int x, int y, int w, int h, int r, int argb) {
        r = Math.min(r, Math.min(w, h) / 2);
        c.fill(x + r, y, x + w - r, y + h, argb);
        for (int i = 0; i < r; i++) {
            int inset = r - (int) Math.round(Math.sqrt(r * r - (double) (r - i - 0.5) * (r - i - 0.5)));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, argb);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, argb);
        }
        c.fill(x, y + r, x + r, y + h - r, argb);
        c.fill(x + w - r, y + r, x + w, y + h - r, argb);
    }

    /** Simple play-triangle. */
    public static void play(DrawContext c, int cx, int cy, int size, int argb) {
        for (int i = 0; i < size; i++) {
            int h = (int) (size * (1.0 - (double) i / size));
            c.fill(cx - size / 3 + i, cy - h / 2, cx - size / 3 + i + 1, cy + h / 2, argb);
        }
    }
}
