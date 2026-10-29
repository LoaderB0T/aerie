import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Writes Aerie's placeholder 16x16 block textures.
 * Run: java tools/GenTextures.java src/main/resources/assets/aerie/textures/block
 */
public class GenTextures {
    static final int BLACKSTONE = 0x2b2730, GOLD = 0xd4a83a, QUARTZ = 0xe8e2d6, PURPLE = 0x8b3fbf,
            LIGHT_PURPLE = 0xc58af0, TEAL = 0x1fb5a8;

    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        dir.mkdirs();
        write(dir, "plinth", (x, y) -> rim(x, y) ? GOLD : BLACKSTONE, 0.10);
        write(dir, "column_side", (x, y) -> x == 7 || x == 8 ? PURPLE : QUARTZ, 0.04);
        write(dir, "column_end", (x, y) -> ring(x, y, 4, 6) ? PURPLE : rim(x, y) ? GOLD : QUARTZ, 0.04);
        write(dir, "focus", (x, y) -> dist(x, y) < 4 ? LIGHT_PURPLE : rim(x, y) ? GOLD : PURPLE, 0.08);
        write(dir, "core", (x, y) -> diamond(x, y, 5) ? PURPLE : rim(x, y) ? GOLD : TEAL, 0.06);
        write(dir, "relay_crown", (x, y) -> diamond(x, y, 4) ? PURPLE : rim(x, y) ? BLACKSTONE : GOLD, 0.06);
    }

    interface Pattern { int color(int x, int y); }

    static void write(File dir, String name, Pattern p, double noise) throws IOException {
        Random random = new Random(name.hashCode());
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
                img.setRGB(x, y, 0xff000000 | shade(p.color(x, y), 1 + (random.nextDouble() * 2 - 1) * noise));
        ImageIO.write(img, "png", new File(dir, name + ".png"));
    }

    static boolean rim(int x, int y) { return x == 0 || y == 0 || x == 15 || y == 15; }
    static double dist(int x, int y) { return Math.hypot(x - 7.5, y - 7.5); }
    static boolean ring(int x, int y, double r0, double r1) { double d = dist(x, y); return d >= r0 && d < r1; }
    static boolean diamond(int x, int y, double r) { return Math.abs(x - 7.5) + Math.abs(y - 7.5) < r; }

    static int shade(int rgb, double f) {
        int r = clamp((rgb >> 16 & 0xff) * f), g = clamp((rgb >> 8 & 0xff) * f), b = clamp((rgb & 0xff) * f);
        return r << 16 | g << 8 | b;
    }

    static int clamp(double v) { return (int) Math.max(0, Math.min(255, v)); }
}
