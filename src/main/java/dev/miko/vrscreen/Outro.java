package dev.miko.vrscreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;

/** Reverse of the intro: you take the headset / AirPods off and put them away. */
final class Outro {
    private final Panel.Type type;
    private final long start = Util.getMeasuringTimeMs();
    private boolean closed, chimed;

    Outro(Panel.Type type) { this.type = type; }

    /** @return true when finished */
    boolean render(DrawContext ctx) {
        float dur = type == Panel.Type.VR ? 2300f : 2500f;
        float p = Math.min(1f, (Util.getMeasuringTimeMs() - start) / dur);
        int W = ctx.getScaledWindowWidth(), H = ctx.getScaledWindowHeight();
        if (type == Panel.Type.VR) vr(ctx, p, W, H); else pods(ctx, p, W, H);
        return p >= 1f;
    }

    private void closeNow() {
        if (!closed) { closed = true; VRScreenClient.closePanel(); }
    }

    private void sound(float pitch) {
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), pitch));
    }

    private void vr(DrawContext ctx, float p, int W, int H) {
        float a = p < 0.2f ? p / 0.2f : 1f - Gfx.clamp((p - 0.2f) / 0.3f);
        if (p >= 0.2f) {
            closeNow();
            float zoom = 1f - Gfx.ease((p - 0.25f) / 0.45f);
            float away = Gfx.ease((p - 0.65f) / 0.3f);
            float h = H * (1f + zoom * 3.2f), w = h * 1200f / 900f;
            float cy = Gfx.lerp(H * 0.60f, H * 1.45f, away) - zoom * H * 0.25f;
            float rot = (float) Math.sin(p * 9) * 1.2f * (1 - zoom) - zoom * 3f;
            Gfx.blit(ctx, Sprites.headset(), W / 2f, cy, w, h, rot, 1f, false);
        }
        if (p >= 0.25f && !chimed) { chimed = true; sound(0.6f); }
        ctx.fill(0, 0, W, H, ((int) (Gfx.clamp(a) * 255)) << 24);
        Gfx.blit(ctx, Sprites.vignette(), W / 2f, H / 2f, W, H, 0f, Gfx.clamp(a) * 0.9f, false);
    }

    private void pods(DrawContext ctx, float p, int W, int H) {
        if (p >= 0.1f) closeNow();
        float t2 = 1f - Gfx.ease((p - 0.1f) / 0.4f);
        float t1 = 1f - Gfx.ease((p - 0.55f) / 0.35f);
        if (p >= 0.1f) {
            float ph = H * 0.78f, pw = ph * 0.5f;
            for (int side = -1; side <= 1; side += 2) {
                float x = Gfx.lerp(W / 2f + side * W * 0.13f, W / 2f + side * W * 0.62f, t2);
                float y = Gfx.lerp(H * 1.35f, H * 0.62f, t1) - t2 * H * 0.20f;
                float s = 1f - 0.2f * t2;
                Gfx.blit(ctx, Sprites.pod(), x, y, pw * s, ph * s, side * 35f * t2, 1f, side < 0);
            }
        }
        float dim = 0.35f * (float) Math.sin(Gfx.clamp(p / 0.6f) * Math.PI);
        ctx.fill(0, 0, W, H, ((int) (dim * 255)) << 24);
        if (p >= 0.08f && !chimed) { chimed = true; sound(1.0f); }
        float in = Gfx.ease(p / 0.12f), out = Gfx.ease((p - 0.38f) / 0.1f);
        float y = Gfx.lerp(-H * 0.12f, H * 0.09f, in) - out * H * 0.2f;
        float pwid = W * 0.34f;
        Gfx.blit(ctx, Sprites.pillOff(), W / 2f, y, pwid, pwid * 170f / 760f, 0f, 1f, false);
    }
}
