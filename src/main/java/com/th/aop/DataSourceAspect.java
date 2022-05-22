package com.th.aop;

import com.th.annotation.DataSource;
import com.th.datasource.DynmaicDataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MemberSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

/**
 * @program: dynamic_datasourece
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-22 15:27
 **/
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
