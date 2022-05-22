package com.th.aop;

import com.th.datasource.DataSourceType;
import com.th.datasource.DynmaicDataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;

/**
 * @program: dynamic_datasourece
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-22 17:23
 **/
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
