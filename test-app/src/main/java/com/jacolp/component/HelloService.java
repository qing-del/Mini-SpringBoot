package com.jacolp.component;

import com.jacolp.beans.Component;
import com.jacolp.beans.Scope;

@Component("helloService")
@Scope("prototype")
public class HelloService {
    public void test() {
        System.out.println("Hello prototype!");
    }
}
