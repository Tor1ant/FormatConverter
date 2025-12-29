package com.github.tor1ant.formatconverter.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ConvertService {

    public byte[] convertImageToPdf(InputStream file, String fileName) throws Exception {
        try (PDDocument document = new PDDocument();

             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            PDImageXObject image = PDImageXObject.createFromByteArray(
                    document,
                    file.readAllBytes(),
                    fileName
            );

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(image, 0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
