import java.awt.*;

public class JustACircle {
    public static final int fps = 1000;
    public static final double delta_t = 1.0 / fps;
    public static boolean done = false;

    public static final double g = 980.66;
    public static final double ay = -g;
    public static final int vx = 0, vy = 0;

    public static final int width = 640;
    public static final int height = 480;

    public static Circle circ = new Circle(100, 480, 10);

    public static void run() {
    {
        StdDraw.setXscale(0, width);
        StdDraw.setYscale(0, height);
        StdDraw.setCanvasSize(width, height);
        StdDraw.enableDoubleBuffering();

        int frame = 0;
        double time = 0;
        double dy = 0;
        double vt = 0;
        double cv = 0;

        System.out.println(circ);

        System.out.println("Time at start: " + time);
        System.out.println("y at start: " + (circ.getY() - circ.getRadius()));

        // main animation loop
        while (!done) {
            frame++;
            time += delta_t;

            StdDraw.setPenColor(0, 255, 166);
            StdDraw.filledRectangle(0, 0, width, height);

            cv = ay * delta_t;
            vt += cv;
            dy = vt * delta_t;

            circ.move(vx, dy);

            circ.draw();

            StdDraw.show();

            StdDraw.pause(1000 / fps);

            if(circ.getY() - circ.getRadius() == 0) {
                done = true;
                break;
            }
        }

        System.out.println("Time at end: " + time + "\n");

        System.out.println(circ.toAdvString());
    }}

    public static void main(String[] args) {
        run();
    }
}