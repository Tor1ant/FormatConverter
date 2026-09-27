package com.github.tor1ant.formatconverter.service;

import com.github.tor1ant.formatconverter.exception.ConversionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ConvertService {

    /**
     * Размер страницы результирующего PDF (книжная ориентация).
     */
    static final PDRectangle PAGE_SIZE = PDRectangle.A4;

    /**
     * Конвертирует изображение (JPEG, PNG, GIF, BMP, TIFF) в одностраничный PDF.
     * <p>
     * Изображение вписывается в страницу с сохранением пропорций и центрируется.
     * Для «горизонтальных» изображений страница разворачивается в альбомную ориентацию,
     * чтобы картинка занимала максимум места.
     *
     * @param imageStream содержимое изображения
     * @param fileName    имя исходного файла — используется только в сообщениях об ошибках
     * @return содержимое PDF-файла
     * @throws ConversionException если файл пустой, повреждён или его формат не поддерживается
     * @throws IOException         при внутренней ошибке формирования PDF
     */
    public byte[] convertImageToPdf(InputStream imageStream, String fileName) throws IOException {
        byte[] imageBytes = imageStream.readAllBytes();
        if (imageBytes.length == 0) {
            throw new ConversionException("Файл «" + fileName + "» пустой");
        }

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDImageXObject image = readImage(document, imageBytes, fileName);

            PDPage page = new PDPage(pageSizeFor(image));
            document.addPage(page);

            PDRectangle mediaBox = page.getMediaBox();
            float scale = Math.min(mediaBox.getWidth() / image.getWidth(),
                    mediaBox.getHeight() / image.getHeight());
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;
            float x = (mediaBox.getWidth() - width) / 2;
            float y = (mediaBox.getHeight() - height) / 2;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(image, x, y, width, height);
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static PDImageXObject readImage(PDDocument document, byte[] imageBytes, String fileName) {
        try {
            return PDImageXObject.createFromByteArray(document, imageBytes, fileName);
        } catch (IllegalArgumentException e) {
            // Так PDFBox сообщает о неподдерживаемом формате (WebP, HEIC, PDF, текст и т.п.)
            throw new ConversionException("Формат файла «" + fileName + "» не поддерживается. "
                    + "Поддерживаются изображения JPEG, PNG, GIF, BMP и TIFF", e);
        } catch (IOException | RuntimeException e) {
            // Формат распознан, но декодировать данные не удалось
            throw new ConversionException("Не удалось прочитать изображение «" + fileName
                    + "»: файл повреждён или имеет неподдерживаемый вариант формата", e);
        }
    }

    private static PDRectangle pageSizeFor(PDImageXObject image) {
        boolean landscape = image.getWidth() > image.getHeight();
        return landscape
                ? new PDRectangle(PAGE_SIZE.getHeight(), PAGE_SIZE.getWidth())
                : PAGE_SIZE;
    }
}
