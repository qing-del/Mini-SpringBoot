package com.jacolp.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 组件扫描注解
 * @author jacolp
 * <p>- 默认扫描当前包及其子包的路径</p>
 * <p>- 可以自定义扫描路径</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ComponentScan {
    String value() default "";
}
