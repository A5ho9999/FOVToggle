package net.a5ho9999.fovtoggle;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SpeedEffectRenderer {
    private static final List<SpeedLine> speedLines = new ArrayList<>();
    private static final Random random = new Random();
    private static float lastSpeed = 0f;
    private static int lineSpawnTimer = 0;

    private static final float MIN_SPEED_THRESHOLD = 0.2f;
    private static final float MAX_SPEED_FOR_FULL_EFFECT = 1.5f;
    private static final int MAX_LINES = 35;
    private static final int LINE_SPAWN_INTERVAL = 6;

    public static void register() {
        HudRenderCallback.EVENT.register(SpeedEffectRenderer::renderSpeedEffect);
    }

    private static void renderSpeedEffect(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null || client.options.hudHidden || !FOVToggleMod.getZoomies()) {
            return;
        }

        if (!(client.player.getVehicle() instanceof BoatEntity boat)) {
            fadeOutLines(0.05f);
            return;
        }

        Vec3d velocity = boat.getVelocity();
        float currentSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        lastSpeed = MathHelper.lerp(0.05f, lastSpeed, currentSpeed);

        if (lastSpeed < MIN_SPEED_THRESHOLD) {
            fadeOutLines(0.03f);
            return;
        }

        float intensity = MathHelper.clamp(lastSpeed / MAX_SPEED_FOR_FULL_EFFECT, 0f, 1f);

        spawnSpeedLines(intensity);

        updateAndRenderLines(drawContext, intensity, tickCounter.getTickProgress(true));
    }

    private static void spawnSpeedLines(float intensity) {
        lineSpawnTimer++;

        if (lineSpawnTimer >= LINE_SPAWN_INTERVAL && speedLines.size() < MAX_LINES) {
            int linesToSpawn = 1 + (int)(intensity * 2);

            for (int i = 0; i < linesToSpawn && speedLines.size() < MAX_LINES; i++) {
                speedLines.add(new SpeedLine(intensity));
            }

            lineSpawnTimer = 0;
        }
    }

    private static void updateAndRenderLines(DrawContext drawContext, float intensity, float deltaTime) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        Iterator<SpeedLine> iterator = speedLines.iterator();

        while (iterator.hasNext()) {
            SpeedLine line = iterator.next();
            line.update(deltaTime, intensity);

            if (line.shouldRemove()) {
                iterator.remove();
                continue;
            }

            float distance = line.getDistance();
            float angle = line.getAngle();
            float length = line.getLength();

            float startDistance = Math.max(20f, distance - length);
            int startX = centerX + (int)(Math.cos(angle) * startDistance);
            int startY = centerY + (int)(Math.sin(angle) * startDistance);

            int endX = centerX + (int)(Math.cos(angle) * distance);
            int endY = centerY + (int)(Math.sin(angle) * distance);

            if (isCompletelyOutsideScreen(startX, startY, endX, endY, screenWidth, screenHeight)) {
                iterator.remove();
                continue;
            }

            float alpha = line.getAlpha() * intensity;
            int baseColor = line.getBaseColor();
            int color = (int)(alpha * 255) << 24 | baseColor;

            drawSpeedLineOptimized(drawContext, startX, startY, endX, endY, color, line.getThickness());
        }
    }

    private static void fadeOutLines(float fadeRate) {
        Iterator<SpeedLine> iterator = speedLines.iterator();
        while (iterator.hasNext()) {
            SpeedLine line = iterator.next();
            line.fadeOut(fadeRate);
            if (line.getAlpha() <= 0.02f) {
                iterator.remove();
            }
        }
    }

    private static boolean isCompletelyOutsideScreen(int x1, int y1, int x2, int y2, int width, int height) {
        return (x1 < -50 && x2 < -50) ||
                (x1 > width + 50 && x2 > width + 50) ||
                (y1 < -50 && y2 < -50) ||
                (y1 > height + 50 && y2 > height + 50);
    }

    private static void drawSpeedLineOptimized(DrawContext drawContext, int x1, int y1, int x2, int y2, int color, float thickness) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        float length = (float)Math.sqrt(dx * dx + dy * dy);

        if (length < 1) return;

        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;

        int thicknessInt = Math.max(1, Math.round(thickness));
        int halfThickness = thicknessInt / 2;

        if (thickness <= 1.5f) {
            if (Math.abs(dx) >= Math.abs(dy)) {
                int minX = Math.min(x1, x2);
                int maxX = Math.max(x1, x2);
                int y = (y1 + y2) / 2;
                drawContext.fill(minX, y - halfThickness, maxX + 1, y + halfThickness + 1, color);
            } else {
                int minY = Math.min(y1, y2);
                int maxY = Math.max(y1, y2);
                int x = (x1 + x2) / 2;
                drawContext.fill(x - halfThickness, minY, x + halfThickness + 1, maxY + 1, color);
            }
            return;
        }

        float stepX = (float)dx / steps;
        float stepY = (float)dy / steps;

        int chunkSize = Math.max(1, steps / 8);

        for (int i = 0; i <= steps; i += chunkSize) {
            int endChunk = Math.min(i + chunkSize, steps);

            int startX = Math.round(x1 + i * stepX);
            int startY = Math.round(y1 + i * stepY);
            int endX = Math.round(x1 + endChunk * stepX);
            int endY = Math.round(y1 + endChunk * stepY);

            if (Math.abs(endX - startX) >= Math.abs(endY - startY)) {
                int minX = Math.min(startX, endX);
                int maxX = Math.max(startX, endX);
                int centerY = (startY + endY) / 2;
                drawContext.fill(minX, centerY - halfThickness, maxX + thicknessInt, centerY + halfThickness + 1, color);
            } else {
                int minY = Math.min(startY, endY);
                int maxY = Math.max(startY, endY);
                int centerX = (startX + endX) / 2;
                drawContext.fill(centerX - halfThickness, minY, centerX + halfThickness + 1, maxY + thicknessInt, color);
            }
        }
    }

    private static class SpeedLine {
        private float distance;
        private float angle;
        private float speed;
        private float alpha;
        private float length;
        private float maxAlpha;
        private float age;
        private boolean fadingOut;
        private float thickness;
        private int baseColor;

        public SpeedLine(float intensity) {
            // Restored original spawn parameters
            this.distance = 40f + random.nextFloat() * 20f;
            this.angle = random.nextFloat() * 2f * (float) Math.PI;
            this.speed = 30f + random.nextFloat() * 40f * intensity; // Original speed
            this.maxAlpha = 0.5f + random.nextFloat() * 0.4f; // Original alpha
            this.alpha = 0f;
            this.length = 25f + random.nextFloat() * 40f * intensity; // Original length
            this.thickness = 2f + random.nextFloat() * 3f * intensity; // Original thickness
            this.age = 0f;
            this.fadingOut = false;

            // Original color selection with dark grey option restored
            float colorRoll = random.nextFloat();
            if (colorRoll < 0.4f) {
                this.baseColor = 0xFFFFFF; // White
            } else if (colorRoll < 0.7f) {
                this.baseColor = 0xC0C0C0; // Light grey
            } else if (colorRoll < 0.9f) {
                this.baseColor = 0x808080; // Dark grey
            } else {
                this.baseColor = 0x404040; // Dark grey (almost black but visible)
            }
        }

        public void update(float deltaTime, float intensity) {
            age += deltaTime;

            if (fadingOut) {
                return; // fadeOut method handles alpha reduction
            }

            // Move outward
            distance += speed * deltaTime;

            // Restored original fade timing
            if (age < 0.5f) {
                // Slower fade in over half a second
                alpha = MathHelper.lerp(alpha / maxAlpha, 1f, deltaTime * 4f) * maxAlpha;
            } else if (distance > 200f) {
                // Start fading out later and over a longer distance
                float fadeStart = 200f;
                float fadeEnd = 300f;
                float fadeFactor = 1f - MathHelper.clamp((distance - fadeStart) / (fadeEnd - fadeStart), 0f, 1f);
                alpha = maxAlpha * fadeFactor;
            } else {
                // Maintain full opacity for longer
                alpha = maxAlpha;
            }
        }

        public void fadeOut(float fadeRate) {
            fadingOut = true;
            alpha -= fadeRate;
            if (alpha < 0) alpha = 0;
        }

        public boolean shouldRemove() {
            return alpha <= 0f || distance > 280f; // Remove earlier
        }

        public float getDistance() {
            return distance;
        }

        public float getAngle() {
            return angle;
        }

        public float getAlpha() {
            return Math.max(0f, alpha);
        }

        public float getLength() {
            return length;
        }

        public float getThickness() {
            return thickness;
        }

        public int getBaseColor() {
            return baseColor;
        }
    }
}
