package com.jacolp.config;

import com.jacolp.beans.BeanDefinition;
import com.jacolp.beans.Component;
import com.jacolp.exception.BaseBeanException;
import com.jacolp.exception.NotFoundScanAnnotationException;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationConfigApplicationContext {
    // 配置类
    private Class configClass;

    // BeanDefinition 列表
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

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

        scanDirectory(directory);   // 扫描目录

        System.out.println(beanDefinitionMap);
    }

    /**
     * 递归扫描扫描目录
     * @param directory 必须是一个目录
     */
    private void scanDirectory(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file);    // 如果是文件夹就需要递归
            } else {
                String fileName = file.getName();
                if (!fileName.endsWith(".class")) continue; // 不是类文件就跳过

                // 将其加入到 BeanDefinition 列表中
                tryAddBeanDefinition(file);
            }
        }
    }

    /**
     * 添加 BeanDefinition 到待列表中
     * @param file
     */
    private void tryAddBeanDefinition(File file) {
        Class beanClass = getBeanClassByFile(file); // 获取类文件所对应的类

        if (!beanClass.isAnnotationPresent(Component.class)) {
            return; // 不是 Bean 组件
        }

        String beanName = beanClass.getSimpleName();    // 获取 Bean 名称

        BeanDefinition beanDefinition = new BeanDefinition();
        beanDefinition.setBeanClass(beanClass); // 设置 BeanDefinition 的 Bean 类
        registerBeanDefinition(beanName, beanDefinition);   // 注册 BeanDefinition
    }

    /**
     * 获取类文件所对应的类
     * @param file
     * @return
     */
    private Class getBeanClassByFile(File file) {
        String absolutePath = file.getAbsolutePath();   // 获取绝对路径
        // 获取全类名
        String className = absolutePath.substring(absolutePath.indexOf("classes") + 8, absolutePath.indexOf(".class"));
        Class beanClass = null;
        try {
            beanClass = Class.forName(className.replace(File.separator, "."));
        } catch (ClassNotFoundException e) {
            throw new BaseBeanException(e.getMessage());
        }

        if (beanClass == null) {
            throw new BaseBeanException("Class can't load component of class!");
        }

        return beanClass;
    }

    /**
     * 注册 BeanDefinition 到 BeanDefinition 列表中
     * @param beanName
     * @param beanDefinition
     */
    private void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        beanDefinitionMap.putIfAbsent(beanName, beanDefinition);
    }

}
