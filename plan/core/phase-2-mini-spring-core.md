# mini-spring-core 第二阶段计划：把能跑的容器整理成更像容器的结构

第一阶段验收结论：通过。

你现在已经完成了一个最小 IoC 闭环：

- 配置类能启动容器
- 能扫描 class 文件
- 能识别 `@Component`
- 能注册 `BeanDefinition`
- 能通过 `getBean("userService")` 拿到 Bean
- singleton 多次获取是同一个对象
- Maven 编译和测试能通过
- 手动运行 `AppConfig` 能正常输出 `UserService`

第二阶段不要急着照搬 Spring 的完整继承体系。更适合你的路线是：先从当前代码里真实存在的问题出发，问“为什么这里需要拆分职责”，再慢慢长出接口和设计模式。

## 1. 第二阶段核心目标

第二阶段的目标是把第一阶段“能跑”的容器，整理成“更稳定、边界更清楚、方便继续扩展”的容器。

重点不是上来就实现完整 Spring，而是解决这些问题：

- `@ComponentScan` 的扫描路径要真正生效
- 扫描、注册、创建 Bean 的职责要逐步分清
- Bean 名称、Scope、异常提示要更可靠
- singleton/prototype 要有可验证测试
- 为第三阶段的依赖注入 `@Autowired` 留出位置

## 2. 第一阶段保留到第二阶段纠正的问题

### 问题一：`@ComponentScan("target")` 是一个危险信号

现在 `AppConfig` 写的是：

```java
@ComponentScan("target")
```

但从语义上看，用户应该写的是业务包名：

```java
@ComponentScan("com.jacolp")
```

当前容器里虽然计算了：

```java
String packagePath = path.replace(".", File.separator);
```

但后面没有使用它，而是直接从 classpath 根目录开始扫描。这样会导致：

- `@ComponentScan` 的 value 没有真正控制扫描范围
- 容器可能扫描到不该扫描的 class
- 用户必须知道编译输出目录结构，这不符合注解语义

第二阶段建议先修这个点。

你可以思考：

- 用户写的是包名，为什么框架内部要转成资源路径？
- `com.jacolp` 和 `com/jacolp` 分别属于哪个世界？
- ClassLoader 查找资源时要的是什么格式？

### 问题二：扫描 class 时依赖 `"classes"` 字符串太脆

现在类名解析依赖：

```java
absolutePath.substring(absolutePath.indexOf("classes") + 8, absolutePath.indexOf(".class"))
```

这个在当前 Maven 输出目录下能跑，但它隐含了很多假设：

- 编译目录一定叫 `classes`
- 路径中不会提前出现另一个 `classes`
- 路径没有空格、中文 URL 编码、jar 包等情况
- 一定是文件系统目录，不是 jar 内资源

第二阶段不用立刻支持 jar 扫描，但至少应该让“从扫描根目录推导类全名”这件事更清楚。

推荐思路：

1. 先通过 `ClassLoader.getResource(packagePath)` 找到扫描包对应目录
2. 扫描时保留当前包名
3. 每进入一个子目录，就把子包名拼进去
4. 遇到 `.class` 文件时，用包名 + 类名得到全限定类名

这样你就不需要从绝对路径里硬切字符串。

### 问题三：`@Component("xxx")` 的 value 还没有生效

你已经给 `@Component` 设计了：

```java
String value() default "";
```

但注册 Bean 时只用了类名首字母小写：

```java
String beanName = beanClass.getSimpleName();
```

第二阶段可以把 beanName 规则补完整：

- 如果 `@Component("xxx")` 不为空，用 `xxx`
- 如果为空，使用默认 beanName

这一步的意义不是“Spring 也这么做”，而是让框架使用者可以自己命名 Bean。

### 问题四：重复 BeanName 不能悄悄忽略

现在注册时使用：

```java
beanDefinitionMap.putIfAbsent(beanName, beanDefinition);
```

如果两个类得到同一个 beanName，后者会被静默忽略。学习阶段建议改成主动抛异常。

因为容器启动时最怕“看起来正常，其实有 Bean 没注册进去”。

你可以设计一个异常：

```java
DuplicateBeanDefinitionException
```

提示哪个 beanName 冲突了。

### 问题五：无参构造不存在时会空指针

`BeanFactory.createBean` 里如果找不到 public 无参构造器，`usedConstructor` 会是 `null`，后面调用：

```java
usedConstructor.newInstance();
```

会变成空指针。

第二阶段建议改成清晰异常：

```java
No default constructor found for bean class: xxx
```

这一步是在为第三阶段构造器注入做铺垫。现在先只支持无参构造没问题，但要把“不支持”的边界说清楚。

### 问题六：singleton 现在是懒加载，不是启动时预实例化

现在 singleton Bean 是第一次 `getBean` 时才创建。这个行为可以接受，但要明确命名：

- 如果你想第一阶段更简单，可以继续懒加载
- 如果你想更接近默认 Spring 行为，可以在容器启动后预实例化所有 singleton

第二阶段建议你先实现一个方法：

```java
preInstantiateSingletons();
```

容器构造流程变成：

1. 扫描
2. 注册 BeanDefinition
3. 创建非懒加载 singleton

这样你会更清楚 Spring 为什么要把“注册定义”和“创建对象”分成两个阶段。

### 问题七：调试输出不要留在核心容器

现在容器启动时会打印：

```java
System.out.println(beanDefinitionMap);
```

学习阶段打印没问题，但核心框架类最好不要随便输出。

第二阶段建议：

- 要么删除
- 要么临时放到 `test-app`
- 暂时不引入日志框架

### 问题八：测试还没有真正验收容器行为

当前 `mvn test` 能通过，但测试还是模板里的：

```java
assertTrue(true);
```

第二阶段至少补三个测试：

- singleton 多次获取是同一个对象
- prototype 多次获取是不同对象
- 找不到 beanName 会抛异常

测试不只是为了覆盖率，而是帮你固定住“容器应该有什么行为”。

## 3. 第二阶段建议新增能力

### 3.1 支持真正的包扫描

目标：

```java
@ComponentScan("com.jacolp")
```

能只扫描 `com.jacolp` 包及其子包。

建议拆出一个方法：

```java
private void scan(String basePackage)
```

后续如果方法变长，再考虑拆成独立类：

```java
ClassPathBeanDefinitionScanner
```

不要一开始就拆。先在一个方法里写清楚，等你感到“扫描逻辑已经挤占容器启动主流程”时，再拆出去。设计模式应该从不舒服的地方长出来。

### 3.2 支持自定义 beanName

目标：

```java
@Component("userService")
public class UserService {
}
```

能用 `userService` 注册。

建议加一个方法：

```java
private String resolveBeanName(Class<?> beanClass)
```

这样主流程会更干净：

```java
String beanName = resolveBeanName(beanClass);
BeanDefinition beanDefinition = resolveBeanDefinition(beanClass);
registerBeanDefinition(beanName, beanDefinition);
```

这不是为了炫技，而是因为“命名规则”和“注册动作”是两个不同问题。

### 3.3 支持启动时创建 singleton

目标：

```java
private void preInstantiateSingletons()
```

伪代码：

```java
for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
    if ("singleton".equals(entry.getValue().getScope())) {
        Object bean = createBean(entry.getValue());
        singletonObjects.put(entry.getKey(), bean);
    }
}
```

然后 `getBean` 里 singleton 分支只负责从缓存拿。

你可以对比两种设计：

- 懒加载：第一次 getBean 才创建
- 预实例化：容器启动时就创建

Spring 默认倾向预实例化 singleton，是因为启动时尽早暴露配置错误。你的小框架可以先支持预实例化，后面再加 `@Lazy`。

### 3.4 整理包结构，但不要为了“像 Spring”而整理

当前包结构能用，但语义有点混：

- `Component`、`Scope` 放在 `beans` 下不太直观
- `BeanFactory` 现在是一个静态工具类，还不是真正意义上的工厂接口
- `AnnotationConfigApplicationContext` 同时负责扫描、注册、创建、获取 Bean

第二阶段可以先轻微整理：

```text
com.jacolp
├── annotations
│   ├── Component.java
│   ├── ComponentScan.java
│   └── Scope.java
├── beans
│   ├── BeanDefinition.java
│   └── BeanFactory.java
├── context
│   └── AnnotationConfigApplicationContext.java
├── constant
│   └── BeanScopeConstant.java
└── exception
```

如果你不想马上大规模移动，也可以先保持现状，只做方法级拆分。

判断标准：

- 如果只是为了目录好看，不急着改
- 如果一个类越来越长，读起来已经看不清主流程，就该拆
- 如果某段逻辑未来会被复用，比如扫描器，就适合拆

## 4. 不建议第二阶段马上做的事

这些继续往后放：

- `@Autowired`
- 构造器注入
- 循环依赖
- BeanPostProcessor
- AOP
- `@Configuration` / `@Bean`
- 类型转换
- FactoryBean

第二阶段先把地基修平。第三阶段再加依赖注入会舒服很多。

## 5. 第二阶段验收标准

完成后应该满足：

- `@ComponentScan("com.jacolp")` 能生效
- 不会扫描整个 classpath 根目录
- `@Component("自定义名字")` 能注册指定 beanName
- 重复 beanName 会抛明确异常
- 没有无参构造器会抛明确异常
- singleton 可以在容器启动阶段创建
- prototype 每次 `getBean` 都是新对象
- 核心容器不再直接 `System.out.println`
- 至少有 3 个真实测试覆盖 singleton、prototype、异常场景

## 6. 建议实现顺序

1. 先修 `@ComponentScan("com.jacolp")` 真正生效
2. 把类名解析从“绝对路径截字符串”改成“扫描时携带包名”
3. 实现 `resolveBeanName`
4. 实现重复 beanName 异常
5. 改造 `BeanFactory.createBean` 的异常提示
6. 增加 `preInstantiateSingletons`
7. 删除核心容器里的调试打印
8. 在 `test-app` 或 core 测试里补真实测试

每一步都尽量保持可运行。你现在已经有主干了，第二阶段最重要的是把主干修直。

## 7. 你应该重点理解的设计问题

第二阶段写代码时，建议反复问自己：

- 为什么要先注册 BeanDefinition，再创建 Bean？
- 扫描器为什么不应该关心单例缓存？
- BeanFactory 为什么应该关心“如何创建对象”，而不是“从哪里扫描对象”？
- ApplicationContext 为什么像一个总控入口？
- 用户传入的是包名，框架内部为什么要转换成资源路径？
- 异常是在启动时暴露好，还是等运行时出问题再暴露好？

这些问题想明白后，你再去看 Spring 的继承体系，会更容易理解它为什么长成那样，而不是只记住一堆类名。

## 8. 第二阶段推荐完成后的状态

完成第二阶段后，你的容器最好能形成这个流程：

```text
AnnotationConfigApplicationContext
    -> 读取配置类
    -> 解析 ComponentScan
    -> 扫描指定包
    -> 生成 BeanDefinition
    -> 注册 BeanDefinition
    -> 预实例化 singleton
    -> 对外提供 getBean
```

到这里，你就可以进入第三阶段：依赖注入。

第三阶段会开始出现真正有意思的问题：一个 Bean 创建时依赖另一个 Bean，容器应该怎么找、什么时候注入、字段注入和构造器注入有什么区别。那时候再引入更多设计，会自然很多。
