package jp.co.pbl2026.sales.dao;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * 【模範解答解説: データベース接続管理クラス (Db)】
 * 本クラスは、JNDI（Java Naming and Directory Interface）を利用してアプリケーションサーバー（Tomcat）
 * のコネクションプールからデータベース接続（Connection）を取得します。
 * 
 * ■ 設計・実装のポイント:
 * 1. コネクションプールによるパフォーマンス向上:
 *    リクエストのたびにDBとの接続を物理的に確立・切断すると高負荷になるため、Tomcat側であらかじめ接続をプールしておき、
 *    DataSourceを通じて接続を使い回す仕組みを導入しています。
 * 2. 接続情報の外部化（保守性向上）:
 *    DBの接続URL、ユーザー名、パスワードなどの情報は `context.xml` に外部化し、Javaソースコード内には持たせない設計としています。
 */
public final class Db {
    /** Tomcatの context.xml にて定義された JDBC JNDI リソース名 */
    private static final String JNDI_NAME = "java:comp/env/jdbc/Pbl2026DB";

    /**
     * インスタンス化を禁止するプライベートコンストラクタ。
     * 本クラスはユーティリティクラスであるため、外部から new されるのを防ぎます。
     */
    private Db() {}

    /**
     * データベースへの接続（Connection）を取得して返します。
     * 呼び出し元は、使用後に必ず `close()` を行う必要があります（try-with-resources 推奨）。
     * 
     * @return データベース接続オブジェクト
     * @throws SQLException データベース接続の取得に失敗した場合
     */
    public static Connection getConnection() throws SQLException {
        try {
            // InitialContext を生成し、JNDIツリーから指定のデータソースをルックアップします。
            InitialContext context = new InitialContext();
            DataSource dataSource = (DataSource) context.lookup(JNDI_NAME);
            // コネクションプールから空いているコネクションを1つ取得して返します。
            return dataSource.getConnection();
        } catch (NamingException e) {
            // NamingException を SQLException にラップして呼び出し元に通知し、例外ハンドリングを統一します。
            throw new SQLException("JNDI DataSource が見つかりません: " + JNDI_NAME, e);
        }
    }
}
