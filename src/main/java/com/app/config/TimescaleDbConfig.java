package com.app.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.app.repository.TimescaleMapper;

@Configuration
@MapperScan(
        basePackages = "com.app.repository.timeseries",
        annotationClass = TimescaleMapper.class,
        sqlSessionTemplateRef = "timescaleSqlSessionTemplate"
)
public class TimescaleDbConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.timescale")
    public DataSourceProperties timescaleDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource timescaleDataSource() {
        return timescaleDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public SqlSessionFactory timescaleSqlSessionFactory(
            @Qualifier("timescaleDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.app.entity,com.app.entity.timeseries");
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/timeseries/*.xml")
        );
        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate timescaleSqlSessionTemplate(
            @Qualifier("timescaleSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public DataSourceTransactionManager timescaleTransactionManager(
            @Qualifier("timescaleDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}