package jp.co.pbl2026.sales.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 【模範解答解説: バリデーション＆パースユーティリティ (ValidationUtil)】
 * 画面からの入力データに対する共通チェック（空文字処理、数値・日付型パース、バリデーションエラー管理）を提供します。
 * 
 * ■ 設計・実装のポイント:
 * 1. エラーマップの順序性保持:
 *    `LinkedHashMap` を使用してエラー情報を格納・返却します。これにより、画面にエラーメッセージを出力する際、
 *    入力フォームのレイアウト順（上から下）と同じ順番でエラーメッセージを表示することができ、UXが向上します。
 * 2. 入力トリム処理の自動化:
 *    全角半角の空白や入力値 NULL の揺らぎによるチェックエラーを防ぐため、文字列トリムを安全に行う `trim()` を定義しています。
 * 3. 例外キャッチによる安全なデータパース:
 *    文字列から `Integer` や `LocalDate` へのキャスト中に発生しうる例外（`NumberFormatException` や `DateTimeParseException`）
 *    をキャッチし、自動的にエラーマップへメッセージを格納して `null` を返却するエラーハンドリングを抽象化しています。
 */
public final class ValidationUtil {

    /** インスタンス化を禁止するプライベートコンストラクタ */
    private ValidationUtil() {}

    /**
     * バリデーションエラーを保持するための新しいマップを生成して返します。
     * キーには入力フィールド名（例: "loginId"）、値にはエラー文言（例: "ログインIDは必須です。"）を保持します。
     * 
     * @return 挿入順序を維持する LinkedHashMap
     */
    public static Map<String, String> errors() {
        // 画面上の表示順と入力順を統一するため、通常の HashMap ではなく LinkedHashMap を返します。
        return new LinkedHashMap<>();
    }

    /**
     * 文字列の前後の空白文字を除去します。値が null の場合は空文字を返します。
     * 
     * @param value 処理対象の文字列
     * @return トリム済みの文字列、または空文字列
     */
    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 文字列を数値（Integer）にパースします。
     * フォーマットエラーがある場合はエラーマップにメッセージを追加し、null を返します。
     * 
     * @param value パース対象の文字列
     * @param fieldName エラー時にマップへ登録するキー名（フィールド名）
     * @param errors エラー情報を書き込むマップ
     * @return パースに成功した場合は Integer 値、入力が空またはエラー時は null
     */
    public static Integer parseInteger(String value, String fieldName, Map<String, String> errors) {
        String text = trim(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            // パース例外をキャッチして、ユーザーにわかりやすいメッセージをエラーマップへ挿入
            errors.put(fieldName, "整数で入力してください。");
            return null;
        }
    }

    /**
     * 文字列を日付（LocalDate）にパースします。
     * 日付フォーマット（yyyy-MM-dd）に反する場合はエラーマップにメッセージを追加し、null を返します。
     * 
     * @param value パース対象の文字列
     * @param fieldName エラー時にマップへ登録するキー名（フィールド名）
     * @param errors エラー情報を書き込むマップ
     * @return パースに成功した場合は LocalDate オブジェクト、入力が空またはエラー時は null
     */
    public static LocalDate parseDate(String value, String fieldName, Map<String, String> errors) {
        String text = trim(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            // 日付解析エラー時の共通例外ハンドリング
            errors.put(fieldName, "日付形式で入力してください。");
            return null;
        }
    }
}
