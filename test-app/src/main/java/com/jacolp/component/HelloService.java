package com.jacolp.component;

import com.jacolp.anno.Component;
import com.jacolp.anno.Scope;

@Component("helloService")
@Scope("prototype")
public class HelloService {
    public void test() {
        System.out.println("Hello prototype!");
    }
}
