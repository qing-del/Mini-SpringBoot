# mini-spring-core 第一阶段计划：先把 IoC 容器跑起来

这份计划的目标不是一口气复刻完整 Spring，而是先实现一个能工作的最小核心：通过配置类启动容器，扫描指定包下的组件，把类注册成 BeanDefinition，创建单例对象，并能通过 `getBean` 取出来。

你现在已经写了：

- `AnnotationConfigApplicationContext`
- `@ComponentScan`
- `test-app` 里的 `AppConfig`

所以第一阶段就顺着这个方向继续，不急着上 AOP、事务、Web、自动配置。先把“容器是什么、Bean 怎么被发现、怎么被创建、怎么被管理”这几个问题吃透。

## 1. 第一阶段总目标

完成后，你应该能写出类似下面的测试代码：

```java
@Component
public class UserService {
}

@ComponentScan("com.jacolp")
public class AppConfig {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(AppConfig.class);

        UserService userService = (UserService) context.getBean("userService");
        System.out.println(userService);
    }
}
```

预期效果：

- 容器启动时读取 `@ComponentScan("com.jacolp")`
- 扫描 `com.jacolp` 包下的 `.class` 文件
- 找到带 `@Component` 的类
- 生成并保存 BeanDefinition
- 创建单例 Bean
- `context.getBean("userService")` 能拿到对象

## 2. 推荐实现顺序

### 第一步：补齐基础注解

先实现最小注解集合：

- `@Component`
- `@ComponentScan`
- `@Scope`

建议先支持两种 scope：

- `singleton`：默认值，容器启动时创建一次
- `prototype`：每次 `getBean` 都创建一个新对象

这一阶段可以先不做：

- `@Autowired`
- `@Value`
- `@Configuration`
- `@Bean`

原因是：这些会引入依赖注入、配置方法解析、类型转换等新问题，容易把第一阶段搞散。

### 第二步：设计 BeanDefinition

`BeanDefinition` 是你理解 Spring 的关键入口。它不是 Bean 本身，而是“创建 Bean 所需的元信息”。

建议字段：

```java
public class BeanDefinition {
    private Class beanClass;
    private String scope;
}
```

后面可以再扩展：

- beanName
- lazyInit
- initMethodName
- destroyMethodName
- dependsOn

第一阶段只保留 `beanClass` 和 `scope` 就够了。

### 第三步：容器里准备两个 Map

在 `AnnotationConfigApplicationContext` 里先准备两个核心容器：

```java
private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
private Map<String, Object> singletonObjects = new HashMap<>();
```

含义：

- `beanDefinitionMap`：保存所有 Bean 的定义信息
- `singletonObjects`：保存已经创建好的单例对象

这两个 Map 是第一阶段的主线。你可以把它们理解成：

- BeanDefinitionMap 管“有哪些 Bean，怎么创建”
- SingletonObjects 管“已经创建出来的 Bean 实例”

### 第四步：实现包扫描

容器启动时做这几件事：

1. 从配置类上读取 `@ComponentScan`
2. 拿到扫描路径，比如 `com.jacolp`
3. 把包路径转换成资源路径，比如 `com/jacolp`
4. 通过类加载器找到这个目录
5. 遍历目录里的 `.class` 文件
6. 加载 Class
7. 判断类上有没有 `@Component`

注意这里第一阶段先扫描文件系统目录就行，也就是适配你本地开发环境。暂时不用处理 jar 包里的 class 扫描。

### 第五步：生成 beanName

为了简单，第一阶段可以按 Spring 的常见默认规则：

- 如果 `@Component("xxx")` 指定了名字，就用指定名字
- 如果没指定，就用类名首字母小写

例子：

- `UserService` -> `userService`
- `OrderService` -> `orderService`
- `URLParser` 这种特殊命名第一阶段可以先不纠结

### 第六步：注册 BeanDefinition

扫描到一个组件类后：

1. 解析 beanName
2. 解析 scope，没写 `@Scope` 就默认 `singleton`
3. 创建 `BeanDefinition`
4. 放入 `beanDefinitionMap`

伪代码：

```java
BeanDefinition beanDefinition = new BeanDefinition();
beanDefinition.setBeanClass(clazz);
beanDefinition.setScope(scope);
beanDefinitionMap.put(beanName, beanDefinition);
```

### 第七步：创建单例 Bean

扫描和注册完成后，遍历 `beanDefinitionMap`：

- 如果 scope 是 `singleton`，就创建对象并放进 `singletonObjects`
- 如果 scope 是 `prototype`，先不创建，等 `getBean` 时再创建

第一阶段创建对象只用无参构造：

```java
Object instance = clazz.getConstructor().newInstance();
```

先不支持构造器注入，因为那会牵扯依赖解析。

### 第八步：实现 getBean

`getBean(String beanName)` 的逻辑：

1. 从 `beanDefinitionMap` 找 BeanDefinition
2. 如果找不到，抛异常
3. 如果是 singleton，从 `singletonObjects` 返回
4. 如果是 prototype，调用 `createBean` 创建新对象并返回

这一步做完，第一阶段的容器就真正“能用了”。

## 3. 建议包结构

可以先这样放：

```text
mini-spring-core
└── src/main/java/com/jacolp
    ├── annotation
    │   ├── Component.java
    │   ├── ComponentScan.java
    │   └── Scope.java
    ├── bean
    │   └── BeanDefinition.java
    └── context
        └── AnnotationConfigApplicationContext.java
```

你现在的包名是 `com.jacolp.config`，也可以继续用，不一定马上重构。只是从学习角度看：

- `annotation` 放注解更清楚
- `bean` 放 Bean 元信息
- `context` 放容器启动逻辑

第一阶段如果你想少改动，也可以先保持现在的包结构，等功能跑通后再整理。

## 4. 第一阶段暂时不要做的事

这些功能很重要，但建议放到后面：

- 依赖注入：`@Autowired`
- 生命周期：`BeanPostProcessor`、`InitializingBean`
- 配置类方法：`@Configuration`、`@Bean`
- AOP：动态代理、切点、通知
- Web：Controller、RequestMapping、Tomcat
- Spring Boot 自动配置：starter、条件装配
- 循环依赖：三级缓存

原因很简单：如果第一阶段就全塞进去，最后会变成“每个概念都碰了一点，但容器主流程没吃透”。

## 5. 阶段验收清单

第一阶段完成时，至少满足这些条件：

- 能通过 `new AnnotationConfigApplicationContext(AppConfig.class)` 启动容器
- `@ComponentScan` 能指定扫描包
- `@Component` 类能被扫描出来
- 能生成默认 beanName
- 能保存 BeanDefinition
- 能创建 singleton Bean
- 能通过 `getBean(String beanName)` 获取 Bean
- singleton 多次获取是同一个对象
- prototype 多次获取是不同对象
- 找不到 Bean 时能抛出清晰异常

建议你在 `test-app` 里写一个非常小的验证：

```java
Object bean1 = context.getBean("userService");
Object bean2 = context.getBean("userService");
System.out.println(bean1 == bean2);
```

singleton 应该输出：

```text
true
```

prototype 应该输出：

```text
false
```

## 6. 学习时重点想明白的问题

写代码时可以一直带着这些问题：

- 配置类的作用是什么？
- `@ComponentScan` 为什么要运行时保留？
- ClassLoader 是怎么通过包名找到 class 文件的？
- BeanDefinition 和 Bean 实例有什么区别？
- 为什么 singleton 要缓存？
- prototype 为什么不能放进 singletonObjects？
- `getBean` 是直接 new 对象，还是从容器拿对象？
- 如果类没有无参构造，会发生什么？

这些问题想明白，比多写几个注解更值钱。

## 7. 推荐提交节奏

可以按下面的小步走：

1. 新增 `@Component`、`@Scope`
2. 新增 `BeanDefinition`
3. 完成包扫描，先打印扫描到的 class
4. 只注册带 `@Component` 的类
5. 创建 singleton Bean
6. 实现 `getBean`
7. 在 `test-app` 写两个简单测试类验证 singleton 和 prototype

每一步都尽量让代码能编译。这样你会比较有掌控感，不会写到一半不知道哪里坏了。

## 8. 一个小提醒

`test-app/pom.xml` 里依赖的 artifactId 现在写的是 `Mini-SpringBoot`，但真正的 core 模块 artifactId 是 `mini-spring-core`。后面你正式跑 `test-app` 时，可能需要把依赖改成：

```xml
<dependency>
    <groupId>com.jacolp</groupId>
    <artifactId>mini-spring-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

这个不影响你先规划和学习，但实现验证时要注意。

## 9. 第一阶段最终产物

完成后，`mini-spring-core` 应该至少拥有：

- 三个注解：`@Component`、`@ComponentScan`、`@Scope`
- 一个 Bean 元信息类：`BeanDefinition`
- 一个核心容器：`AnnotationConfigApplicationContext`
- 一个对象创建方法：`createBean`
- 一个获取 Bean 的入口：`getBean`
- 一个测试应用：`test-app` 里能演示容器启动和取 Bean

到这里，你就已经不是“照着 Spring 抄类名”了，而是真的把 IoC 容器的第一层骨架搭起来了。
