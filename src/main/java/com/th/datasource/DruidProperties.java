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
