package com.th.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @program: dynamic_datasourece
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-22 15:55
 **/
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
