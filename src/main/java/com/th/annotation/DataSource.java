package com.th.annotation;

import com.th.datasource.DataSourceType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @program: dynamic_datasourece
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-22 15:19
 **/

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
