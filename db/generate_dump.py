import random
import datetime

def generate():
    sql_lines = []
    
    # Disable foreign key checks for truncation
    sql_lines.append("SET FOREIGN_KEY_CHECKS = 0;")
    sql_lines.append("TRUNCATE TABLE sales_transaction;")
    sql_lines.append("TRUNCATE TABLE product_master;")
    sql_lines.append("TRUNCATE TABLE account;")
    sql_lines.append("SET FOREIGN_KEY_CHECKS = 1;")
    sql_lines.append("")
    
    # 1. Generate Accounts (20 accounts)
    sql_lines.append("-- Accounts")
    accounts = []
    # manager
    accounts.append("('manager', '店長', SHA2('password', 256), 'MANAGER')")
    # staff01 to staff19
    for i in range(1, 20):
        login_id = f"staff{i:02d}"
        staff_name = f"スタッフ{i:02d}"
        accounts.append(f"('{login_id}', '{staff_name}', SHA2('password', 256), 'STAFF')")
    
    sql_lines.append("INSERT INTO account (login_id, staff_name, password_hash, role) VALUES")
    sql_lines.append(",\n".join(accounts) + ";")
    sql_lines.append("")
    
    # 2. Generate Products (500 products)
    sql_lines.append("-- Products")
    products_sql = []
    product_prices = {}
    categories = [1, 2, 3, 4] # ドリンク, フード, お菓子, その他
    
    for prod_id in range(1, 501):
        cat_id = random.choice(categories)
        prod_name = f"商品{prod_id:03d}"
        price = random.randint(5, 150) * 10 # 50円から1500円
        product_prices[prod_id] = price
        last_account = random.randint(1, 20)
        products_sql.append(f"({cat_id}, '{prod_name}', {price}, TRUE, {last_account})")
        
    sql_lines.append("INSERT INTO product_master (category_id, product_name, price, on_sale, last_updated_account_id) VALUES")
    sql_lines.append(",\n".join(products_sql) + ";")
    sql_lines.append("")
    
    # 3. Generate Sales (10,000 transactions)
    sql_lines.append("-- Sales Transactions")
    sales_sql = []
    start_date = datetime.date(2026, 1, 1)
    end_date = datetime.date(2026, 12, 31)
    days_between = (end_date - start_date).days
    
    for sale_id in range(1, 10001):
        random_days = random.randint(0, days_between)
        sale_date = start_date + datetime.timedelta(days=random_days)
        prod_id = random.randint(1, 500)
        qty = random.randint(1, 10)
        unit_price = product_prices[prod_id]
        
        # 10% chance to have a memo
        if random.random() < 0.1:
            memo = f"'売上登録テストメモ {sale_id}'"
        else:
            memo = "NULL"
            
        reg_account = random.randint(1, 20)
        sales_sql.append(f"('{sale_date}', {prod_id}, {qty}, {unit_price}, {memo}, {reg_account}, {reg_account})")
        
        # Batch insert by 1000 to keep SQL statements manageable
        if len(sales_sql) >= 1000:
            sql_lines.append("INSERT INTO sales_transaction (sale_date, product_id, quantity, unit_price, memo, registered_account_id, last_updated_account_id) VALUES")
            sql_lines.append(",\n".join(sales_sql) + ";")
            sql_lines.append("")
            sales_sql = []
            
    if sales_sql:
        sql_lines.append("INSERT INTO sales_transaction (sale_date, product_id, quantity, unit_price, memo, registered_account_id, last_updated_account_id) VALUES")
        sql_lines.append(",\n".join(sales_sql) + ";")
        sql_lines.append("")

    with open("db/dump_large.sql", "w", encoding="utf-8") as f:
        f.write("\n".join(sql_lines))
    print("Successfully generated db/dump_large.sql")

if __name__ == "__main__":
    generate()
