package com.github.tor1ant.formatconverter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Генерация тестовых файлов «на лету», чтобы не хранить бинарники в репозитории.
 */
public final class TestFiles {

    private TestFiles() {
    }

    public static byte[] png(int width, int height) throws IOException {
        return image(width, height, "png");
    }

    public static byte[] jpeg(int width, int height) throws IOException {
        return image(width, height, "jpg");
    }

    private static byte[] image(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, out)) {
            throw new IllegalStateException("ImageIO не умеет писать формат " + format);
        }
        return out.toByteArray();
    }

    /**
     * PDF с заданным числом пустых страниц указанного размера.
     */
    public static byte[] pdf(int pages, PDRectangle pageSize) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                document.addPage(new PDPage(pageSize));
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Одностраничный PDF, для открытия которого нужен пароль пользователя.
     */
    public static byte[] passwordProtectedPdf(String userPassword) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.A4));

            StandardProtectionPolicy policy = new StandardProtectionPolicy("owner", userPassword, new AccessPermission());
            policy.setEncryptionKeyLength(128);
            document.protect(policy);

            document.save(out);
            return out.toByteArray();
        }
    }
}
