package com.jacolp.config;

import com.jacolp.beans.BeanDefinition;
import com.jacolp.beans.BeanFactory;
import com.jacolp.beans.Component;
import com.jacolp.beans.Scope;
import com.jacolp.constant.BeanScopeConstant;
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
    // 单例 Bean 的缓存
    private ConcurrentHashMap<String, Object> singletonBeanMap = new ConcurrentHashMap<>();

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

        if (path.equals("")) {
            path = configClass.getPackage().getName();
        }

        // 获取需要扫描的包路径
        String packagePath = path.replace(".", File.separator);

        ClassLoader classLoader = AnnotationConfigApplicationContext.class.getClassLoader();
        URL resource = classLoader.getResource("");
        if (resource == null) {
            return; // 扫描路径不存在
        }

        // 找到需要扫描的类路径
        File resourceDirectory = new File(resource.getFile());
        if (!resourceDirectory.exists() || !resourceDirectory.isDirectory()) {
            return;
        }

        // 获取最终扫描路径
        File scanDirectory = getFinalScanDirectory(resourceDirectory, packagePath);
        if (scanDirectory == null) {
            return;
        }

        scanDirectory(scanDirectory);   // 扫描目录
    }

    /**
     * 获取最终扫描目录
     * @param resourceDirectory
     * @return
     */
    private File getFinalScanDirectory(File resourceDirectory, String packagePath) {
        File[] files = resourceDirectory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if (file.getPath().endsWith(packagePath)) {
                    return file;
                }
                // 递归搜索最终扫描路径
                File finalScanDirectory = getFinalScanDirectory(file, packagePath);
                if (finalScanDirectory != null) {
                    return finalScanDirectory;
                }
            }
        }
        return null;
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
        beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);

        BeanDefinition beanDefinition = new BeanDefinition();

        // 设置 BeanDefinition 的 Scope 类型
        if (beanClass.isAnnotationPresent(Scope.class)) {
            Scope scopeAnnotation = (Scope) beanClass.getAnnotation(Scope.class);
            if (scopeAnnotation.value().equals(BeanScopeConstant.PROTOTYPE)) {
                beanDefinition.setScope(BeanScopeConstant.PROTOTYPE);
            } else {
                beanDefinition.setScope(BeanScopeConstant.SINGLETON);
            }
        } else {
            beanDefinition.setScope(BeanScopeConstant.SINGLETON);   // 默认单例
        }

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

    /**
     * 获取 Bean
     * @param beanName
     * @return
     */
    public Object getBean(String beanName) {
        // 获取 BeanDefinition
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new BaseBeanException("BeanDefinition not found!");
        }

        Object bean;
        // 检查是不是单例 Bean
        if (beanDefinition.getScope().equals(BeanScopeConstant.SINGLETON)) {
            if (!singletonBeanMap.containsKey(beanName)) {
                bean = BeanFactory.createBean(beanDefinition.getBeanClass());   // 创建单例 Bean
                singletonBeanMap.putIfAbsent(beanName, bean);   // 缓存单例 Bean
            }
            bean = singletonBeanMap.get(beanName);  // 获取单例 Bean
        } else {
            bean = BeanFactory.createBean(beanDefinition.getBeanClass());   // 原型 Bean
        }

        return bean;
    }
}
