package com.jacolp;


import com.jacolp.config.AnnotationConfigApplicationContext;

public class AppConfig {
    public static void main( String[] args ) {
        // 注入配置类并启动容器
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

    }
}
