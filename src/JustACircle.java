import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import javax.sound.sampled.*;
import java.util.ArrayList;

public class JustACircle {
    public static final int fps = 5;
    public static final double delta_t = 1.0 / fps;
    public static final double g = -980.66;
    public static final int width = 1280;
    public static final int height = 720;

    public static Circle player = new Circle(320, 40, 15);

    public static Circle entity = new Circle(40, 120, 58);

    public static BufferedImage[] entityFrames;
    public static int entityFrameIndex = 0;
    public static double entityAnimTimer = 0;

    public static int score = 0;
    public static double enemySpeed = 3;
    public static double projectileRadius = 6;
    public static double projectileSpawnCooldown = 0.12;
    public static double timeSinceLastShot = projectileSpawnCooldown;

    public static boolean lastSpace = false;

    private static String[] entityFramePaths;
    private static boolean entityFramesCached = false;

    public static double playerVy = 0;
    public static boolean onGround = true;

    private static BufferedImage backgroundImg;

    public static boolean enemyMovingRight = true;

    static double bgX1 = 0;
    static double bgX2 = width;
    static double bgSpeed = 70;

    static BufferedImage[] introFrames;
    static int introFrameIndex = 0;
    static double introTimer = 0;
    static final double INTRO_FRAME_TIME = 0.1;

    static String bgPath;

    static final double ENTITY_SCALE = 1.5;

    enum GameState {
        WAITING_TO_START,
        INTRO,
        CHASE,
        DEAD
    }

    static GameState gameState = GameState.WAITING_TO_START;

    static class Obstacle {
        double x, y, w, h;
    }

    static ArrayList<Obstacle> obstacles = new ArrayList<>();
    static double obstacleTimer = 0;

    static BufferedImage obstacleImg;
    static String obstaclePath;
    static final double OBSTACLE_SCALE = 1;

    static void drawStaticBackground() {
        if (bgPath == null) return;

        StdDraw.picture(
                width / 2.0,
                height / 2.0,
                bgPath,
                width,
                height
        );
    }

    static void drawScrollingBackground() {
        double bgScale = height / (double) backgroundImg.getHeight();
        double bgW = backgroundImg.getWidth() * bgScale;

        bgX1 -= bgSpeed;
        bgX2 -= bgSpeed;

        if (bgX1 <= -bgW) bgX1 = bgX2 + bgW;
        if (bgX2 <= -bgW) bgX2 = bgX1 + bgW;

        StdDraw.picture(bgX1 + bgW / 2, height / 2,
                bgPath,
                bgW, height);
        StdDraw.picture(bgX2 + bgW / 2, height / 2,
                bgPath,
                bgW, height);
    }

    static void loadIntroSprites() {
        try {
            BufferedImage sheet = ImageIO.read(
                    Objects.requireNonNull(JustACircle.class.getResource("/entitySpawn.png"))
            );

            int frameW = 128;
            int frameH = 128;

            int cols = sheet.getWidth() / frameW;
            int rows = sheet.getHeight() / frameH;

            introFrames = new BufferedImage[cols * rows];
            int idx = 0;

            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    introFrames[idx++] =
                            sheet.getSubimage(x * frameW, y * frameH, frameW, frameH);
                }
            }

            System.out.println("Loaded " + idx + " intro frames");

        } catch (Exception e) {
            System.out.println("Intro load failed: " + e);
            introFrames = null;
        }
    }

    static void updateIntro() {
        introTimer += delta_t;

        if (introTimer >= INTRO_FRAME_TIME) {
            introFrameIndex++;
            introTimer = 0;

            if (introFrameIndex >= introFrames.length) {
                gameState = GameState.CHASE;
                drawScrollingBackground();
                return;
            }
        }

        drawStaticBackground();

        BufferedImage frame = introFrames[introFrameIndex];
        StdDraw.picture(
                entity.x + 100,
                entity.y,
                tempImagePath(frame),
                frame.getWidth() * ENTITY_SCALE,
                frame.getHeight() * ENTITY_SCALE
        );

        player.draw();
    }

    private static void loadEntitySprites(int frameW, int frameH) {
        try {
            BufferedImage spriteSheet = ImageIO.read(
                    Objects.requireNonNull(JustACircle.class.getResource("/entityMoving.png"))
            );

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

    static void updateChase() throws IOException {
        drawScrollingBackground();

        player.x = width / 2.0;

        boolean space = StdDraw.isKeyPressed(KeyEvent.VK_SPACE);
        if (space && !lastSpace && onGround) {
            playerVy = 650;
            onGround = false;
        }
        lastSpace = space;

        playerVy += g * delta_t;
        player.y += playerVy * delta_t;

        if (player.y - player.radius <= 40) {
            player.y = 40 + player.radius;
            playerVy = 0;
            onGround = true;
        }

        obstacleTimer += delta_t;

        if (obstacleTimer >= 1.3) {
            Obstacle o = new Obstacle();

            o.w = obstacleImg.getWidth() * OBSTACLE_SCALE;
            o.h = obstacleImg.getHeight() * OBSTACLE_SCALE;

            o.x = width + o.w;
            o.y = 40; // ground

            obstacles.add(o);
            obstacleTimer = 0;
        }

        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle o = obstacles.get(i);

            o.x -= bgSpeed;

            StdDraw.picture(
                    o.x,
                    o.y + o.h / 2,
                    obstaclePath,
                    o.w,
                    o.h
            );

            // collision (AABB vs circle)
            double closestX = Math.max(o.x - o.w / 2,
                    Math.min(player.x, o.x + o.w / 2));
            double closestY = Math.max(o.y,
                    Math.min(player.y, o.y + o.h));

            double dx = player.x - closestX;
            double dy = player.y - closestY;

            if (dx * dx + dy * dy < player.radius * player.radius) {
                gameState = GameState.DEAD;
                deathScreen();
            }

            if (o.x + o.w < 0) {
                obstacles.remove(i);
            }
        }

        entityAnimTimer += delta_t;
        if (entityAnimTimer >= 0.1) {
            entityFrameIndex = (entityFrameIndex + 1) % entityFrames.length;
            entityAnimTimer = 0;
        }

        BufferedImage frame = entityFrames[entityFrameIndex];
        StdDraw.picture(
                entity.x + 100,
                entity.y,
                tempImagePath(frame),
                frame.getWidth() * ENTITY_SCALE,
                frame.getHeight() * ENTITY_SCALE
        );

        player.draw();

        double dx = player.x - entity.x;
        double dy = player.y - entity.y;
        if (Math.sqrt(dx * dx + dy * dy) <= player.radius + entity.radius) {
            gameState = GameState.DEAD;
            deathScreen();
        }
    }

    public static void main(String[] args) throws IOException {
        try {
            backgroundImg = ImageIO.read(
                    Objects.requireNonNull(JustACircle.class.getResource("/bg.png"))
            );

            File temp = File.createTempFile("bg_", ".png");
            ImageIO.write(backgroundImg, "png", temp);
            temp.deleteOnExit();
            bgPath = temp.getAbsolutePath();

        } catch (Exception e) {
            System.out.println("Failed to load background image");
            e.printStackTrace();
        }

        try {
            obstacleImg = ImageIO.read(
                    Objects.requireNonNull(JustACircle.class.getResource("/obstacle.png"))
            );

            File temp = File.createTempFile("obstacle_", ".png");
            ImageIO.write(obstacleImg, "png", temp);
            temp.deleteOnExit();
            obstaclePath = temp.getAbsolutePath();

        } catch (Exception e) {
            System.out.println("Failed to load obstacle image");
            e.printStackTrace();
        }

        StdDraw.setCanvasSize(width, height);
        StdDraw.setXscale(0, width);
        StdDraw.setYscale(0, height);
        StdDraw.enableDoubleBuffering();

        URL url = JustACircle.class.getResource("/bgm.wav");
        System.out.println(url);

        loadIntroSprites();
        loadEntitySprites(128, 128);
        playBackgroundMusic("/bgm.wav");

        while (true) {
            StdDraw.clear();

            switch (gameState) {

                case WAITING_TO_START:
                    gameState = GameState.INTRO;
                    break;

                case INTRO:
                    updateIntro();
                    if (gameState == GameState.CHASE) {
                        player.y = 40 + player.radius;
                        playerVy = 0;
                        onGround = true;
                    }
                    break;


                case CHASE:
                    updateChase();
                    break;

                case DEAD:
                    break;
            }

            StdDraw.show();
            StdDraw.pause(1000 / fps);
        }
    }

    private static String tempImagePath(BufferedImage img) {
        if (img == null) return null;

        try {
            File temp = File.createTempFile("img_", ".png");
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

    private static void deathScreen() throws IOException {
        if (bgm != null && bgm.isRunning()) {
            bgm.stop();
            bgm.close();
        }

        BufferedImage backgroundImg = ImageIO.read(
                Objects.requireNonNull(JustACircle.class.getResource("/background.png"))
        );

        File temp = File.createTempFile("background_", ".png");
        ImageIO.write(backgroundImg, "png", temp);
        temp.deleteOnExit();
        String backgroundPath = temp.getAbsolutePath();

        playBackgroundMusic("/deathBgm.wav");

        StdDraw.clear(Color.BLACK);
        StdDraw.picture(width / 2.0, height / 2.0,
                backgroundPath,
                width, height);
        StdDraw.setPenColor(Color.RED);
        StdDraw.text(width / 2.0, height / 2.0, "YOU DIED");
        StdDraw.setPenColor(Color.BLACK);
        StdDraw.show();
        StdDraw.pause(3000);
    }

    private static void playIntroAnimation() {
        try {
            BufferedImage spriteSheet = ImageIO.read(Objects.requireNonNull(JustACircle.class.getResource("/entitySpawn.png")));
            if (spriteSheet == null) {
                System.out.println("Spritesheet not found: " + "/entitySpawn.png");
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
                        bgPath,
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
        g2d.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);
        g2d.dispose();
        return flipped;
    }
}