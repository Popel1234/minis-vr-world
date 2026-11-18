package dev.miko.vrscreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;

/** First-person animations: you hold a VR headset / AirPods in your hands and put them on. */
final class Intro {
    private final Panel.Type type;
    private final Vec3d eye;
    private final float yaw;
    private final long start = Util.getMeasuringTimeMs();
    private boolean spawned, chimed;

    Intro(Panel.Type type, Vec3d eye, float yaw) { this.type = type; this.eye = eye; this.yaw = yaw; }

    /** @return true when finished */
    boolean render(DrawContext ctx) {
        float dur = type == Panel.Type.VR ? 2900f : 3200f;
        float p = Math.min(1f, (Util.getMeasuringTimeMs() - start) / dur);
        int W = ctx.getScaledWindowWidth(), H = ctx.getScaledWindowHeight();
        if (type == Panel.Type.VR) vr(ctx, p, W, H); else pods(ctx, p, W, H);
        return p >= 1f;
    }

    private void sound(float pitch) {
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), pitch));
    }

    private void spawn() {
        if (!spawned) { spawned = true; VRScreenClient.spawn(type, eye, yaw); }
    }

    private void vr(DrawContext ctx, float p, int W, int H) {
        float rise = Gfx.ease(p / 0.25f);
        float zoom = Gfx.ease((p - 0.45f) / 0.35f);
        if (p < 0.8f) {
            float h = H * (1.0f + zoom * 3.2f), w = h * 1200f / 900f;
            float bob = (float) Math.sin(p * 18) * (1 - zoom) * H * 0.004f;
            float cy = Gfx.lerp(H * 1.45f, H * 0.60f, rise) - zoom * H * 0.25f + bob;
            float rot = (float) Math.sin(p * 9) * 1.2f * (1 - zoom) - zoom * 3f;
            Gfx.blit(ctx, Sprites.headset(), W / 2f, cy, w, h, rot, 1f, false);
        }
        if (p > 0.45f && !chimed) { chimed = true; sound(0.7f); }
        float a = p < 0.8f ? Gfx.clamp((p - 0.5f) / 0.3f) : 1f - Gfx.clamp((p - 0.82f) / 0.18f);
        ctx.fill(0, 0, W, H, ((int) (a * 255)) << 24);
        if (p >= 0.8f) {
            spawn();
            Gfx.blit(ctx, Sprites.vignette(), W / 2f, H / 2f, W, H, 0f, a * 0.9f, false);
        }
    }

    private void pods(DrawContext ctx, float p, int W, int H) {
        float t1 = Gfx.ease(p / 0.3f), t2 = Gfx.ease((p - 0.38f) / 0.34f);
        float ph = H * 0.78f, pw = ph * 0.5f;
        for (int side = -1; side <= 1; side += 2) {
            float x = Gfx.lerp(W / 2f + side * W * 0.13f, W / 2f + side * W * 0.62f, t2);
            float y = Gfx.lerp(H * 1.35f, H * 0.62f, t1) - t2 * H * 0.20f;
            float s = 1f - 0.2f * t2;
            Gfx.blit(ctx, Sprites.pod(), x, y, pw * s, ph * s, side * 35f * t2, 1f, side < 0);
        }
        float dim = 0.35f * (float) Math.sin(Gfx.clamp((p - 0.4f) / 0.6f) * Math.PI);
        ctx.fill(0, 0, W, H, ((int) (dim * 255)) << 24);
        if (p >= 0.72f && !chimed) { chimed = true; sound(1.6f); }
        if (p >= 0.78f) spawn();
        float in = Gfx.ease((p - 0.72f) / 0.12f), out = Gfx.ease((p - 0.93f) / 0.07f);
        float y = Gfx.lerp(-H * 0.12f, H * 0.09f, in) - out * H * 0.2f;
        float pwid = W * 0.34f;
        Gfx.blit(ctx, Sprites.pill(), W / 2f, y, pwid, pwid * 170f / 760f, 0f, 1f, false);
    }
}
