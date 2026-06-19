package jp.co.pbl2026.sales.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jp.co.pbl2026.sales.model.Sale;

public final class MailUtil {
    private MailUtil() {}

    public static void sendSaleNotification(Sale sale, String staffName) {
        String to = "manager@example.com";
        String subject = "【売上登録通知】売上が新しく登録されました";
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String nowStr = LocalDateTime.now().format(formatter);
        
        String body = String.format(
            "登録日時: %s\n" +
            "登録スタッフ: %s\n" +
            "売上日: %s\n" +
            "商品名: %s\n" +
            "数量: %d\n" +
            "単価: %d 円\n" +
            "合計金額: %d 円\n" +
            "メモ: %s\n",
            nowStr,
            staffName,
            sale.getSaleDate(),
            sale.getProductName(),
            sale.getQuantity(),
            sale.getUnitPrice(),
            sale.getTotalAmount(),
            sale.getMemo() != null ? sale.getMemo() : ""
        );

        String rawMail = String.format(
            "========================================\n" +
            "送信日時: %s\n" +
            "宛先: %s\n" +
            "件名: %s\n" +
            "----------------------------------------\n" +
            "%s" +
            "========================================\n\n",
            nowStr, to, subject, body
        );

        // コンソール出力
        System.out.println("[模擬メール送信]");
        System.out.print(rawMail);

        // ファイル出力 (logs/mail.log)
        try {
            Path logDir = Paths.get("logs");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            Path logFile = logDir.resolve("mail.log");
            Files.writeString(logFile, rawMail, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("メールログの書き込みに失敗しました: " + e.getMessage());
        }
    }
}
