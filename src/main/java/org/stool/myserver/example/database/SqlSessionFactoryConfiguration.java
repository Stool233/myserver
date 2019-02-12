package org.stool.myserver.example.database;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;

public class SqlSessionFactoryConfiguration {


    private static class SqlSessionFactoryHide {
        static SqlSessionFactory sqlSessionFactory;

        static {
            DataSource dataSource = new PooledDataSource("com.mysql.jdbc.Driver",
                    "jdbc:mysql://localhost:3306/db",
                    "root",
                    "123456");
            Environment environment = new Environment("development", new JdbcTransactionFactory(), dataSource);
            Configuration configuration = new Configuration(environment);
            configuration.addMapper(BlogMapper.class);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        }
    }

    public static SqlSessionFactory sqlSessionFactory() {
        return SqlSessionFactoryHide.sqlSessionFactory;
    }

}
