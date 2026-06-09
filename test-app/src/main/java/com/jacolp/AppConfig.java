package com.jacolp;

import com.jacolp.component.UserService;
import com.jacolp.config.AnnotationConfigApplicationContext;
import com.jacolp.config.ComponentScan;

@ComponentScan("com.jacolp")
public class AppConfig {
    public static void main( String[] args ) {
        // 注入配置类并启动容器
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // 获取 Bean
        UserService userService = (UserService) context.getBean("userService");

        System.out.println(userService);

        System.out.println(context.getBean("userService"));
        System.out.println(context.getBean("userService"));
        System.out.println(context.getBean("userService"));

        userService.test();
    }
}