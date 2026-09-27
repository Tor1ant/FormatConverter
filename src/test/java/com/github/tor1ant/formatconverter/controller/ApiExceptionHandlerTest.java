package com.github.tor1ant.formatconverter.controller;

import com.github.tor1ant.formatconverter.config.UploadLimits;
import com.github.tor1ant.formatconverter.exception.ConversionException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.unit.DataSize;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler =
            new ApiExceptionHandler(new UploadLimits(DataSize.ofMegabytes(50), DataSize.ofMegabytes(200)));

    @Test
    void conversionErrorBecomesBadRequestWithUserMessage() {
        ProblemDetail problem = handler.handleConversionException(new ConversionException("Файл «x.txt» пустой"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Файл «x.txt» пустой");
    }

    @Test
    void tooLargeUploadMentionsConfiguredLimits() {
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(-1);

        ResponseEntity<Object> response = handler.handleMaxUploadSizeExceededException(
                exception, new HttpHeaders(), exception.getStatusCode(),
                new ServletWebRequest(new MockHttpServletRequest("POST", "/toPdf")));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(exception.getStatusCode());
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem.getDetail())
                .contains("50 МБ")
                .contains("200 МБ");
    }
}
