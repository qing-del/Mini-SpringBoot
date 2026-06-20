package com.jacolp;

import com.jacolp.component.HelloService;
import com.jacolp.component.UserService;
import com.jacolp.context.AnnotationConfigApplicationContext;
import com.jacolp.anno.ComponentScan;

@ComponentScan("com.jacolp")
public class AppConfig {
    public static void main( String[] args ) {
        // 注入配置类并启动容器
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // 获取 Bean
        UserService userService = (UserService) context.getBean("userService");

         // 测试 能否获取 单例 Bean
        System.out.println(userService);
        userService.test();

        // 测试 能否获取 同一个单例 Bean
        System.out.println(context.getBean("userService"));
        System.out.println(context.getBean("userService"));
        System.out.println(context.getBean("userService"));


        // 测试 能否获取 原型 Bean
        HelloService helloService = (HelloService) context.getBean("helloService");
        helloService.test();

        // 测试 能否获取 同一个原型 Bean
        System.out.println(context.getBean("helloService"));
        System.out.println(context.getBean("helloService"));
        System.out.println(context.getBean("helloService"));
        System.out.println(context.getBean("helloService"));

        // 测试获取不存在的 Bean
        try {
            Object notExistBean = context.getBean("notExistBean");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("测试成功！");
        }

    }
}