package tools;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Deterministic multi-density launcher icon generator for Volkan Web2Android.
 */
public class IconGenerator {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java tools.IconGenerator <iconPath> <resDir>");
            System.exit(1);
        }

        File iconFile = new File(args[0]);
        File resDir = new File(args[1]);

        if (!iconFile.exists() || !iconFile.isFile()) {
            System.err.println("❌ Icon file does not exist: " + iconFile.getAbsolutePath());
            System.exit(1);
        }

        try {
            BufferedImage srcImage = ImageIO.read(iconFile);
            if (srcImage == null) {
                System.err.println("❌ Could not decode image file: " + iconFile.getAbsolutePath());
                System.exit(1);
            }

            System.out.println("🎨 Processing icon: " + iconFile.getName() + " (" + srcImage.getWidth() + "x" + srcImage.getHeight() + ")");

            // Standard mipmap density sizes (Square and Round)
            Map<String, Integer> densities = Map.of(
                "mipmap-mdpi", 48,
                "mipmap-hdpi", 72,
                "mipmap-xhdpi", 96,
                "mipmap-xxhdpi", 144,
                "mipmap-xxxhdpi", 192
            );

            for (Map.Entry<String, Integer> entry : densities.entrySet()) {
                File folder = new File(resDir, entry.getKey());
                folder.mkdirs();
                int size = entry.getValue();

                // Clean up any stale formats
                new File(folder, "ic_launcher.png").delete();
                new File(folder, "ic_launcher.webp").delete();
                new File(folder, "ic_launcher_round.png").delete();
                new File(folder, "ic_launcher_round.webp").delete();

                // Standard square / legacy icon
                BufferedImage squareIcon = scaleImage(srcImage, size, size, false);
                ImageIO.write(squareIcon, "PNG", new File(folder, "ic_launcher.png"));

                // Circular round icon
                BufferedImage roundIcon = scaleImage(srcImage, size, size, true);
                ImageIO.write(roundIcon, "PNG", new File(folder, "ic_launcher_round.png"));
            }

            // Adaptive Foreground (432x432 with 72dp / 288px safe zone centering)
            File drawableDir = new File(resDir, "drawable");
            drawableDir.mkdirs();

            int canvasSize = 432;
            int safeZoneSize = 288;
            int offset = (canvasSize - safeZoneSize) / 2;

            BufferedImage foregroundImage = new BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = foregroundImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(srcImage, offset, offset, safeZoneSize, safeZoneSize, null);
            g2.dispose();

            new File(drawableDir, "ic_launcher_foreground.xml").delete();
            new File(drawableDir, "ic_launcher_foreground.webp").delete();
            ImageIO.write(foregroundImage, "PNG", new File(drawableDir, "ic_launcher_foreground.png"));

            // Splash Screen Icon (288x288 crisp icon with alpha transparency for Android 12+ SplashScreen)
            int splashSize = 288;
            BufferedImage splashIcon = scaleImage(srcImage, splashSize, splashSize, false);
            new File(drawableDir, "ic_splash_icon.xml").delete();
            new File(drawableDir, "ic_splash_icon.webp").delete();
            ImageIO.write(splashIcon, "PNG", new File(drawableDir, "ic_splash_icon.png"));

            // Ensure adaptive icon XML descriptors
            File anyDpiDir = new File(resDir, "mipmap-anydpi-v26");
            anyDpiDir.mkdirs();

            String adaptiveXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <background android:drawable=\"@drawable/ic_launcher_background\" />\n" +
                "    <foreground android:drawable=\"@drawable/ic_launcher_foreground\" />\n" +
                "    <monochrome android:drawable=\"@drawable/ic_launcher_foreground\" />\n" +
                "</adaptive-icon>\n";

            java.nio.file.Files.writeString(new File(anyDpiDir, "ic_launcher.xml").toPath(), adaptiveXml);
            java.nio.file.Files.writeString(new File(anyDpiDir, "ic_launcher_round.xml").toPath(), adaptiveXml);

            System.out.println("✅ Generated launcher icons across 5 densities, adaptive xml, and splash icon in " + resDir.getAbsolutePath());
            System.exit(0);

        } catch (Exception e) {
            System.err.println("❌ Failed to generate launcher icons: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static BufferedImage scaleImage(BufferedImage src, int width, int height, boolean circular) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (circular) {
            g2.setClip(new Ellipse2D.Double(0, 0, width, height));
        }

        g2.drawImage(src, 0, 0, width, height, null);
        g2.dispose();
        return scaled;
    }
}
