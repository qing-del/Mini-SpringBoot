package com.jacolp.exception;

public class NotFoundScanAnnotationException extends RuntimeException {
    public NotFoundScanAnnotationException() {
        super("配置类缺少ComponentScan注解");
    }

    public NotFoundScanAnnotationException(String message) {
        super(message);
    }
}
