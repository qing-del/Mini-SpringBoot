package com.jacolp.beans;

public class BeanDefinition {
    Class beanClass;
    String beanClassName;
    String scope;

    public BeanDefinition() {
    }

    public BeanDefinition(Class beanClass, String beanClassName, String scope) {
        this.beanClass = beanClass;
        this.beanClassName = beanClassName;
        this.scope = scope;
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public void setBeanClassName(String beanClassName) {
        this.beanClassName = beanClassName;
    }

    public Class getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
