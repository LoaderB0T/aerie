import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Writes Aerie's 16x16 block textures (animated ones as vertical frame strips with a .mcmeta).
 * Run: java tools/GenTextures.java src/main/resources/assets/aerie/textures/block [preview.png]
 */
public class GenTextures {
    // Colour ramps, dark to light.
    static final int[] BLACKSTONE = {0x141019, 0x1e1824, 0x29212f, 0x352b3d, 0x44384e, 0x56485f};
    static final int[] GOLD = {0x5a3c0e, 0x8a6118, 0xb98a28, 0xe0b546, 0xf6d978, 0xfff4c4};
    static final int[] QUARTZ = {0x9c9286, 0xbab0a3, 0xd6cdc0, 0xe9e2d6, 0xf6f1e8, 0xffffff};
    static final int[] AMETHYST = {0x22093a, 0x3d145e, 0x5e2390, 0x8840c2, 0xb26ee6, 0xdcb4ff, 0xf6eaff};
    static final int[] ENDER = {0x03181c, 0x07332f, 0x0e5a52, 0x188f80, 0x2fc9b4, 0x8ff5e4, 0xe6fffb};

    interface Painter { int color(int x, int y, int frame, int frames); }

    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        dir.mkdirs();
        BufferedImage[] sheet = {
                write(dir, "plinth", 1, GenTextures::plinth),
                write(dir, "column_side", 16, GenTextures::columnSide),
                write(dir, "column_end", 1, GenTextures::columnEnd),
                write(dir, "focus", 16, GenTextures::focus),
                write(dir, "core", 32, GenTextures::core),
                write(dir, "relay_crown", 16, GenTextures::relayCrown),
        };
        if (args.length > 1) preview(sheet, new File(args[1]));
    }

    // ---------- blocks ----------

    /** Polished blackstone slab with a bevel, a gold inlay ring, corner studs and a faint violet rune. */
    static int plinth(int x, int y, int f, int n) {
        int base = BLACKSTONE[1 + (int) (noise(x, y, 3) * 2.2)];
        if (x == 0 || y == 0) base = BLACKSTONE[4];
        if (x == 15 || y == 15) base = BLACKSTONE[0];
        boolean ring = inRect(x, y, 2, 13) && (x == 2 || x == 13 || y == 2 || y == 13);
        if (ring) base = GOLD[(x == 2 || y == 2) ? 4 : 2];
        if (isCorner(x, y, 2, 13)) base = GOLD[5];
        if (inRect(x, y, 3, 12)) {
            double d = Math.abs(x - 7.5) + Math.abs(y - 7.5);
            if (Math.abs(d - 3.0) < 0.6) base = AMETHYST[4];
            else if (d < 1.1) base = AMETHYST[5];
            else if (Math.abs(d - 3.0) < 1.6) base = mix(base, AMETHYST[2], 0.45);
        }
        return base;
    }

    /** Fluted quartz with gold bands and a violet vein; a glow travels up the vein. */
    static int columnSide(int x, int y, int f, int n) {
        int flute = x % 5;
        int base = QUARTZ[flute == 0 ? 1 : flute == 1 ? 4 : 2 + (int) (noise(x, y, 7) * 1.6)];
        if (y <= 1 || y >= 14) {
            int[] band = {5, 3, 2, 2, 3, 1};
            base = GOLD[band[y <= 1 ? y : y - 12]];
            if ((x + (y <= 1 ? 0 : 2)) % 4 == 0) base = GOLD[1];
        } else if (x == 7 || x == 8) {
            double pulse = wave(13 - y, f, n, 12, 3.0);
            base = AMETHYST[2 + (int) Math.round(pulse * 4)];
        } else if (x == 6 || x == 9) {
            double pulse = wave(13 - y, f, n, 12, 3.0);
            base = mix(base, AMETHYST[3], 0.15 + pulse * 0.35);
        }
        return base;
    }

    /** Quartz cap: gold rim, a carved ring and an amethyst cabochon. */
    static int columnEnd(int x, int y, int f, int n) {
        double d = dist(x, y);
        int base = QUARTZ[2 + (int) (noise(x, y, 11) * 1.8)];
        if (x == 0 || y == 0 || x == 15 || y == 15) base = GOLD[(x == 0 || y == 0) ? 4 : 1];
        if (Math.abs(d - 5.5) < 0.55) base = QUARTZ[0];
        if (Math.abs(d - 4.8) < 0.4) base = QUARTZ[5];
        if (d < 3.2) base = AMETHYST[2 + (int) ((3.2 - d) * 1.1) + (x < 8 && y < 8 ? 1 : 0)];
        if (x == 6 && y == 6) base = AMETHYST[6];
        return base;
    }

    /** Faceted amethyst in a gold-cornered setting; the heart of the crystal breathes. */
    static int focus(int x, int y, int f, int n) {
        double cx = x - 7.5, cy = y - 7.5;
        double d = Math.abs(cx) + Math.abs(cy);
        // Facet lighting: light from the top-left.
        int facet = (cx < 0 ? 1 : 0) + (cy < 0 ? 1 : 0);
        int base = AMETHYST[1 + facet + (d < 5 ? 1 : 0)];
        if (Math.abs(cx - cy) < 0.6 || Math.abs(cx + cy) < 0.6) base = AMETHYST[Math.max(0, 1 + facet - 1)];
        double breath = 0.5 + 0.5 * Math.sin(2 * Math.PI * f / n);
        double glowR = 2.2 + breath * 1.6;
        if (d < glowR) base = mix(base, AMETHYST[6], 0.55 * (1 - d / glowR) + 0.25 * breath);
        if (d >= 6.5 && d < 7.5) base = AMETHYST[0];
        if (bracket(x, y)) base = GOLD[(x < 8) == (y < 8) ? 4 : 2];
        if (sparkle(x, y, f, n, 3)) base = AMETHYST[6];
        return base;
    }

    /** Ornate frame around a swirling ender vortex ringed by four violet runes. */
    static int core(int x, int y, int f, int n) {
        if (x == 0 || y == 0 || x == 15 || y == 15) return BLACKSTONE[(x == 0 || y == 0) ? 4 : 0];
        if (x == 1 || y == 1 || x == 14 || y == 14) {
            boolean filigree = (x + y) % 3 == 0;
            return filigree ? GOLD[4] : GOLD[2];
        }
        double cx = x - 7.5, cy = y - 7.5;
        double r = Math.hypot(cx, cy);
        double a = Math.atan2(cy, cx);
        double phase = 2 * Math.PI * f / n;
        double swirl = 0.5 + 0.5 * Math.sin(3 * a + r * 1.25 - phase);
        int base = ENDER[1 + (int) (swirl * 3.4)];
        if (r < 1.6) base = ENDER[6];
        else if (r < 2.6) base = ENDER[5];
        // Four runes at N/E/S/W that pulse in turn.
        int[][] runes = {{7, 3}, {12, 7}, {8, 12}, {3, 8}};
        for (int i = 0; i < 4; i++) {
            int rx = runes[i][0], ry = runes[i][1];
            if (Math.abs(x - rx) + Math.abs(y - ry) <= 1) {
                double lit = 0.5 + 0.5 * Math.sin(phase - i * Math.PI / 2);
                base = AMETHYST[3 + (int) Math.round(lit * 3)];
            }
        }
        if (inRect(x, y, 2, 13) && (x == 2 || y == 2 || x == 13 || y == 13)) base = mix(base, BLACKSTONE[2], 0.6);
        return base;
    }

    /** Engraved gold plate with a faceted violet gem; a glint sweeps across. */
    static int relayCrown(int x, int y, int f, int n) {
        int base = GOLD[noise(x, y, 5) > 0.85 ? 3 : 2];
        if ((x + y) % 4 == 0) base = GOLD[1];
        else if ((x + y) % 4 == 1) base = GOLD[3];
        if (x == 0 || y == 0) base = GOLD[5];
        if (x == 15 || y == 15) base = GOLD[0];
        if (isCorner(x, y, 1, 14)) base = BLACKSTONE[3];
        double d = Math.abs(x - 7.5) + Math.abs(y - 7.5);
        if (d < 5) {
            int facet = (x < 8 ? 1 : 0) + (y < 8 ? 1 : 0);
            base = AMETHYST[2 + facet];
            if (d < 2) base = AMETHYST[5];
        } else if (d < 6) base = BLACKSTONE[2];
        // Diagonal glint sweeping from top-left to bottom-right.
        int sweep = (int) Math.round((double) f / n * 44) - 8;
        if (Math.abs((x + y) - sweep) <= 1 && d < 6) base = mix(base, 0xffffff, Math.abs((x + y) - sweep) == 0 ? 0.7 : 0.35);
        return base;
    }

    // ---------- helpers ----------

    static BufferedImage write(File dir, String name, int frames, Painter p) throws IOException {
        BufferedImage img = new BufferedImage(16, 16 * frames, BufferedImage.TYPE_INT_ARGB);
        for (int fr = 0; fr < frames; fr++)
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++)
                    img.setRGB(x, fr * 16 + y, 0xff000000 | p.color(x, y, fr, frames));
        ImageIO.write(img, "png", new File(dir, name + ".png"));
        File meta = new File(dir, name + ".png.mcmeta");
        if (frames > 1) Files.writeString(meta.toPath(), "{\n  \"animation\": { \"frametime\": 2, \"interpolate\": true }\n}\n");
        else Files.deleteIfExists(meta.toPath());
        return img;
    }

    /** Each texture's first frame, plus a strip of its animation, at 8x. */
    static void preview(BufferedImage[] sheet, File out) throws IOException {
        int scale = 8, tile = 16 * scale, pad = 12, cols = 8;
        BufferedImage img = new BufferedImage(pad + (tile + pad) * (cols + 1), pad + (tile + pad) * sheet.length, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x2b2b2b));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        for (int row = 0; row < sheet.length; row++) {
            BufferedImage t = sheet[row];
            int frames = t.getHeight() / 16;
            int shown = Math.min(cols, frames);
            for (int c = 0; c <= shown; c++) {
                int frame = c == 0 ? 0 : (c - 1) * frames / shown;
                if (c > 0 && frames == 1) break;
                BufferedImage sub = t.getSubimage(0, frame * 16, 16, 16);
                g.drawImage(sub, pad + c * (tile + pad), pad + row * (tile + pad), tile, tile, null);
            }
        }
        ImageIO.write(img, "png", out);
    }

    static double noise(int x, int y, int seed) {
        long h = x * 374761393L + y * 668265263L + seed * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return ((h ^ (h >>> 16)) & 0xffff) / 65536.0;
    }

    /** 0..1 brightness of a glow travelling along a 0..len line, width w. */
    static double wave(int along, int f, int n, int len, double w) {
        double head = (double) f / n * (len + 2 * w) - w;
        return Math.max(0, 1 - Math.abs(along - head) / w);
    }

    static boolean sparkle(int x, int y, int f, int n, int count) {
        for (int i = 0; i < count; i++) {
            int sx = 3 + (int) (noise(i, 1, 41) * 10), sy = 3 + (int) (noise(i, 2, 43) * 10);
            int on = (int) (noise(i, 3, 47) * n);
            if (x == sx && y == sy && Math.abs(f - on) <= 1) return true;
        }
        return false;
    }

    static boolean inRect(int x, int y, int lo, int hi) { return x >= lo && x <= hi && y >= lo && y <= hi; }
    static boolean isCorner(int x, int y, int lo, int hi) { return (x == lo || x == hi) && (y == lo || y == hi); }
    static boolean bracket(int x, int y) {
        boolean edgeX = x <= 1 || x >= 14, edgeY = y <= 1 || y >= 14;
        boolean nearX = x <= 3 || x >= 12, nearY = y <= 3 || y >= 12;
        return (edgeX && nearY) || (edgeY && nearX);
    }
    static double dist(int x, int y) { return Math.hypot(x - 7.5, y - 7.5); }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) ((a >> 16 & 0xff) * (1 - t) + (b >> 16 & 0xff) * t);
        int g = (int) ((a >> 8 & 0xff) * (1 - t) + (b >> 8 & 0xff) * t);
        int bl = (int) ((a & 0xff) * (1 - t) + (b & 0xff) * t);
        return r << 16 | g << 8 | bl;
    }
}
