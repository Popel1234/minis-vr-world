package dev.miko.vrscreen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

/** Smooth textures from Java2D images (mipmapped + bilinear) and small draw helpers. */
public final class Gfx {
    private Gfx() {}

    public static final class Tex {
        public final int id;
        public final int w, h;

        private Tex(int w, int h) {
            this.w = w; this.h = h;
            this.id = GlStateManager._genTexture();
        }

        public static Tex create(BufferedImage img) {
            Tex t = new Tex(img.getWidth(), img.getHeight());
            GlStateManager._bindTexture(t.id);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            t.update(img);
            return t;
        }

        public void update(BufferedImage img) {
            int[] px = img.getRGB(0, 0, w, h, null, 0, w);
            IntBuffer buf = MemoryUtil.memAllocInt(px.length);
            buf.put(px).flip();
            GlStateManager._bindTexture(id);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                    GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buf);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            MemoryUtil.memFree(buf);
        }

        public void delete() { GlStateManager._deleteTexture(id); }
    }

    /** Draws a texture centered at (cx,cy) in GUI space, optionally rotated/mirrored. */
    public static void blit(DrawContext ctx, Tex t, float cx, float cy, float w, float h,
                            float rotDeg, float alpha, boolean mirror) {
        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(cx, cy, 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotDeg));
        Matrix4f m = ms.peek().getPositionMatrix();
        float u0 = mirror ? 1f : 0f, u1 = mirror ? 0f : 1f;
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, t.id);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(m, -w / 2, h / 2, 0).texture(u0, 1f).color(255, 255, 255, a);
        b.vertex(m, w / 2, h / 2, 0).texture(u1, 1f).color(255, 255, 255, a);
        b.vertex(m, w / 2, -h / 2, 0).texture(u1, 0f).color(255, 255, 255, a);
        b.vertex(m, -w / 2, -h / 2, 0).texture(u0, 0f).color(255, 255, 255, a);
        BufferRenderer.drawWithGlobalProgram(b.end());
        ms.pop();
    }

    public static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
    public static float ease(float t) { t = clamp(t); return 1f - (1f - t) * (1f - t) * (1f - t); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Rounded-rectangle outline as polygon points (clockwise, pixel space). */
    public static float[][] roundedPoly(float x, float y, float w, float h, float r, int seg) {
        r = Math.min(r, Math.min(w, h) / 2f);
        float[][] p = new float[(seg + 1) * 4][2];
        float[][] c = {{x + r, y + r}, {x + w - r, y + r}, {x + w - r, y + h - r}, {x + r, y + h - r}};
        double[] start = {Math.PI, Math.PI * 1.5, 0, Math.PI * 0.5};
        int k = 0;
        for (int i = 0; i < 4; i++) {
            for (int s = 0; s <= seg; s++) {
                double a = start[i] + (Math.PI / 2) * s / seg;
                p[k][0] = (float) (c[i][0] + r * Math.cos(a));
                p[k][1] = (float) (c[i][1] + r * Math.sin(a));
                k++;
            }
        }
        return p;
    }
}
