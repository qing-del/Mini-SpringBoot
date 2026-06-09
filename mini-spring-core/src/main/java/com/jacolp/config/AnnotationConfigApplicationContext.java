package com.jacolp.config;

import com.jacolp.exception.NotFoundScanAnnotationException;

import java.io.File;
import java.net.URL;

public class AnnotationConfigApplicationContext {
    // 配置类
    private Class configClass;

    /**
     * 启动容器
     */
    public AnnotationConfigApplicationContext(Class configClass) {
        // 获取配置类
        this.configClass = configClass;

        // 获取包扫描路径
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
        if (componentScanAnnotation == null) {
            throw new NotFoundScanAnnotationException();
        }

        // 获取扫描路径
        String path = componentScanAnnotation.value();
        // 获取需要扫描的包路径
        String packagePath = path.replace(".", File.separator);

        ClassLoader classLoader = AnnotationConfigApplicationContext.class.getClassLoader();
        URL resource = classLoader.getResource("");
        if (resource == null) {
            return; // 扫描路径不存在
        }

        // 找到需要扫描的类路径
        File directory = new File(resource.getFile());
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        scanDirectory(directory);
    }

    /**
     * 递归扫描扫描目录
     * @param directory 必须是一个目录
     */
    public void scanDirectory(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file);
            } else {
                String fileName = file.getName();
                if (!fileName.endsWith(".class")) continue;
                System.out.println(fileName);
            }
        }
    }
}
