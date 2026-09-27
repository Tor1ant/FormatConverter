package com.github.tor1ant.formatconverter.service;

import com.github.tor1ant.formatconverter.TestFiles;
import com.github.tor1ant.formatconverter.exception.ConversionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PdfMergeServiceTest {

    private final PdfMergeService service = new PdfMergeService();

    @Test
    void mergesAllPagesPreservingFileOrder() throws IOException {
        MultipartFile first = pdfFile("first.pdf", TestFiles.pdf(1, PDRectangle.A4));
        MultipartFile second = pdfFile("second.pdf", TestFiles.pdf(2, PDRectangle.LETTER));

        byte[] merged = service.mergePdfs(List.of(first, second));

        try (PDDocument document = Loader.loadPDF(merged)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
            // размеры страниц разные — по ним видно, что порядок файлов сохранён
            assertThat(document.getPage(0).getMediaBox().getHeight())
                    .isCloseTo(PDRectangle.A4.getHeight(), within(0.01f));
            assertThat(document.getPage(1).getMediaBox().getHeight())
                    .isCloseTo(PDRectangle.LETTER.getHeight(), within(0.01f));
            assertThat(document.getPage(2).getMediaBox().getHeight())
                    .isCloseTo(PDRectangle.LETTER.getHeight(), within(0.01f));
        }
    }

    @Test
    void requiresAtLeastTwoFiles() throws IOException {
        MultipartFile single = pdfFile("single.pdf", TestFiles.pdf(1, PDRectangle.A4));

        assertThatThrownBy(() -> service.mergePdfs(List.of(single)))
                .isInstanceOf(ConversionException.class)
                .hasMessageContaining("два");
    }

    @Test
    void rejectsFileThatIsNotPdf() throws IOException {
        MultipartFile pdf = pdfFile("real.pdf", TestFiles.pdf(1, PDRectangle.A4));
        MultipartFile fake = pdfFile("fake.pdf", "just some text".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.mergePdfs(List.of(pdf, fake)))
                .isInstanceOf(ConversionException.class)
                .hasMessageContaining("fake.pdf");
    }

    @Test
    void rejectsPasswordProtectedPdf() throws IOException {
        MultipartFile pdf = pdfFile("open.pdf", TestFiles.pdf(1, PDRectangle.A4));
        MultipartFile locked = pdfFile("locked.pdf", TestFiles.passwordProtectedPdf("secret"));

        assertThatThrownBy(() -> service.mergePdfs(List.of(pdf, locked)))
                .isInstanceOf(ConversionException.class)
                .hasMessageContaining("locked.pdf")
                .hasMessageContaining("паролем");
    }

    @Test
    void rejectsEmptyFile() throws IOException {
        MultipartFile pdf = pdfFile("ok.pdf", TestFiles.pdf(1, PDRectangle.A4));
        MultipartFile empty = pdfFile("empty.pdf", new byte[0]);

        assertThatThrownBy(() -> service.mergePdfs(List.of(pdf, empty)))
                .isInstanceOf(ConversionException.class)
                .hasMessageContaining("empty.pdf")
                .hasMessageContaining("пустой");
    }

    private static MultipartFile pdfFile(String name, byte[] content) {
        return new MockMultipartFile("files", name, "application/pdf", content);
    }
}
