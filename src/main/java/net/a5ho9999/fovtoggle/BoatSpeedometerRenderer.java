package net.a5ho9999.fovtoggle;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BoatSpeedometerRenderer {
    private static float currentSpeed = 0f;
    private static float displayedSpeed = 0f;
    private static float maxSpeedRecorded = 0f;
    private static int redlineFlashTimer = 0;
    private static int raveTimer = 0;
    private static boolean showSpeedometer = false;

    // Race car theme colors
    private static final int BACKGROUND_COLOR = 0x80000000; // Semi-transparent black
    private static final int NEEDLE_COLOR = 0xFFFF0000; // Bright red
    private static final int TEXT_COLOR = 0xFFFFFFFF; // White
    private static final int SCALE_COLOR = 0xFFC0C0C0; // Light gray
    private static final int REDLINE_COLOR = 0xFFFF4444; // Bright red for danger zone
    private static final int GREEN_ZONE_COLOR = 0xFF44FF44; // Green for safe zone
    private static final int BORDER_COLOR = 0xFF808080; // Gray border

    // Speedometer parameters
    private static final float MAX_DISPLAY_SPEED = 2.0f; // Maximum speed to display (blocks/tick)
    private static final float REDLINE_THRESHOLD = 1.5f; // Speed at which redline starts
    private static final int SPEEDOMETER_SIZE = 80; // Diameter of speedometer
    private static final float SPEED_SMOOTHING = 0.1f; // How quickly the needle moves

    public static void register() {
        HudRenderCallback.EVENT.register(BoatSpeedometerRenderer::renderSpeedometer);
    }

    private static void drawRaveEffect(DrawContext drawContext, int centerX, int centerY, int radius) {
        // Rainbow rave effect for extreme speeds (above 40 B/s)
        int[] rainbowColors = {
                0xFFFF0000, // Red
                0xFFFF8000, // Orange
                0xFFFFFF00, // Yellow
                0xFF80FF00, // Lime
                0xFF00FF00, // Green
                0xFF00FF80, // Spring green
                0xFF00FFFF, // Cyan
                0xFF0080FF, // Sky blue
                0xFF0000FF, // Blue
                0xFF8000FF, // Purple
                0xFFFF00FF, // Magenta
                0xFFFF0080  // Rose
        };

        // Fast cycling through rainbow colors
        int colorIndex = (raveTimer / 3) % rainbowColors.length;
        int currentColor = rainbowColors[colorIndex];

        // Multiple flashing borders for rave effect
        if (raveTimer % 8 < 4) {
            // Outer border
            drawContext.fill(centerX - radius - 3, centerY - radius - 3, centerX + radius + 3, centerY - radius - 2, currentColor);
            drawContext.fill(centerX - radius - 3, centerY + radius + 2, centerX + radius + 3, centerY + radius + 3, currentColor);
            drawContext.fill(centerX - radius - 3, centerY - radius - 2, centerX - radius - 2, centerY + radius + 2, currentColor);
            drawContext.fill(centerX + radius + 2, centerY - radius - 2, centerX + radius + 3, centerY + radius + 2, currentColor);

            // Inner border with different color
            int innerColorIndex = (colorIndex + 6) % rainbowColors.length;
            int innerColor = rainbowColors[innerColorIndex];
            drawContext.fill(centerX - radius - 1, centerY - radius - 1, centerX + radius + 1, centerY - radius, innerColor);
            drawContext.fill(centerX - radius - 1, centerY + radius, centerX + radius + 1, centerY + radius + 1, innerColor);
            drawContext.fill(centerX - radius - 1, centerY - radius, centerX - radius, centerY + radius, innerColor);
            drawContext.fill(centerX + radius, centerY - radius, centerX + radius + 1, centerY + radius, innerColor);
        }

        // Additional sparkle effects around the speedometer
        if (raveTimer % 6 < 3) {
            int sparkleColor = rainbowColors[(colorIndex + 3) % rainbowColors.length];

            // Sparkles at cardinal directions
            drawContext.fill(centerX - 1, centerY - radius - 6, centerX + 1, centerY - radius - 4, sparkleColor);
            drawContext.fill(centerX - 1, centerY + radius + 4, centerX + 1, centerY + radius + 6, sparkleColor);
            drawContext.fill(centerX - radius - 6, centerY - 1, centerX - radius - 4, centerY + 1, sparkleColor);
            drawContext.fill(centerX + radius + 4, centerY - 1, centerX + radius + 6, centerY + 1, sparkleColor);
        }
    }

    private static void renderSpeedometer(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null || client.options.hudHidden || !FOVToggleMod.getZoomies()) {
            showSpeedometer = false;
            return;
        }

        // Only show when in a boat
        if (!(client.player.getVehicle() instanceof BoatEntity boat)) {
            showSpeedometer = false;
            return;
        }

        showSpeedometer = true;

        // Calculate speed in blocks per tick
        Vec3d velocity = boat.getVelocity();
        currentSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // Convert to blocks per second (20 ticks per second)
        float blocksPerSecond = currentSpeed * 20f;

        // Smooth the displayed speed
        displayedSpeed = MathHelper.lerp(SPEED_SMOOTHING, displayedSpeed, blocksPerSecond);

        // Update max speed
        if (blocksPerSecond > maxSpeedRecorded) {
            maxSpeedRecorded = blocksPerSecond;
        }

        // Update redline flash timer and rave timer
        if (blocksPerSecond > 40f) {
            raveTimer++;
            redlineFlashTimer = 0; // Stop redline when in rave mode
        } else if (blocksPerSecond > REDLINE_THRESHOLD * 20f) {
            redlineFlashTimer++;
            raveTimer = 0; // Stop rave when in redline mode
        } else {
            redlineFlashTimer = 0;
            raveTimer = 0;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Position above hotbar (hotbar is typically at bottom with height ~22)
        int centerX = screenWidth / 2;
        int centerY = screenHeight - 70 - SPEEDOMETER_SIZE / 2; // Above hotbar

        renderSpeedometerGauge(drawContext, centerX, centerY, displayedSpeed);
    }

    private static void renderSpeedometerGauge(DrawContext drawContext, int centerX, int centerY, float speed) {
        int radius = SPEEDOMETER_SIZE / 2;

        // Draw background using simple rectangles for better performance
        drawContext.fill(centerX - radius, centerY - radius, centerX + radius, centerY + radius, BACKGROUND_COLOR);

        // Draw border using simple rectangles
        drawContext.fill(centerX - radius - 1, centerY - radius - 1, centerX + radius + 1, centerY - radius, BORDER_COLOR); // Top
        drawContext.fill(centerX - radius - 1, centerY + radius, centerX + radius + 1, centerY + radius + 1, BORDER_COLOR); // Bottom
        drawContext.fill(centerX - radius - 1, centerY - radius, centerX - radius, centerY + radius, BORDER_COLOR); // Left
        drawContext.fill(centerX + radius, centerY - radius, centerX + radius + 1, centerY + radius, BORDER_COLOR); // Right

        // Draw simplified scale markings
        drawScaleMarkings(drawContext, centerX, centerY, radius);

        // Draw needle as simple lines
        drawNeedle(drawContext, centerX, centerY, radius - 8, speed);

        // Draw center dot as small rectangle
        drawContext.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, NEEDLE_COLOR);

        // Draw speed text inside the gauge
        renderSpeedText(drawContext, centerX, centerY, speed);

        // Draw warning effects
        if (speed > 40f) {
            // RAVE MODE for speeds above 40 B/s
            drawRaveEffect(drawContext, centerX, centerY, radius);
        } else if (speed > REDLINE_THRESHOLD * 20f) {
            // Regular redline flash effect for speeds between 30-40 B/s
            if (redlineFlashTimer % 20 < 10) {
                drawContext.fill(centerX - radius - 2, centerY - radius - 2, centerX + radius + 2, centerY - radius - 1, REDLINE_COLOR);
                drawContext.fill(centerX - radius - 2, centerY + radius + 1, centerX + radius + 2, centerY + radius + 2, REDLINE_COLOR);
                drawContext.fill(centerX - radius - 2, centerY - radius - 1, centerX - radius - 1, centerY + radius + 1, REDLINE_COLOR);
                drawContext.fill(centerX + radius + 1, centerY - radius - 1, centerX + radius + 2, centerY + radius + 1, REDLINE_COLOR);
            }
        }
    }



    private static void drawScaleMarkings(DrawContext drawContext, int centerX, int centerY, int radius) {
        // Draw simplified scale markings using basic math
        int numMajorTicks = 9; // 0, 5, 10, 15, 20, 25, 30, 35, 40 B/s

        for (int i = 0; i < numMajorTicks; i++) {
            float speedValue = i * 5f; // Every 5 B/s
            float speedRatio = speedValue / (MAX_DISPLAY_SPEED * 20f);
            if (speedRatio > 1f) break;

            // Simple angle calculation for semicircle (180 degrees)
            float angle = (float) (Math.PI + speedRatio * Math.PI);

            int tickLength = (i % 2 == 0) ? 8 : 4; // Major vs minor ticks
            int outerRadius = radius - 3;
            int innerRadius = outerRadius - tickLength;

            int x1 = centerX + (int) (Math.cos(angle) * innerRadius);
            int y1 = centerY + (int) (Math.sin(angle) * innerRadius);
            int x2 = centerX + (int) (Math.cos(angle) * outerRadius);
            int y2 = centerY + (int) (Math.sin(angle) * outerRadius);

            // Draw tick as simple line using fill
            drawSimpleLine(drawContext, x1, y1, x2, y2, SCALE_COLOR);

            // Draw numbers for major ticks (every 10 B/s)
            if (i % 2 == 0) {
                int textRadius = radius - 15;
                int textX = centerX + (int) (Math.cos(angle) * textRadius);
                int textY = centerY + (int) (Math.sin(angle) * textRadius);

                String speedText = String.valueOf((int)speedValue);
                int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(speedText);

                drawContext.drawText(MinecraftClient.getInstance().textRenderer,
                        speedText, textX - textWidth / 2, textY - 4, SCALE_COLOR, true);
            }
        }
    }

    private static void drawNeedle(DrawContext drawContext, int centerX, int centerY, int length, float speed) {
        float maxSpeed = MAX_DISPLAY_SPEED * 20f; // Convert to blocks/second
        float speedRatio = MathHelper.clamp(speed / maxSpeed, 0f, 1f);

        // Needle sweeps 180 degrees (semicircle)
        float angle = (float) (Math.PI + speedRatio * Math.PI);

        int endX = centerX + (int) (Math.cos(angle) * length);
        int endY = centerY + (int) (Math.sin(angle) * length);

        // Draw needle as thick simple line
        drawThickLine(drawContext, centerX, centerY, endX, endY, NEEDLE_COLOR, 2);

        // Draw needle tip as small rectangle
        drawContext.fill(endX - 1, endY - 1, endX + 1, endY + 1, NEEDLE_COLOR);
    }

    private static void renderSpeedText(DrawContext drawContext, int centerX, int centerY, float speed) {
        String speedText = String.format("%.1f", speed);
        String maxSpeedText = String.format("%.1f", maxSpeedRecorded);

        int speedTextWidth = MinecraftClient.getInstance().textRenderer.getWidth(speedText);
        int maxSpeedTextWidth = MinecraftClient.getInstance().textRenderer.getWidth(maxSpeedText);

        // Draw current speed in the upper part of the gauge
        drawContext.drawText(MinecraftClient.getInstance().textRenderer,
                speedText, centerX - speedTextWidth / 2, centerY - 8, TEXT_COLOR, true);

        // Draw "B/s" label smaller below current speed
        String label = "B/s";
        int labelWidth = MinecraftClient.getInstance().textRenderer.getWidth(label);
        drawContext.drawText(MinecraftClient.getInstance().textRenderer,
                label, centerX - labelWidth / 2, centerY + 2, SCALE_COLOR, true);

        // Draw max speed in the lower part (smaller)
        drawContext.drawText(MinecraftClient.getInstance().textRenderer,
                maxSpeedText, centerX - maxSpeedTextWidth / 2, centerY + 12, SCALE_COLOR, true);
    }

    // Optimized line drawing methods
    private static void drawSimpleLine(DrawContext drawContext, int x1, int y1, int x2, int y2, int color) {
        // Use Bresenham's line algorithm but optimized for simple cases
        if (Math.abs(x2 - x1) <= 1 && Math.abs(y2 - y1) <= 1) {
            // Very short line, just draw a pixel
            drawContext.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        if (dx > dy) {
            // More horizontal than vertical
            if (x1 > x2) {
                int temp = x1; x1 = x2; x2 = temp;
                temp = y1; y1 = y2; y2 = temp;
            }
            int steps = x2 - x1;
            for (int i = 0; i <= steps; i++) {
                int x = x1 + i;
                int y = y1 + (i * (y2 - y1)) / steps;
                drawContext.fill(x, y, x + 1, y + 1, color);
            }
        } else {
            // More vertical than horizontal
            if (y1 > y2) {
                int temp = x1; x1 = x2; x2 = temp;
                temp = y1; y1 = y2; y2 = temp;
            }
            int steps = y2 - y1;
            for (int i = 0; i <= steps; i++) {
                int y = y1 + i;
                int x = x1 + (i * (x2 - x1)) / steps;
                drawContext.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    private static void drawThickLine(DrawContext drawContext, int x1, int y1, int x2, int y2, int color, int thickness) {
        // Draw multiple thin lines for thickness
        for (int i = -thickness/2; i <= thickness/2; i++) {
            for (int j = -thickness/2; j <= thickness/2; j++) {
                drawSimpleLine(drawContext, x1 + i, y1 + j, x2 + i, y2 + j, color);
            }
        }
    }

    public static void resetMaxSpeed() {
        maxSpeedRecorded = 0f;
    }

    public static boolean isSpeedometerVisible() {
        return showSpeedometer;
    }
}
