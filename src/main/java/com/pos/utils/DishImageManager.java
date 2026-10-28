package com.pos.utils;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public final class DishImageManager {
    private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();

    private DishImageManager() {}

    /**
     * Lấy ImageIcon của món ăn theo ID & imagePath, có bo góc và cache bộ nhớ.
     */
    public static ImageIcon getDishIcon(int itemId, String categoryName, String itemName, int width, int height, int cornerRadius) {
        return getDishIcon(itemId, null, categoryName, itemName, width, height, cornerRadius);
    }

    public static ImageIcon getDishIcon(int itemId, String imagePath, String categoryName, String itemName, int width, int height, int cornerRadius) {
        String key = itemId + "_" + (imagePath != null ? imagePath : "") + "_" + width + "x" + height + "_" + cornerRadius;
        return CACHE.computeIfAbsent(key, k -> loadOrGenerateImage(itemId, imagePath, categoryName, itemName, width, height, cornerRadius));
    }

    public static ImageIcon getDishIcon(int itemId, String categoryName, String itemName, int width, int height) {
        return getDishIcon(itemId, null, categoryName, itemName, width, height, 12);
    }

    public static ImageIcon getDishIcon(int itemId, String imagePath, String categoryName, String itemName, int width, int height) {
        return getDishIcon(itemId, imagePath, categoryName, itemName, width, height, 12);
    }

    private static ImageIcon loadOrGenerateImage(int itemId, String imagePath, String categoryName, String itemName, int width, int height, int cornerRadius) {
        BufferedImage rawImage = null;

        List<String> candidateNames = new ArrayList<>();
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            candidateNames.add(imagePath.trim());
        }
        candidateNames.add(itemId + ".jpg");
        candidateNames.add(itemId + ".png");

        // 1. Thử load từ resource bên trong classpath (khi chạy JAR)
        for (String cName : candidateNames) {
            String path = cName.startsWith("/") ? cName : "/images/dishes/" + cName;
            try (InputStream in = DishImageManager.class.getResourceAsStream(path)) {
                if (in != null) {
                    rawImage = ImageIO.read(in);
                    if (rawImage != null) break;
                }
            } catch (Exception ignored) {}
        }

        // 2. Thử load từ thư mục images/dishes/ ngoài đĩa hoặc src/main/resources
        if (rawImage == null) {
            for (String cName : candidateNames) {
                File f = new File("images/dishes/" + cName);
                if (f.exists()) {
                    try {
                        rawImage = ImageIO.read(f);
                        if (rawImage != null) break;
                    } catch (Exception ignored) {}
                }
                File fDev = new File("src/main/resources/images/dishes/" + cName);
                if (fDev.exists()) {
                    try {
                        rawImage = ImageIO.read(fDev);
                        if (rawImage != null) break;
                    } catch (Exception ignored) {}
                }
            }
        }

        // 3. Nếu tìm thấy ảnh thực tế: Resize và bo góc
        if (rawImage != null) {
            return new ImageIcon(createRoundedImage(rawImage, width, height, cornerRadius));
        }

        // 4. Nếu chưa có ảnh: Vẽ ảnh placeholder nghệ thuật
        return new ImageIcon(createPlaceholderImage(categoryName, itemName, width, height, cornerRadius));
    }

    private static BufferedImage createRoundedImage(BufferedImage src, int width, int height, int cornerRadius) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Clip hình bo tròn
        g2.setClip(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));

        // Vẽ ảnh canh giữa và cắt tràn (Center crop)
        double scale = Math.max((double) width / src.getWidth(), (double) height / src.getHeight());
        int scaledW = (int) (src.getWidth() * scale);
        int scaledH = (int) (src.getHeight() * scale);
        int x = (width - scaledW) / 2;
        int y = (height - scaledH) / 2;

        g2.drawImage(src, x, y, scaledW, scaledH, null);
        g2.dispose();
        return output;
    }

    private static BufferedImage createPlaceholderImage(String category, String name, int width, int height, int cornerRadius) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Màu gradient theo danh mục
        Color color1 = new Color(245, 158, 11);
        Color color2 = new Color(217, 119, 6);
        String icon = "🍽️";

        if (category != null) {
            String lower = category.toLowerCase();
            if (lower.contains("nước") || lower.contains("bún") || lower.contains("mì")) {
                color1 = new Color(239, 68, 68);
                color2 = new Color(185, 28, 28);
                icon = "🍜";
            } else if (lower.contains("cơm")) {
                color1 = new Color(245, 158, 11);
                color2 = new Color(217, 119, 6);
                icon = "🍚";
            } else if (lower.contains("bánh")) {
                color1 = new Color(234, 88, 12);
                color2 = new Color(194, 65, 12);
                icon = "🥖";
            } else if (lower.contains("gỏi") || lower.contains("ăn vặt")) {
                color1 = new Color(34, 197, 94);
                color2 = new Color(21, 128, 61);
                icon = "🥗";
            } else if (lower.contains("hải sản")) {
                color1 = new Color(14, 165, 233);
                color2 = new Color(3, 105, 161);
                icon = "🦐";
            } else if (lower.contains("lẩu")) {
                color1 = new Color(225, 29, 72);
                color2 = new Color(159, 18, 57);
                icon = "🍲";
            } else if (lower.contains("đồ uống") || lower.contains("tráng miệng")) {
                color1 = new Color(13, 148, 136);
                color2 = new Color(15, 118, 110);
                icon = "🥤";
            }
        }

        GradientPaint gp = new GradientPaint(0, 0, color1, width, height, color2);
        g2.setPaint(gp);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));

        // Vẽ icon ở giữa
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, height / 3));
        FontMetrics fm = g2.getFontMetrics();
        int iconX = (width - fm.stringWidth(icon)) / 2;
        int iconY = (height / 2) + (fm.getAscent() / 3);
        g2.drawString(icon, iconX, iconY);

        g2.dispose();
        return output;
    }
}
