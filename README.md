# 思路

1. 自定义一个注解 @DataSource，将来可以将该注解加在 service 层方法或者类上面，表示方法或者类中的所有方法都使用某一个数据源。
2. 对于第一步，如果某个方法上面有 @DataSource 注解，那么就将该方法需要使用的数据源名称存入到 ThreadLocal。
3. 自定义切面，在切面中解析 @DataSource 注解，当一个方法或者类上面有 @DataSource 注解的时候，将 @DataSource 注解所标记的数据源存入到 ThreadLocal 中。
4. 最后，当 Mapper 执行的时候，需要 DataSource，他会自动去 AbstractRoutingDataSource 类中查找需要的数据源，我们只需要在 AbstractRoutingDataSource 中返回 ThreadLocal  中的值即可。



# 自定义注解和切面



## 注解

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

这里使用了`ThreadLocal`来保存需要哪种数据源

`MethodSignature signature = (MethodSignature) pjb.getSignature();`

`AnnotationUtils.findAnnotation`



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





# 动态获取配置

`@ConfigurationProperties(prefix = "spring.datasource")`

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



# ThreadLocal(DynmaicDataSourceContextHolder)

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



# 导入数据源

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



# 最后一步

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



# 测试

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



# 手动切换数据源

## aop

`@Order(10)`，后执行的aop会覆盖之前的数据源

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

