package com.jacolp.context;

import com.jacolp.ClassPathBeanDefinitionScanner;

import com.jacolp.beans.BeanDefinition;
import com.jacolp.beans.BeanFactory;
import com.jacolp.constant.BeanScopeConstant;
import com.jacolp.exception.BaseBeanException;

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

        // 扫描包
        beanDefinitionMap = ClassPathBeanDefinitionScanner.doScan(configClass);

        preInstantiateSingletons(); // 提前实例化单例 Bean
    }

    /**
     * 提前实例化单例 Bean
     */
    private void preInstantiateSingletons() {
        // 遍历 BeanDefinition 列表
        beanDefinitionMap.forEach((beanName, beanDefinition) -> {
            if (beanDefinition.getScope().equals(BeanScopeConstant.SINGLETON)) {    // 如果是单例 Bean
                Object bean = BeanFactory.createBean(beanDefinition.getBeanClass());
                singletonBeanMap.put(beanName, bean);
            }
        });
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
