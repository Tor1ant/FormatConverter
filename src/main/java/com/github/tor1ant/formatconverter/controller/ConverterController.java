package com.github.tor1ant.formatconverter.controller;

import com.github.tor1ant.formatconverter.service.ConvertService;
import com.github.tor1ant.formatconverter.service.PdfMergeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ConverterController {

    private final ConvertService converter;
    private final PdfMergeService mergeService;

    @PostMapping(value = "/toPdf")
    public ResponseEntity<byte[]> ConvertToPdf(@RequestPart(name = "file") MultipartFile file) throws Exception {
        byte[] pdfBytes;
        try (InputStream inputStream = file.getInputStream()) {
            pdfBytes = converter.convertImageToPdf(inputStream, file.getName());
        }
        var pdfFileName = getPdfFileName(file.getOriginalFilename());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + pdfFileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping(value = "/merge")
    public ResponseEntity<byte[]> mergePdfs(@RequestPart("files") List<MultipartFile> files) throws IOException {
        var mergedPdf = mergeService.mergePdfs(files);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + "mergedPdf" + "\"")
                .body(mergedPdf);
    }

    private String getPdfFileName(String originalFilename) {
        if (originalFilename == null) {
            return "converted.pdf";
        }

        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return originalFilename.substring(0, lastDotIndex) + ".pdf";
        }

        return originalFilename + ".pdf";
    }
}
