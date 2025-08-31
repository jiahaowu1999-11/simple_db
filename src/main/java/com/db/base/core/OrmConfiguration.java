package com.db.base.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ORM框架配置类，用于创建数据源和SqlSession
 */
public class OrmConfiguration {
    private DataSource dataSource;
    
    public OrmConfiguration(Properties props) {
        // 使用HikariCP作为数据源
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("jdbc.url"));
        config.setUsername(props.getProperty("jdbc.username"));
        config.setPassword(props.getProperty("jdbc.password"));
        config.setDriverClassName(props.getProperty("jdbc.driver"));
        
        // 可以设置其他连接池参数
        this.dataSource = new HikariDataSource(config);
    }
    
    public SqlSession openSession() {
        return new SqlSession(dataSource);
    }

    public void close(){
        try {
            this.dataSource.getConnection().close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
    