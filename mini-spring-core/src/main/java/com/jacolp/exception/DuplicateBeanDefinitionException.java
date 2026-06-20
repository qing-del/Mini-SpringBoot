package com.jacolp.exception;

public class DuplicateBeanDefinitionException extends BaseBeanException {
    public DuplicateBeanDefinitionException() {
        super("Duplicate bean definition!");
    }

    public DuplicateBeanDefinitionException(String message) {
        super(message);
    }
}
