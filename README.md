# 思路

1. 自定义一个注解 @DataSource，将来可以将该注解加在 service 层方法或者类上面，表示方法或者类中的所有方法都使用某一个数据源。
2. 对于第一步，如果某个方法上面有 @DataSource 注解，那么就将该方法需要使用的数据源名称存入到 ThreadLocal。
3. 自定义切面，在切面中解析 @DataSource 注解，当一个方法或者类上面有 @DataSource 注解的时候，将 @DataSource 注解所标记的数据源存入到 ThreadLocal 中。
4. 最后，当 Mapper 执行的时候，需要 DataSource，他会自动去 AbstractRoutingDataSource 类中查找需要的数据源，我们只需要在 AbstractRoutingDataSource 中返回 ThreadLocal  中的值即可。



> 项目代码链接:https://github.com/1040580896/dynamic_datasourece



# 1. 预备知识

想要自定义动态数据源切换，得先了解一个类 `AbstractRoutingDataSource`：

`AbstractRoutingDataSource` 是在 Spring2.0.1 中引入的（注意是 Spring2.0.1 不是 Spring Boot2.0.1，所以这其实也算是 Spring 一个非常古老的特性了）, 该类充当了 DataSource 的路由中介，它能够在运行时, 根据某种 key 值来动态切换到真正的 DataSource 上。

大致的用法就是你提前准备好各种数据源，存入到一个 Map 中，Map 的 key 就是这个数据源的名字，Map 的 value 就是这个具体的数据源，然后再把这个 Map 配置到 `AbstractRoutingDataSource` 中，最后，每次执行数据库查询的时候，拿一个 key 出来，`AbstractRoutingDataSource` 会找到具体的数据源去执行这次数据库操作。



# 2、引入依赖

首先我们创建一个 Spring Boot 项目，引入 Web、MyBatis 以及 MySQL 依赖，项目创建成功之后，再手动加入 Druid 和 AOP 依赖，如下：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.2.9</version>
</dependency>
```







# 3、配置文件

```xml
# 数据源配置
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    ds:
      # 主库数据源
      master:
        url: jdbc:mysql://localhost:3306/test1?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8
        username: root
        password: th123456
        # 从库数据源
      slave:
        url: jdbc:mysql://localhost:3306/test2?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8
        username: root
        password: th123456
        # 初始连接数
    initialSize: 5
    # 最小连接池数量
    minIdle: 10
    # 最大连接池数量
    maxActive: 20
    # 配置获取连接等待超时的时间
    maxWait: 60000
    # 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
    timeBetweenEvictionRunsMillis: 60000
    # 配置一个连接在池中最小生存的时间，单位是毫秒
    minEvictableIdleTimeMillis: 300000
    # 配置一个连接在池中最大生存的时间，单位是毫秒
    maxEvictableIdleTimeMillis: 900000
    # 配置检测连接是否有效
    validationQuery: SELECT 1 FROM DUAL
    testWhileIdle: true
    testOnBorrow: false
    testOnReturn: false
    webStatFilter:
      enabled: true
    statViewServlet:
      enabled: true
      # 设置白名单，不填则允许所有访问
      allow:
      url-pattern: /druid/*
      # 控制台管理用户名和密码
      login-username: tienchin
      login-password: 123456
    filter:
      stat:
        enabled: true
        # 慢SQL记录
        log-slow-sql: true
        slow-sql-millis: 1000
        merge-sql: true
      wall:
        config:
          multi-statement-allow: tru
```



都是 Druid 的常规配置，也没啥好说的，唯一需要注意的是我们整个配置文件的格式。ds 里边配置我们的所有数据源，每个数据源都有一个名字，master 是默认数据源的名字，不可修改，其他数据源都可以自定义名字。最后面我们还配置了 Druid 的监控功能，如果小伙伴们还不懂 Druid 的监控功能，可以查看[Spring Boot 如何监控 SQL 运行情况？](https://mp.weixin.qq.com/s?__biz=MzI1NDY0MTkzNQ==&mid=2247496414&idx=1&sn=ba0303da578ade71b2d5fccf9c530809&scene=21#wechat_redirect)。



# 4、自定义注解和切面



## 注解

这个注解将来加在 Service 层的方法上，使用该注解的时候，需要指定一个数据源名称，不指定的话，默认就使用 master 作为数据源。

```java
/**
 * 将来可以加在某一个service 类上或者方法上，通过value属性来指定类或者方法应该使用哪一个数据源
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface DataSource {

    /**
     * 如果一个方法上加了 @DataSource 但是却没有指定数据源名称，那么默认使用 master 数据源
     * @return
     */
    String value() default DataSourceType.DEFAULT_DS_NAME;
}

```



## 切面

过 AOP 来解析当前的自定义注解

这里使用了`ThreadLocal`来保存需要哪种数据源

`MethodSignature signature = (MethodSignature) pjb.getSignature();`

`AnnotationUtils.findAnnotation`工具类



ThreadLocal 的特点，简单说就是在哪个线程中存入的数据，在哪个线程才能取出来，换一个线程就取不出来了，这样可以确保多线程环境下的数据安全。



```java
@Component
@Aspect
public class DataSourceAspect {

    /**
     * 切点
     *
     * @annotation(com.th.annotation.DataSource 表示方法上有 @DataSource 注解就将方法拦截下来
     * @within(com.th.annotation.DataSource) 表示如果类上面有 @DataSource 注解，就将类中的方法拦截下来
     */
    @Pointcut("@annotation(com.th.annotation.DataSource) || @within(com.th.annotation.DataSource)")
    public void pc() {

    }


    @Around("pc()")
    public Object around(ProceedingJoinPoint pjb) {
        //获取方法上面的注解
        DataSource dataSource = getDataSourece(pjb);
        if (dataSource != null) {
            //数据源的名称
            String value = dataSource.value();
            DynmaicDataSourceContextHolder.setDataSourceType(value);
        }
        try {
            return pjb.proceed();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            DynmaicDataSourceContextHolder.clearDataSourceType();
        }
        return null;
    }

    private DataSource getDataSourece(ProceedingJoinPoint pjb) {

        MethodSignature signature = (MethodSignature) pjb.getSignature();

        //查找方法上面的注解
        DataSource annotation = AnnotationUtils.findAnnotation(signature.getMethod(), DataSource.class);
        if (annotation != null) {
            //说明方法上面有 DataSource 注解
            return annotation;
        }
        //类上面找
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), DataSource.class);
    }
}

```



## 总结

1. 首先，我们在 dsPc() 方法上定义了切点，我们拦截下所有带有 `@DataSource` 注解的方法，同时由于该注解也可以加在类上，如果该注解加在类上，就表示类中的所有方法都使用该数据源。
2. 接下来我们定义了一个环绕通知，首先根据当前的切点，调用 getDataSource 方法获取到 `@DataSource` 注解，这个注解可能来自方法上也可能来自类上，方法上的优先级高于类上的优先级。如果拿到的注解不为空，则我们在 DynamicDataSourceContextHolder 中设置当前的数据源名称，设置完成后进行方法的调用；如果拿到的注解为空，那么就直接进行方法的调用，不再设置数据源了（将来会自动使用默认的数据源）。最后记得方法调用完成后，从 ThreadLocal 中移除数据源。



# 4、动态获取配置

`@ConfigurationProperties(prefix = "spring.datasource")`，注意结构，选择合适的类型存储

```java
package com.th.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.sql.DataSource;
import java.util.Map;

/**
 * @program: dynamic_datasourece
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-22 15:45
 **/
@ConfigurationProperties(prefix = "spring.datasource")
public class DruidProperties {

    private String type;
    private String driverClassName;
    private Map<String,Map<String,String>> ds;
    private Integer initialSize;
    private Integer minIdle;
    private Integer maxActive;
    private Integer maxWait;


    /**
     * 在外部构造好一个 DruidDataSource 对象，但是这个对象只包含三个核心属性  url，username，passowd
     * 在这个方法中，给这个对象设置公共属性
     * @param druidDataSource
     * @return
     */
    public DataSource dataSource(DruidDataSource druidDataSource){
        druidDataSource.setInitialSize(initialSize);
        druidDataSource.setMaxActive(maxActive);
        druidDataSource.setMinIdle(minIdle);
        druidDataSource.setMaxWait(maxWait);
      	//。。。。。
        return druidDataSource;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public Map<String, Map<String, String>> getDs() {
        return ds;
    }

    public void setDs(Map<String, Map<String, String>> ds) {
        this.ds = ds;
    }

    public Integer getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(Integer initialSize) {
        this.initialSize = initialSize;
    }

    public Integer getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(Integer minIdle) {
        this.minIdle = minIdle;
    }

    public Integer getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(Integer maxActive) {
        this.maxActive = maxActive;
    }

    public Integer getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(Integer maxWait) {
        this.maxWait = maxWait;
    }
}

```



# 5、存储数据源名称

对于当前数据库操作使用哪个数据源？我们有很多种不同的设置方案，当然最为省事的办法是把当前使用的数据源信息存入到 ThreadLocal 中，ThreadLocal 的特点，简单说就是在哪个线程中存入的数据，在哪个线程才能取出来，换一个线程就取不出来了，这样可以确保多线程环境下的数据安全。

ThreadLocal(DynmaicDataSourceContextHolder)

```java
/**
 * 这个类用来存储当前线程所使用的数据源名称
 */
public class DynmaicDataSourceContextHolder {

    private static ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void setDataSourceType(String dsType){
        CONTEXT_HOLDER.set(dsType);
    }

    public static String getDataSourceType(){
       return CONTEXT_HOLDER.get();
    }

    public static void clearDataSourceType(){
        CONTEXT_HOLDER.remove();
    }
}
```



# 6、加载数据源

@EnableConfigurationProperties(DruidProperties.class)

#### 先说作用：

@EnableConfigurationProperties注解的作用是：使使用 @ConfigurationProperties 注解的类生效。

#### 说明：

如果一个配置类只配置@ConfigurationProperties注解，而没有使用@Component，那么在IOC容器中是获取不到properties 配置文件转化的bean。说白了 @EnableConfigurationProperties 相当于把使用  @ConfigurationProperties 的类进行了一次注入。
测试发现 @ConfigurationProperties 与 @EnableConfigurationProperties 关系特别大



```java
@Component
@EnableConfigurationProperties(DruidProperties.class)
public class LoadDataSource {

    @Autowired
    DruidProperties druidProperties;

    public Map<String, DataSource> loadAllDataSource(){

        Map<String,DataSource> map = new HashMap<>();
        Map<String, Map<String, String>> ds = druidProperties.getDs();
        try {
            Set<String> keySet = ds.keySet();
            for (String key : keySet) {
                //druidProperties.dataSource((DruidDataSource) DruidDataSourceFactory.createDataSource(ds.get(key))) 公共属性也设置上去了
                map.put(key, druidProperties.dataSource((DruidDataSource) DruidDataSourceFactory.createDataSource(ds.get(key))));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }
}

```



# 7、数据源切换

这就是我们文章开头所说的 `AbstractRoutingDataSource` 了，该类有一个方法名为 determineCurrentLookupKey，当需要使用数据源的时候，系统会自动调用该方法，获取当前数据源的标记，如 master 或者 slave 或者其他，拿到标记之后，就可以据此获取到一个数据源了。

当我们配置 DynamicDataSource 的时候，需要配置两个关键的参数，**一个是 setTargetDataSources**，这个就是当前所有的数据源，把当前所有的数据源都告诉给 AbstractRoutingDataSource，这些数据源都是 key-value 的形式（将来根据 determineCurrentLookupKey 方法返回的 key 就可以获取到具体的数据源了）；**另一个方法是 setDefaultTargetDataSource**，这个就是默认的数据源，当我们执行一个数据库操作的时候，如果没有指定数据源（例如 Service 层的方法没有加 @DataSource 注解），那么默认就使用这个数据源。

最后，再将这个 bean 注册到 Spring 容器中，如下：

```java
@Component
public class DynmaicDataSource extends AbstractRoutingDataSource {

    public DynmaicDataSource(LoadDataSource loadDataSource) {

        //1、设置所有的数据源
        Map<String, DataSource> allDs = loadDataSource.loadAllDataSource();
        super.setTargetDataSources(new HashMap<>(allDs));
        //2、设置默认的数据源
        //将来，并不是所以方法上都有 @DataSourece注解 对于哪些没有 @DataSouce 注解的方法，该使用那个数据源？
        super.setDefaultTargetDataSource(allDs.get(DataSourceType.DEFAULT_DS_NAME));
        //3
        super.afterPropertiesSet();
    }

    /**
     * 用来返回数据源名称，当系统需要数据源的时候，会自动调用该方法获取数据源的名称
     * @return
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DynmaicDataSourceContextHolder.getDataSourceType();
    }
}

```



# 7、测试

好啦，大功告成，我们再来测试一下，写一个 UserMapper：

```java
@Mapper
public interface UserMapper {

    @Select("select * from user")
    List<User> getAllUsers();
}
```



通过 `@DataSource` 注解来指定具体操作的数据源，如果没有使用该注解指定，默认就使用 master 数据源。

最后去单元测试中测一下，如下：

再来一个 service：

```java
@Service
@DataSource("slave")
public class UserService {

    @Autowired
    UserMapper userMapper;


    //@DataSource("master")
    public List<User> getAllUsers(){
        return userMapper.getAllUsers();
    }

}
```



# 基于页面手动切换数据源

## aop

`@Order(10)`越小，后执行的aop会覆盖之前的数据源

```java
@Aspect
@Component
@Order(10)
public class GloalbDataSourceAspetc {

    @Autowired
    HttpSession session;

    @Pointcut("execution(* com.th.service.*.*(..))")
    public void pc(){
    }

    @Around("pc()")
    public Object around(ProceedingJoinPoint pjb){
        DynmaicDataSourceContextHolder.setDataSourceType((String) session.getAttribute(DataSourceType.DS_SESSION_KEY));
        try {
            return pjb.proceed();
        } catch (Throwable e) {
            e.printStackTrace();
        }finally {
            DynmaicDataSourceContextHolder.clearDataSourceType();
        }
        return null;
    }
}

```





`HttpSession session`,也可以用其他方式存储

```java
@RestController
public class DataSourceController {

    private static final Logger log = LoggerFactory.getLogger(DataSourceController.class);

    @Autowired
    UserService userService;

    /**
     * 修改数据源的接口
     */
    @PostMapping("/dstype")
    public void setDsType(String dsType, HttpSession session){

        //将数据源的信息存放到session
        session.setAttribute(DataSourceType.DS_SESSION_KEY,dsType);
        log.info("数据源切换为:{}",dsType);

    }
    @GetMapping("/users")
    public List<User> getAllUsers(){
        return userService.getAllUsers();
    }
}

```





```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
    <script src="jquery.js"></script>
</head>
<body>
<div>
    请选择数据源
    <select name="" id="" onchange="dfChange(this.options[this.options.selectedIndex].value)">
        <option value="请选择">请选择</option>
        <option value="master">master</option>
        <option value="slave">slave</option>
    </select>
</div>

<div id="result">

</div>

<button onclick="loadData()">加载数据</button>

<script>

    function loadData(){
        $.get("/users",function (data){
            $("#result").html(JSON.stringify(data))
        })
    }

    function dfChange(value){
        $.post("/dstype",{dsType:value})
    }
</script>
</body>
</html>
```

