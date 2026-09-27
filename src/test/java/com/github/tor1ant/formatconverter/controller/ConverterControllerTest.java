package com.github.tor1ant.formatconverter.controller;

import com.github.tor1ant.formatconverter.TestFiles;
import com.github.tor1ant.formatconverter.config.UploadLimits;
import com.github.tor1ant.formatconverter.service.ConvertService;
import com.github.tor1ant.formatconverter.service.PdfMergeService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConverterController.class)
@Import({ConvertService.class, PdfMergeService.class, UploadLimits.class})
class ConverterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void convertsImageToPdfNamedAfterOriginalFile() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "scan.png", "image/png", TestFiles.png(100, 150));

        byte[] body = mockMvc.perform(multipart("/toPdf").file(image))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scan.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = Loader.loadPDF(body)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void encodesNonAsciiFileNameInContentDisposition() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "фото.jpg", "image/jpeg", TestFiles.jpeg(100, 100));

        mockMvc.perform(multipart("/toPdf").file(image))
                .andExpect(status().isOk())
                // RFC 5987: имя не из ASCII передаётся в параметре filename*, закодированном в UTF-8
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("filename*=UTF-8''%D1%84%D0%BE%D1%82%D0%BE.pdf")));
    }

    @Test
    void stripsPathFromClientProvidedFileName() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "C:\\Users\\me\\photo.png", "image/png",
                TestFiles.png(10, 10));

        mockMvc.perform(multipart("/toPdf").file(image))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"photo.pdf\""));
    }

    @Test
    void returnsProblemDetailForUnsupportedFile() throws Exception {
        MockMultipartFile text = new MockMultipartFile("file", "notes.txt", "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/toPdf").file(text))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("notes.txt")));
    }

    @Test
    void returnsBadRequestWhenFilePartIsMissing() throws Exception {
        mockMvc.perform(multipart("/toPdf"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void mergesPdfFiles() throws Exception {
        MockMultipartFile first = new MockMultipartFile("files", "a.pdf", "application/pdf",
                TestFiles.pdf(1, PDRectangle.A4));
        MockMultipartFile second = new MockMultipartFile("files", "b.pdf", "application/pdf",
                TestFiles.pdf(2, PDRectangle.A4));

        byte[] body = mockMvc.perform(multipart("/merge").file(first).file(second))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"merged.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = Loader.loadPDF(body)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
        }
    }

    @Test
    void rejectsMergeOfSingleFileWithReadableMessage() throws Exception {
        MockMultipartFile only = new MockMultipartFile("files", "a.pdf", "application/pdf",
                TestFiles.pdf(1, PDRectangle.A4));

        mockMvc.perform(multipart("/merge").file(only))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("два")));
    }

    @Test
    void reportsBrokenPdfByName() throws Exception {
        MockMultipartFile good = new MockMultipartFile("files", "good.pdf", "application/pdf",
                TestFiles.pdf(1, PDRectangle.A4));
        MockMultipartFile broken = new MockMultipartFile("files", "broken.pdf", "application/pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/merge").file(good).file(broken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("broken.pdf")));
    }
}
