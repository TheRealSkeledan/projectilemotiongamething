import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class JustACircle {
    public static final int fps = 120;
    public static final double delta_t = 1.0 / fps;
    public static final double g = -980.66;
    public static final int width = 640;
    public static final int height = 480;

    public static Circle player = new Circle(320, 40, 12);
    public static Circle entity = new Circle(-40, 40, 20);

    public static BufferedImage[] entityFrames;
    public static int entityFrameIndex = 0;
    public static double entityAnimTimer = 0;

    private static void loadEntitySprites(int frameW, int frameH) {
        try {
            BufferedImage spriteSheet = ImageIO.read(Objects.requireNonNull(JustACircle.class.getResource("/entityMoving.png")));
            if (spriteSheet == null) {
                System.out.println("Entity spritesheet not found: " + "/entityMoving.png");
                return;
            }

            entityFrames = new BufferedImage[6 * 4];
            int idx = 0;
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 6; x++) {
                    entityFrames[idx++] = spriteSheet.getSubimage(x, y, frameW, frameH);
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to load entity sprites: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        StdDraw.setCanvasSize(width, height);
        StdDraw.setXscale(0, width);
        StdDraw.setYscale(0, height);
        StdDraw.enableDoubleBuffering();

        playIntroAnimation();
        loadEntitySprites(128, 128);

        double angle = 45;
        double power = 400;
        boolean launched = false;

        double vx = 0;
        double vy = 0;

        while (true) {
            drawBackground();

            // --- aiming mode ---
            if (!launched) {
                if (StdDraw.isKeyPressed(KeyEvent.VK_UP)) angle += 0.5;
                if (StdDraw.isKeyPressed(KeyEvent.VK_DOWN)) angle -= 0.5;
                if (StdDraw.isKeyPressed(KeyEvent.VK_LEFT)) power -= 2;
                if (StdDraw.isKeyPressed(KeyEvent.VK_RIGHT)) power += 2;
                power = Math.max(100, Math.min(700, power));

                // Draw trajectory dots
                StdDraw.setPenColor(Color.WHITE);
                for (int i = 1; i <= 30; i++) {
                    double t = i * 0.1;
                    double px = player.getX() + power * Math.cos(Math.toRadians(angle)) * t * 0.05;
                    double py = player.getY() + power * Math.sin(Math.toRadians(angle)) * t * 0.05 + 0.5 * g * Math.pow(t * 0.05, 2);
                    if (py < 0) break;
                    StdDraw.filledCircle(px, py, 2);
                }

                // Draw aiming line
                double arrowX = player.getX() + 70 * Math.cos(Math.toRadians(angle));
                double arrowY = player.getY() + 70 * Math.sin(Math.toRadians(angle));
                StdDraw.setPenColor(Color.RED);
                StdDraw.line(player.getX(), player.getY(), arrowX, arrowY);

                StdDraw.setPenColor(Color.WHITE);
                StdDraw.text(120, 460, "Angle: " + Math.round(angle) + "°");
                StdDraw.text(120, 440, "Power: " + Math.round(power));
                StdDraw.text(120, 420, "Press SPACE to launch");

                if (StdDraw.isKeyPressed(KeyEvent.VK_SPACE)) {
                    launched = true;
                    vx = power * Math.cos(Math.toRadians(angle));
                    vy = power * Math.sin(Math.toRadians(angle));
                }
            } else {
                // --- flight mode ---
                vy += g * delta_t;
                player.move(vx * delta_t, vy * delta_t);

                if (player.getY() - player.getRadius() <= 0) {
                    player.move(0, -(player.getY() - player.getRadius()));
                    launched = false;
                    vx = vy = 0;
                    entity = new Circle(-40, 40, 20);
                }

                entity.move(1.8, 0);

                double dx = player.getX() - entity.getX();
                double dy = player.getY() - entity.getY();
                if (Math.sqrt(dx * dx + dy * dy) <= player.getRadius() + entity.getRadius()) {
                    deathScreen();
                    return;
                }
            }

            if (entityFrames != null) {
                entityAnimTimer += delta_t;
                if (entityAnimTimer >= 0.1) { // switch frame every ~0.1s
                    entityFrameIndex = (entityFrameIndex + 1) % entityFrames.length;
                    entityAnimTimer = 0;
                }
                BufferedImage currentFrame = entityFrames[entityFrameIndex];
                double scale = 1.5; // make it bigger if needed
                StdDraw.picture(entity.getX(), entity.getY(),
                        tempImagePath(currentFrame),
                        currentFrame.getWidth() * scale,
                        currentFrame.getHeight() * scale);
            } else {
                entity.draw();
            }

            player.draw();

            StdDraw.show();
            StdDraw.pause(1000 / fps);
        }
    }

    private static String tempImagePath(BufferedImage img) {
        try {
            File temp = File.createTempFile("entityFrame_", ".png");
            ImageIO.write(img, "png", temp);
            temp.deleteOnExit();
            return temp.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private static void drawBackground() {
        StdDraw.setPenColor(30, 30, 40);
        StdDraw.filledRectangle(width / 2.0, height / 2.0, width / 2.0, height / 2.0);
        StdDraw.setPenColor(60, 40, 25);
        StdDraw.filledRectangle(width / 2.0, 20, width / 2.0, 20);
    }

    private static void deathScreen() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.RED);
        StdDraw.text(width / 2.0, height / 2.0, "YOU DIED");
        StdDraw.show();
        StdDraw.pause(2000);
    }

    private static void playIntroAnimation() {
        try {
            BufferedImage spriteSheet = ImageIO.read(Objects.requireNonNull(JustACircle.class.getResource("/entityWakingUp.png")));
            if (spriteSheet == null) {
                System.out.println("Spritesheet not found: " + "/entityWakingUp.png");
                return;
            }

            BufferedImage[] frames = new BufferedImage[6 * 7];
            int idx = 0;
            for (int y = 0; y < 7; y++) {
                for (int x = 0; x < 6; x++) {
                    frames[idx++] = spriteSheet.getSubimage(x * 128, y * 128, 128, 128);
                }
            }

            // scale factor — change this to make it bigger
            double scale = 3.0;  // 3x the original size

            for (BufferedImage frame : frames) {
                // save frame to temp file
                File temp = File.createTempFile("frame_", ".png");
                ImageIO.write(frame, "png", temp);

                StdDraw.clear(new Color(10, 10, 15));
                StdDraw.picture(
                        width / 2.0,
                        height / 2.0,
                        temp.getAbsolutePath(),
                        128 * scale,
                        128 * scale
                );
                StdDraw.show();
                StdDraw.pause(100);
                temp.deleteOnExit();
            }

        } catch (Exception e) {
            System.out.println("Failed to load intro: " + e);
        }
    }
}