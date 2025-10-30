import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import javax.sound.sampled.*;

public class JustACircle {
    public static final int fps = 120;
    public static final double delta_t = 1.0 / fps;
    public static final double g = -980.66;
    public static final int width = 640;
    public static final int height = 480;

    public static Circle player = new Circle(320, 40, 15);

    public static Circle entity = new Circle(40, 120, 55);

    public static BufferedImage[] entityFrames;
    public static int entityFrameIndex = 0;
    public static double entityAnimTimer = 0;

    public static int score = 0;
    public static double enemySpeed = 1.8;
    public static double projectileRadius = 6;
    public static double projectileSpawnCooldown = 0.12;
    public static double timeSinceLastShot = projectileSpawnCooldown;

    public static ArrayList<Projectile> projectiles = new ArrayList<>();

    public static boolean lastSpace = false;

    private static String[] entityFramePaths;
    private static boolean entityFramesCached = false;

    public static double playerVy = 0;
    public static boolean onGround = true;

    private static BufferedImage backgroundImg;

    public static boolean enemyMovingRight = true;

    private static void loadEntitySprites(int frameW, int frameH) {
        try {
            BufferedImage spriteSheet = ImageIO.read(
                    Objects.requireNonNull(JustACircle.class.getResource("/entityMoving.png"))
            );

            if (spriteSheet == null) {
                System.out.println("Entity spritesheet not found: /entityMoving.png");
                return;
            }

            int sheetW = spriteSheet.getWidth();
            int sheetH = spriteSheet.getHeight();

            int cols = sheetW / frameW;
            int rows = sheetH / frameH;
            int totalFrames = cols * rows;

            entityFrames = new BufferedImage[totalFrames];
            entityFramePaths = new String[totalFrames];

            int idx = 0;
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    if (x * frameW + frameW <= sheetW && y * frameH + frameH <= sheetH) {
                        BufferedImage sub = spriteSheet.getSubimage(x * frameW, y * frameH, frameW, frameH);

                        // Cache to a temp file once
                        File temp = File.createTempFile("entityFrame_", ".png");
                        ImageIO.write(sub, "png", temp);
                        temp.deleteOnExit();

                        entityFrames[idx] = sub;
                        entityFramePaths[idx] = temp.getAbsolutePath();
                        idx++;
                    }
                }
            }

            entityFramesCached = true;
            System.out.println("Loaded " + idx + " entity frames successfully.");

        } catch (Exception e) {
            System.out.println("Failed to load entity sprites: " + e);
        }
    }

    public static void main(String[] args) {
        try {
            backgroundImg = ImageIO.read(Objects.requireNonNull(JustACircle.class.getResource("/background.png")));
        } catch (IOException e) {
            System.out.println("Failed to load background image: " + e);
        }

        StdDraw.setCanvasSize(width, height);
        StdDraw.setXscale(0, width);
        StdDraw.setYscale(0, height);
        StdDraw.enableDoubleBuffering();

        URL url = JustACircle.class.getResource("/bgm.wav");
        System.out.println(url);

        playBackgroundMusic("/bgm.wav");

        playIntroAnimation();
        loadEntitySprites(128, 128);

        double angle = 90;
        double power = 400;

        while (true) {
            drawBackground();

            if (StdDraw.isKeyPressed(KeyEvent.VK_A)) player.move(-4, 0);
            if (StdDraw.isKeyPressed(KeyEvent.VK_D)) player.move(4, 0);
            if (player.getX() - player.getRadius() < 0) player.move((player.getRadius() - player.getX()), 0);
            if (player.getX() + player.getRadius() > width) player.move((width - player.getRadius() - player.getX()), 0);

            if (StdDraw.isKeyPressed(KeyEvent.VK_UP)) power += 2;
            if (StdDraw.isKeyPressed(KeyEvent.VK_DOWN)) power -= 2;
            if (StdDraw.isKeyPressed(KeyEvent.VK_LEFT)) angle += 0.8;
            if (StdDraw.isKeyPressed(KeyEvent.VK_RIGHT)) angle -= 0.8;

            if (power < 80) power = 80;
            if (power > 900) power = 900;
            if (angle < 10) angle = 10;
            if (angle > 170) angle = 170;

            StdDraw.setPenColor(Color.WHITE);
            for (int i = 1; i <= 30; i++) {
                double t = i * 0.06;
                double px = player.getX() + power * Math.cos(Math.toRadians(angle)) * t * 0.05;
                double py = player.getY() + power * Math.sin(Math.toRadians(angle)) * t * 0.05 + 0.5 * g * Math.pow(t * 0.05, 2);
                if (py < 0) break;
                StdDraw.filledCircle(px, py, 2);
            }

            double arrowX = player.getX() + 70 * Math.cos(Math.toRadians(angle));
            double arrowY = player.getY() + 70 * Math.sin(Math.toRadians(angle));
            StdDraw.setPenColor(Color.RED);
            StdDraw.line(player.getX(), player.getY(), arrowX, arrowY);

            StdDraw.setPenColor(Color.WHITE);
            StdDraw.text(120, 460, "Angle: " + Math.round(angle) + "°");
            StdDraw.text(120, 440, "Power: " + Math.round(power));
            StdDraw.text(120, 420, "Move: A/D   Aim: Arrows   Shoot: SPACE");

            timeSinceLastShot += delta_t;
            boolean spaceNow = StdDraw.isKeyPressed(KeyEvent.VK_SPACE);
            if (spaceNow && (!lastSpace || timeSinceLastShot >= projectileSpawnCooldown)) {
                double vx = power * Math.cos(Math.toRadians(angle));
                double vy = power * Math.sin(Math.toRadians(angle));
                projectiles.add(new Projectile(player.getX(), player.getY(), 6, vx, vy));
                timeSinceLastShot = 0;
            }
            lastSpace = spaceNow;

            Iterator<Projectile> it = projectiles.iterator();
            while (it.hasNext()) {
                Projectile p = it.next();
                p.vy += g * delta_t;
                p.move(p.vx * delta_t, p.vy * delta_t);

                if (p.getY() + p.getRadius() < 0 || p.getX() < -100 || p.getX() > width + 100) {
                    it.remove();
                    continue;
                }

                double dx = p.getX() - entity.getX();
                double dy = p.getY() - entity.getY();
                if (Math.sqrt(dx * dx + dy * dy) <= p.getRadius() + entity.getRadius()) {
                    score++;
                    it.remove();
                    entity = new Circle(-40, 120 + Math.random() * 200, 55);
                    enemySpeed += 0.05;
                    continue;
                }
            }

            if (enemyMovingRight) {
                entity.move(enemySpeed, 0);
            } else {
                entity.move(-enemySpeed, 0);
            }

            if (entity.getX() + entity.getRadius() >= width) {
                entity.move(width - (entity.getX() + entity.getRadius()), 0);
                enemyMovingRight = false;
            } else if (entity.getX() - entity.getRadius() <= 0) {
                entity.move(-(entity.getX() - entity.getRadius()), 0);
                enemyMovingRight = true;
            }

            if (entityFrames != null) {
                entityAnimTimer += delta_t;
                if (entityAnimTimer >= 0.1) {
                    entityFrameIndex = (entityFrameIndex + 1) % entityFrames.length;
                    entityAnimTimer = 0;
                }
                BufferedImage currentFrame = entityFrames[entityFrameIndex];
                double scale = 1.5;

                if (entityFramesCached) {
                    String framePath = entityFramePaths[entityFrameIndex];
                    BufferedImage frameToDraw = enemyMovingRight ? currentFrame : flipImageHorizontally(currentFrame);

                    String tempPath = tempImagePath(frameToDraw);

                    StdDraw.picture(entity.getX(), entity.getY(),
                            tempPath,
                            frameToDraw.getWidth() * scale,
                            frameToDraw.getHeight() * scale);
                }

            } else {
                entity.draw();
            }

            if (entity.getX() > width + entity.getRadius()) {
                entity = new Circle(-40, 120 + Math.random() * 200, 55);
            }

            double dxPE = player.getX() - entity.getX();
            double dyPE = player.getY() - entity.getY();
            if (Math.sqrt(dxPE * dxPE + dyPE * dyPE) <= player.getRadius() + entity.getRadius()) {
                deathScreen();
                return;
            }

            if (StdDraw.isKeyPressed(KeyEvent.VK_W) && onGround) {
                playerVy = 600;
                onGround = false;
            }

            playerVy += g * delta_t;
            player.move(0, playerVy * delta_t);

            if (player.getY() - player.getRadius() <= 40) {
                playerVy = 0;
                onGround = true;
                player.move(0, 40 + player.getRadius() - player.getY());
            }

            if (entityFrames != null) {
                entityAnimTimer += delta_t;
                if (entityAnimTimer >= 0.1) {
                    entityFrameIndex = (entityFrameIndex + 1) % entityFrames.length;
                    entityAnimTimer = 0;
                }
                BufferedImage currentFrame = entityFrames[entityFrameIndex];
                double scale = 1.5;
                if (entityFramesCached) {
                    String framePath = entityFramePaths[entityFrameIndex];
                    StdDraw.picture(
                            entity.getX(), entity.getY(),
                            framePath,
                            currentFrame.getWidth() * 1.5,
                            currentFrame.getHeight() * 1.5
                    );
                }
            } else {
                entity.draw();
            }

            player.draw();
            StdDraw.setPenColor(Color.YELLOW);
            for (Projectile p : projectiles) p.draw();

            StdDraw.setPenColor(Color.WHITE);
            StdDraw.text(550, 460, "Score: " + score);

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
        if (bgm != null && bgm.isRunning()) {
            bgm.stop();
            bgm.close();
        }

        playBackgroundMusic("/deathBgm.wav");

        StdDraw.clear(Color.BLACK);
        StdDraw.picture(width / 2.0, height / 2.0,
                tempImagePath(backgroundImg),
                width, height);
        StdDraw.setPenColor(Color.RED);
        StdDraw.text(width / 2.0, height / 2.0, "YOU DIED");
        StdDraw.setPenColor(Color.BLACK);
        StdDraw.text(width / 2.0, height / 2.0 - 30, "Final Score: " + score);
        StdDraw.show();
        StdDraw.pause(3000);
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

            double scale = 3.0;

            for (BufferedImage frame : frames) {
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

    static class Projectile {
        double x, y;
        double radius;
        double vx, vy;

        public Projectile(double x, double y, double r, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.radius = r;
            this.vx = vx;
            this.vy = vy;
        }

        public void move(double dx, double dy) {
            this.x += dx;
            this.y += dy;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getRadius() { return radius; }

        public void draw() {
            StdDraw.filledCircle(x, y, radius);
        }
    }

    private static Clip bgm;

    private static void playBackgroundMusic(String path) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(
                    Objects.requireNonNull(JustACircle.class.getResource(path))
            );
            bgm = AudioSystem.getClip();
            bgm.open(audioIn);
            bgm.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            System.out.println("Failed to play background music: " + e);
        }
    }

    private static void playSound(String path) {
        new Thread(() -> {
            try {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(
                        Objects.requireNonNull(JustACircle.class.getResource(path))
                );
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            } catch (Exception e) {
                System.out.println("Failed to play sound: " + e);
            }
        }).start();
    }

    private static BufferedImage flipImageHorizontally(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage flipped = new BufferedImage(w, h, img.getType());
        Graphics2D g2d = flipped.createGraphics();
        g2d.drawImage(img, 0, 0, w, h, w, 0, 0, h, null); // flip horizontally
        g2d.dispose();
        return flipped;
    }
}