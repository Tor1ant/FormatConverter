package com.github.tor1ant.formatconverter.service;

import com.github.tor1ant.formatconverter.exception.ConversionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfMergeService {

    /**
     * Объединяет несколько PDF-файлов в один. Страницы идут в порядке переданных файлов.
     *
     * @param files PDF-файлы (не менее двух)
     * @return содержимое объединённого PDF
     * @throws ConversionException если файлов меньше двух либо какой-то из них пустой,
     *                             повреждён, не является PDF или защищён паролем
     * @throws IOException         при внутренней ошибке формирования PDF
     */
    public byte[] mergePdfs(List<MultipartFile> files) throws IOException {
        if (files.size() < 2) {
            throw new ConversionException("Для объединения выберите как минимум два PDF-файла");
        }

        PDFMergerUtility merger = new PDFMergerUtility();

        try (PDDocument merged = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            for (MultipartFile file : files) {
                // Каждый источник открывается и закрывается отдельно: appendDocument копирует
                // содержимое страниц в целевой документ, поэтому держать источники открытыми не нужно.
                try (PDDocument source = loadPdf(file)) {
                    appendDocument(merger, merged, source, displayName(file));
                }
            }

            merged.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static PDDocument loadPdf(MultipartFile file) throws IOException {
        String fileName = displayName(file);
        if (file.isEmpty()) {
            throw new ConversionException("Файл «" + fileName + "» пустой");
        }

        byte[] bytes = file.getBytes();
        try {
            return Loader.loadPDF(bytes);
        } catch (InvalidPasswordException e) {
            throw new ConversionException("Файл «" + fileName + "» защищён паролем — "
                    + "снимите защиту и попробуйте снова", e);
        } catch (IOException | RuntimeException e) {
            // На битых данных PDFBox может бросить не только IOException
            throw new ConversionException("Файл «" + fileName + "» повреждён или не является PDF", e);
        }
    }

    private static void appendDocument(PDFMergerUtility merger, PDDocument destination,
                                       PDDocument source, String fileName) {
        try {
            merger.appendDocument(destination, source);
        } catch (IOException e) {
            // Например, документ с динамической XFA-формой — PDFBox отказывается такие объединять
            throw new ConversionException("Не удалось добавить файл «" + fileName + "» в объединённый PDF", e);
        }
    }

    private static String displayName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return StringUtils.hasText(name) ? name : "без имени";
    }
}
