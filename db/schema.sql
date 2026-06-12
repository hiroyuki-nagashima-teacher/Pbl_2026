CREATE DATABASE IF NOT EXISTS pbl_2026 CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE pbl_2026;

DROP TABLE IF EXISTS sales_transaction;
DROP TABLE IF EXISTS product_master;
DROP TABLE IF EXISTS category_master;
DROP TABLE IF EXISTS account;

CREATE TABLE account (
    account_id INT AUTO_INCREMENT PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL,
    staff_name VARCHAR(50) NOT NULL,
    password_hash CHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_account_login_id (login_id),
    UNIQUE KEY uq_account_staff_name (staff_name),
    CHECK (role IN ('MANAGER', 'STAFF'))
);

CREATE TABLE category_master (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE product_master (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    on_sale BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    last_updated_account_id INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category_master(category_id),
    CONSTRAINT fk_product_last_account FOREIGN KEY (last_updated_account_id) REFERENCES account(account_id),
    CHECK (price >= 0)
);

CREATE TABLE sales_transaction (
    sales_id INT AUTO_INCREMENT PRIMARY KEY,
    sale_date DATE NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price INT NOT NULL,
    memo VARCHAR(500),
    registered_account_id INT NOT NULL,
    last_updated_account_id INT NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sales_product FOREIGN KEY (product_id) REFERENCES product_master(product_id),
    CONSTRAINT fk_sales_registered_account FOREIGN KEY (registered_account_id) REFERENCES account(account_id),
    CONSTRAINT fk_sales_last_account FOREIGN KEY (last_updated_account_id) REFERENCES account(account_id),
    CHECK (quantity >= 1),
    CHECK (unit_price >= 0)
);

INSERT INTO account (login_id, staff_name, password_hash, role) VALUES
('manager', '店長', SHA2('password', 256), 'MANAGER'),
('staff01', 'スタッフ01', SHA2('password', 256), 'STAFF');

INSERT INTO category_master (category_name, display_order) VALUES
('ドリンク', 1),
('フード', 2),
('お菓子', 3),
('その他', 4);
