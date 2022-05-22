package com.th.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: dynamic_datasourece
 * @description:
 * @author: xiaokaixin
 * @create: 2022-05-22 16:04
 **/
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
     * 这个方法用来返回数据源名称，当系统需要数据源的时候，会自动调用该方法获取数据源的名称
     * @return
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DynmaicDataSourceContextHolder.getDataSourceType();
    }
}
