package jp.co.pbl2026.sales.dao;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public final class Db {
    private static final String JNDI_NAME = "java:comp/env/jdbc/Pbl2026DB";

    private Db() {}

    public static Connection getConnection() throws SQLException {
        try {
            // DB接続情報はTomcatのJNDI Resourceに集約し、アプリ側はDataSource名だけを参照する。
            InitialContext context = new InitialContext();
            DataSource dataSource = (DataSource) context.lookup(JNDI_NAME);
            return dataSource.getConnection();
        } catch (NamingException e) {
            throw new SQLException("JNDI DataSource が見つかりません: " + JNDI_NAME, e);
        }
    }
}
