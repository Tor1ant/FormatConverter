package com.github.tor1ant.formatconverter.controller;

import com.github.tor1ant.formatconverter.service.ConvertService;
import com.github.tor1ant.formatconverter.service.PdfMergeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ConverterController {

    private static final String DEFAULT_FILE_NAME = "file";
    private static final String MERGED_FILE_NAME = "merged.pdf";

    private final ConvertService converter;
    private final PdfMergeService mergeService;

    @PostMapping("/toPdf")
    public ResponseEntity<byte[]> convertToPdf(@RequestPart("file") MultipartFile file) throws IOException {
        String originalFileName = originalFileName(file);

        byte[] pdfBytes;
        try (InputStream inputStream = file.getInputStream()) {
            pdfBytes = converter.convertImageToPdf(inputStream, originalFileName);
        }

        return pdfResponse(pdfBytes, toPdfFileName(originalFileName));
    }

    @PostMapping("/merge")
    public ResponseEntity<byte[]> mergePdfs(@RequestPart("files") List<MultipartFile> files) throws IOException {
        byte[] mergedPdf = mergeService.mergePdfs(files);

        return pdfResponse(mergedPdf, MERGED_FILE_NAME);
    }

    private static ResponseEntity<byte[]> pdfResponse(byte[] pdfBytes, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(fileName).toString())
                .body(pdfBytes);
    }

    /**
     * Формирует заголовок Content-Disposition. Имена не из ASCII (например, кириллические)
     * нельзя писать в HTTP-заголовок как есть — они кодируются по RFC 5987
     * ({@code filename*=UTF-8''...}), что понимают все современные браузеры.
     */
    static ContentDisposition attachment(String fileName) {
        ContentDisposition.Builder builder = ContentDisposition.attachment();
        boolean ascii = StandardCharsets.US_ASCII.newEncoder().canEncode(fileName);
        return (ascii ? builder.filename(fileName) : builder.filename(fileName, StandardCharsets.UTF_8)).build();
    }

    /**
     * Имя загруженного файла без пути (некоторые клиенты присылают полный путь)
     * и без управляющих символов, которым не место в HTTP-заголовке.
     */
    static String originalFileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null) {
            return DEFAULT_FILE_NAME;
        }
        String baseName = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        baseName = baseName.replaceAll("\\p{Cntrl}", "_").strip();
        return baseName.isEmpty() ? DEFAULT_FILE_NAME : baseName;
    }

    static String toPdfFileName(String originalFileName) {
        int lastDotIndex = originalFileName.lastIndexOf('.');
        String baseName = lastDotIndex > 0 ? originalFileName.substring(0, lastDotIndex) : originalFileName;
        return baseName + ".pdf";
    }
}
