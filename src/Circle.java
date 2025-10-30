import java.awt.*;

public class Circle {
    double x, y, radius;

    public Circle() {
        x = 20;
        y = 20;
        radius = 20;
    }

    public Circle(double x, double y, double radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    // Functions
    public void draw() {
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.filledCircle(x, y, radius);
    }

    public void move(double dx, double dy) {
        x += dx;
        y += dy;

        checkBoundaries();
    }

    public void checkBoundaries() {
        if((x + radius) > 640) {
            x = 640 - radius;
        }

        if((x - radius) < 0) {
            x = 0 + radius;
        }

        if((y + radius) > 480) {
            y = 480 - radius;
        }

        if((y - radius) < 0) {
            y = 0 + radius;
        }
    }

    // Getters
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getRadius() {
        return radius;
    }

    // Setters
    public void setRadius(int radius) {
        this.radius = radius;
    }

    public String toString() {
        System.out.println("~*---------------------------------------------------------*~");
        System.out.println("Ball information:\nRadius: " + radius + "\nCenter coordinate: (" + x + ", " + y + ")" + "\nCircumference: " + (2 * radius) + "π\nArea: " + (Math.pow(radius, 2)) + "π");
        return "~*---------------------------------------------------------*~\n";
    }

    public String toAdvString() {
        System.out.println("-~*--!--!--!--!--!--!--!--!--!--!--!--!--!--!--!--!--!--!--*~-");
        System.out.println("Advanced ball information:\nDiameter: " + (radius * 2) + "\nRadius: " + radius + "\nCenter coordinate: (" + x + ", " + y + ")" + "\nCircumference: " + (2 * radius) + "π\nArea: " + (Math.pow(radius, 2)) + "π\nTop-most coordinate: (" + x + ". " + (y + radius) + ")" + "\nLeft-most coordinate: (" + (x - radius) + ". " + y + ")" + "\nBottom-most coordinate: (" + x + ". " + (y - radius) + ")" + "\nRight-most coordinate: (" + (x + radius) + ". " + y + ")");
        return "-~*--!--!--!--!--!--!--!--!--!--!--!--!--!--!--!--!--!--!--*~-\n";
    }
}