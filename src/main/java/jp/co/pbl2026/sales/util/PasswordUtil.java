package jp.co.pbl2026.sales.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 【模範解答解説: パスワード暗号化ユーティリティ (PasswordUtil)】
 * パスワードを平文のまま保存せず、安全な暗号ハッシュ値に変換・照合するセキュリティ処理を担います。
 * 非機能要件「5.2 セキュリティ: パスワードは平文保存しない」を満たすための実装です。
 * 
 * ■ 設計・実装のポイント:
 * 1. SHA-256ハッシュアルゴリズムの採用:
 *    広く一般的に利用される一方向性ハッシュ関数である SHA-256 を用いて、元のパスワードを復元不可能なハッシュ文字列へ不可逆変換します。
 * 2. UTF-8によるバイト列変換:
 *    プラットフォーム（OS）依存による文字化けやハッシュ値の不一致を防ぐため、バイト列取得時に `StandardCharsets.UTF_8` を明示指定します。
 */
public final class PasswordUtil {

    /** インスタンス化を禁止するプライベートコンストラクタ */
    private PasswordUtil() {}

    /**
     * 入力された平文パスワードを SHA-256 でハッシュ化し、64文字の16進数文字列として返します。
     * 
     * @param plainPassword 平文パスワード
     * @return 64文字のハッシュ化文字列
     * @throws IllegalStateException 実行環境で SHA-256 アルゴリズムがサポートされていない場合
     */
    public static String hash(String plainPassword) {
        try {
            // SHA-256アルゴリズムのメッセージダイジェストを取得
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 文字列をバイト配列にしてハッシュ値を計算
            byte[] bytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            
            // バイト配列を16進数表記の文字列に変換（2桁のゼロ埋め16進数フォーマット %02x）
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            // Java標準でサポートされるべきアルゴリズムがない場合の安全弁（例外のラップ）
            throw new IllegalStateException("SHA-256 が利用できません。", e);
        }
    }

    /**
     * 入力された平文パスワードを再度ハッシュ化し、登録済みのハッシュ値と一致するか検証します。
     * ログイン認証処理で利用されます。
     * 
     * @param plainPassword 検証する平文パスワード
     * @param passwordHash データベース等に保存されているハッシュ値
     * @return 一致する場合は true、そうでない場合は false
     */
    public static boolean matches(String plainPassword, String passwordHash) {
        // 入力平文をハッシュ化した結果と、既存のハッシュ文字列が文字列一致するか判定
        return hash(plainPassword).equals(passwordHash);
    }
}
