package com.jacolp.beans;

import com.jacolp.exception.BaseBeanException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class BeanFactory {
    public static Object createBean(Class clazz) {
        Constructor<?>[] constructorList = clazz.getConstructors();   // 获取构造函数列表（这里仅仅可以获取 public 构造器）

        Constructor usedConstructor = null;
        for (Constructor constructor : constructorList) {
            // 获取无参构造器
            if (constructor.getParameterCount() == 0) {
                usedConstructor = constructor;
            }
        }

        // 创建对象并返回
        try {
            return usedConstructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new BaseBeanException(e.getMessage());
        }
    }
}
