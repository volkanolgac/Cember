package tools;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MakeSampleIcon {
    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(15, 23, 42));
        g2.fillRoundRect(0, 0, 512, 512, 96, 96);
        g2.setColor(new Color(59, 130, 246));
        g2.fillOval(96, 96, 320, 320);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 220));
        FontMetrics fm = g2.getFontMetrics();
        String text = "V";
        int x = (512 - fm.stringWidth(text)) / 2;
        int y = (512 - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
        g2.dispose();
        ImageIO.write(img, "PNG", new File("sample-icon.png"));
        System.out.println("✅ Generated sample-icon.png (512x512)");
    }
}
