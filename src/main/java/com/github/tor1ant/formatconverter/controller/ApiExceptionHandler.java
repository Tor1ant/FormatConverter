package com.github.tor1ant.formatconverter.controller;

import com.github.tor1ant.formatconverter.config.UploadLimits;
import com.github.tor1ant.formatconverter.exception.ConversionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Переводит ошибки в ответы формата RFC 9457 (Problem Details), чтобы фронтенд мог
 * показать пользователю осмысленное сообщение из поля {@code detail}.
 * <p>
 * Наследование от {@link ResponseEntityExceptionHandler} даёт такой же формат
 * и для стандартных ошибок Spring MVC (отсутствующая часть запроса, 404, 405 и т.д.).
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private final UploadLimits uploadLimits;

    /**
     * Некорректный ввод пользователя (не картинка, битый или зашифрованный PDF и т.п.) — это 400, а не 500.
     */
    @ExceptionHandler(ConversionException.class)
    public ProblemDetail handleConversionException(ConversionException ex) {
        log.debug("Запрос отклонён: {}", ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Превышен лимит размера загрузки — подставляем в сообщение действующие лимиты.
     */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = "Файл слишком большой. Максимальный размер одного файла — %d МБ, всех файлов в запросе — %d МБ"
                .formatted(uploadLimits.maxFileSize().toMegabytes(), uploadLimits.maxRequestSize().toMegabytes());
        log.debug("Запрос отклонён: {}", detail);
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        return handleExceptionInternal(ex, body, headers, status, request);
    }
}
