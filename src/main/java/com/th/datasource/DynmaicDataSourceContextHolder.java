package com.th.datasource;

/**
 * @program: dynamic_datasourece
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-22 15:23
 **/

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
