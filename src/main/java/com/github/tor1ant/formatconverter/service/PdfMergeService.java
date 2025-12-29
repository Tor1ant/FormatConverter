package com.github.tor1ant.formatconverter.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfMergeService {

    public byte[] mergePdfs(List<MultipartFile> files) throws IOException {
        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            pdfMergerUtility.setDestinationStream(outputStream);

            for (MultipartFile file : files) {
                byte[] fileBytes = file.getBytes();
                RandomAccessRead randomAccessRead = new RandomAccessReadBuffer(fileBytes);
                pdfMergerUtility.addSource(randomAccessRead);
            }

            pdfMergerUtility.mergeDocuments(null);
            return outputStream.toByteArray();
        }
    }
}
