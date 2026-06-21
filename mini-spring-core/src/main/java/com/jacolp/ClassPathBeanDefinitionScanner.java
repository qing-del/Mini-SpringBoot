package com.jacolp;

import com.jacolp.anno.Component;
import com.jacolp.anno.ComponentScan;
import com.jacolp.anno.Scope;
import com.jacolp.beans.BeanDefinition;
import com.jacolp.constant.BeanScopeConstant;
import com.jacolp.context.AnnotationConfigApplicationContext;
import com.jacolp.exception.BaseBeanException;
import com.jacolp.exception.DuplicateBeanDefinitionException;
import com.jacolp.exception.NotFoundScanAnnotationException;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class ClassPathBeanDefinitionScanner {
    // BeanDefinition 列表
    private static ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, BeanDefinition> doScan(Class configClass) {

        // 获取包扫描路径
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
        if (componentScanAnnotation == null) {
            throw new NotFoundScanAnnotationException();
        }

        // 获取扫描路径
        String path = componentScanAnnotation.value();

        // 如果没有指定包扫描路径，则默认为配置类所在的包及其子包
        if (path.equals("")) {
            path = configClass.getPackage().getName();
        }

        // 获取需要扫描的包路径
        String packagePath = path.replace(".", File.separator);
        StringBuilder packageName = new StringBuilder(packagePath);

        // 获取容器的类加载器 —— 用其来获取资源路径
        ClassLoader classLoader = AnnotationConfigApplicationContext.class.getClassLoader();
        URL resource = classLoader.getResource("");
        if (resource == null) {
            return null; // 扫描路径不存在
        }

        // 找到需要扫描的类路径
        File resourceDirectory = new File(resource.getFile());
        if (!resourceDirectory.exists() || !resourceDirectory.isDirectory()) {
            return null;
        }

        // 获取最终扫描路径
        File scanDirectory = getFinalScanDirectory(resourceDirectory, packagePath);
        if (scanDirectory == null) {
            return null;
        }

        scanDirectory(scanDirectory, packageName);   // 扫描目录

        return beanDefinitionMap;
    }

    /**
     * 递归扫描扫描目录
     * @param directory 必须是一个目录
     */
    private static void scanDirectory(File directory, StringBuilder packagePath) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packagePath.append(".").append(file.getName()));    // 如果是文件夹就需要递归
            } else {
                String fileName = file.getName();
                if (!fileName.endsWith(".class")) continue; // 不是类文件就跳过

                // 将其加入到 BeanDefinition 列表中
                tryAddBeanDefinition(file, packagePath.toString());
            }
        }
    }

    /**
     * 添加 BeanDefinition 到待列表中
     * @param file
     * @param packagePath 例如：传入 "com.jacolp"
     */
    private static void tryAddBeanDefinition(File file, String packagePath) {
        Class beanClass = getBeanClassByFile(file, packagePath); // 获取类文件所对应的类

        if (!beanClass.isAnnotationPresent(Component.class)) {
            return; // 不是 Bean 组件
        }

        // 获取 Component 中定义的 Bean 名称
        String beanName = resolveBeanName(beanClass);

        // 解析并创建出 BeanDefinition
        BeanDefinition beanDefinition = resolveBeanDefinition(beanClass);

        registerBeanDefinition(beanName, beanDefinition);   // 注册 BeanDefinition
    }

    /**
     * 解析并创建 BeanDefinition
     * @param beanClass
     * @return
     */
    private static BeanDefinition resolveBeanDefinition(Class beanClass) {
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
        return beanDefinition;
    }

    /**
     * 获取 Bean 的名称
     * @param beanClass Bean 类
     * @return Bean 名称
     */
    private static String resolveBeanName(Class beanClass) {
        // 获取 Component 注解
        Component componentAnnotation = (Component) beanClass.getAnnotation(Component.class);
        String beanName = componentAnnotation.value();

        // 如果 Bean 名称没有指定，则默认使用类名
        beanName = beanName.isEmpty() ? beanClass.getSimpleName() : beanName;    // 获取 Bean 名称
        beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
        return beanName;
    }

    /**
     * 获取类文件所对应的类
     * @param file
     * @param packagePath 例如“com.jacolp”
     * @return
     */
    private static Class getBeanClassByFile(File file, String packagePath) {
        String fileName = file.getName();
        // 获取全类名
        String className = packagePath + "." + fileName.substring(0, fileName.indexOf(".class"));
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
     * 获取最终扫描目录
     * @param resourceDirectory
     * @return
     */
    private static File getFinalScanDirectory(File resourceDirectory, String packagePath) {
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
     * 注册 BeanDefinition 到 BeanDefinition 列表中
     * @param beanName Bean 名称
     * @param beanDefinition BeanDefinition
     * @throws DuplicateBeanDefinitionException 当添加的 BeanDefinition 与已存在的 BeanDefinition 名称相同时抛出
     */
    private static void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        BeanDefinition existedBeanDefinition = beanDefinitionMap.putIfAbsent(beanName, beanDefinition);

        // 如果已经存在
        if (existedBeanDefinition != null) {
            // TODO 可以给出具体是哪两个文件冲突
            throw new DuplicateBeanDefinitionException("Duplicate bean definition!" + beanName);
        }
    }
}
