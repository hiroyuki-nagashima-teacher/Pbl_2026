<?php
try {
    $dsn = 'mysql:host=127.0.0.1;port=3310;dbname=pbl_2026;charset=utf8mb4';
    $user = 'root';
    $password = 'password';
    
    echo "Connecting to database...\n";
    $pdo = new PDO($dsn, $user, $password, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES utf8mb4"
    ]);
    
    echo "Reading dump file...\n";
    $sql = file_get_contents('db/dump_large.sql');
    
    echo "Executing SQL statements...\n";
    // Multi-query is allowed in PDO by default
    $pdo->exec($sql);
    
    echo "Successfully imported dump_large.sql\n";
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
    exit(1);
}
