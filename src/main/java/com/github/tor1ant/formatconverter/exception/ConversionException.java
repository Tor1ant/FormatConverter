package com.github.tor1ant.formatconverter.exception;

/**
 * Ошибка обработки файла, вызванная некорректными входными данными пользователя:
 * неподдерживаемый формат, повреждённый или зашифрованный файл и т.п.
 * <p>
 * Сообщение предназначено для показа пользователю. Контроллеры отвечают на такую ошибку
 * статусом 400, а не 500 (см. {@code ApiExceptionHandler}).
 */
public class ConversionException extends RuntimeException {

    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
