package com.github.tor1ant.formatconverter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * Лимиты загрузки файлов, заданные в {@code application.yml}
 * ({@code spring.servlet.multipart.*}). Нужны, чтобы показать их пользователю:
 * в подсказке на странице и в тексте ошибки при превышении.
 */
@Component
public class UploadLimits {

    private final DataSize maxFileSize;
    private final DataSize maxRequestSize;

    public UploadLimits(
            @Value("${spring.servlet.multipart.max-file-size:1MB}") DataSize maxFileSize,
            @Value("${spring.servlet.multipart.max-request-size:10MB}") DataSize maxRequestSize) {
        this.maxFileSize = maxFileSize;
        this.maxRequestSize = maxRequestSize;
    }

    /**
     * Максимальный размер одного файла.
     */
    public DataSize maxFileSize() {
        return maxFileSize;
    }

    /**
     * Максимальный суммарный размер файлов в одном запросе.
     */
    public DataSize maxRequestSize() {
        return maxRequestSize;
    }
}
