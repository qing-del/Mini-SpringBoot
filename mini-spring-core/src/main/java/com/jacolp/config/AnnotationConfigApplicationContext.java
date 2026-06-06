package com.jacolp.config;

public class AnnotationConfigApplicationContext {
    // 配置类
    private Class configClass;

    /**
     * 启动容器
     */
    public AnnotationConfigApplicationContext(Class configClass) {
        // 获取配置类
        this.configClass = configClass;

        System.out.println("获取配置类成功！");
    }
}
