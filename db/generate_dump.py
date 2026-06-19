import random
import datetime

# Real product names and modifiers lists
DRINK_BASE = ['コカ・コーラ', '三ツ矢サイダー', '伊右衛門', '綾鷹', 'お〜いお茶', '午後の紅茶', 'ポカリスエット', 'アクエリアス', 'モンスターエナジー', 'レッドブル', 'クラフトボス', 'ジョージア', 'エメラルドマウンテン', 'サントリー天然水', 'いろはす', 'カルピス', '十六茶', '黒烏龍茶', 'ヘルシア緑茶', 'ワンダ']
DRINK_MOD = ['500ml', '350ml', '1.5L', '無糖', 'レモン', 'グレープ', 'オレンジ', 'ゼロ', '微糖', 'ブラック', '特保', '濃い味', 'ピーチ', 'マイルド', 'ストレート', 'ミルク']

FOOD_BASE = ['牛丼', 'カツ丼', '親子丼', '中華丼', '天丼', 'ビーフカレー', 'カツカレー', 'チキン南蛮弁当', 'ハンバーグ弁当', '幕の内弁当', '唐揚げ弁当', '鮭弁当', '醤油ラーメン', '味噌ラーメン', '豚骨ラーメン', '塩ラーメン', 'かけうどん', 'きつねうどん', '天ぷらそば', 'ざるそば', 'カルボナーラ', 'ミートソースパスタ', 'マルゲリータピザ', '照り焼きチキンピザ', 'オムライス', '炒飯', '餃子定食']
FOOD_MOD = ['大盛', '特盛', '並盛', 'ミニ', 'トッピングチーズ', '温玉のせ', '辛口', '甘口', '中辛', 'ハーフ', 'セット', 'ダブル', 'デラックス', '特製']

SWEETS_BASE = ['ポテトチップス', 'じゃがりこ', 'たけのこの里', 'きのこの山', 'ポッキー', 'トッポ', 'コアラのマーチ', 'パイの実', 'ブラックサンダー', 'アルフォート', 'うまい棒', 'かっぱえびせん', 'ハッピーターン', 'チップスター', '明治ミルクチョコレート', 'ガーナチョコレート', 'チョコパイ', 'カントリーマアム', '堅あげポテト', '果汁グミ']
SWEETS_MOD = ['うすしお味', 'コンソメパンチ', 'のり塩味', 'サラダ味', 'チーズ味', 'じゃがバター味', 'いちご味', '抹茶味', 'ファミリーパック', 'ミニ', 'ビター', 'ホワイト', '贅沢仕立て', '瀬戸内レモン味']

OTHER_BASE = ['ボックスティッシュ', 'トイレットペーパー', '除菌ウェットティッシュ', 'ハンドソープ', 'シャンプー', 'コンディショナー', 'ボディソープ', '歯磨き粉', '歯ブラシ', 'マスク', '使い捨てカイロ', '消臭スプレー', '洗濯用洗剤', '柔軟剤', '食器用洗剤', '電池', 'ビニール傘', 'エコバッグ', 'メモ帳', 'ボールペン']
OTHER_MOD = ['5個パック', '12ロール', '80枚入り', '本体', '詰め替え用', '特大', '薬用', 'メントール配合', 'ふつう', 'かため', '50枚入り', 'レギュラーサイズ', '無香料', 'シトラスの香り', '単3形 4本パック', '単4形 4本パック', '黒 0.5mm', '赤 0.5mm']

def make_product_names():
    names = set()
    drink_names = []
    food_names = []
    sweets_names = []
    other_names = []
    
    # Drink 125
    while len(drink_names) < 125:
        name = f"{random.choice(DRINK_BASE)} {random.choice(DRINK_MOD)}"
        if name not in names:
            names.add(name)
            drink_names.append(name)
            
    # Food 125
    while len(food_names) < 125:
        name = f"{random.choice(FOOD_BASE)} {random.choice(FOOD_MOD)}"
        if name not in names:
            names.add(name)
            food_names.append(name)
            
    # Sweets 125
    while len(sweets_names) < 125:
        name = f"{random.choice(SWEETS_BASE)} {random.choice(SWEETS_MOD)}"
        if name not in names:
            names.add(name)
            sweets_names.append(name)
            
    # Other 125
    while len(other_names) < 125:
        name = f"{random.choice(OTHER_BASE)} {random.choice(OTHER_MOD)}"
        if name not in names:
            names.add(name)
            other_names.append(name)
            
    prods = []
    for n in drink_names:
        prods.append((1, n))
    for n in food_names:
        prods.append((2, n))
    for n in sweets_names:
        prods.append((3, n))
    for n in other_names:
        prods.append((4, n))
        
    random.shuffle(prods)
    return prods

def generate():
    sql_lines = []
    
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
    for i in range(1, 20):
        login_id = f"staff{i:02d}"
        staff_name = f"スタッフ{i:02d}"
        accounts.append(f"('{login_id}', '{staff_name}', SHA2('password', 256), 'STAFF')")
    
    sql_lines.append("INSERT INTO account (login_id, staff_name, password_hash, role) VALUES")
    sql_lines.append(",\n".join(accounts) + ";")
    sql_lines.append("")
    
    # 2. Generate Products (500 products with realistic names)
    sql_lines.append("-- Products")
    products_sql = []
    product_prices = {}
    
    generated_prods = make_product_names()
    
    for prod_id, (cat_id, prod_name) in enumerate(generated_prods, start=1):
        price = random.randint(5, 150) * 10 # 50 to 1500 yen
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
        
        if random.random() < 0.1:
            memo = f"'売上登録テストメモ {sale_id}'"
        else:
            memo = "NULL"
            
        reg_account = random.randint(1, 20)
        sales_sql.append(f"('{sale_date}', {prod_id}, {qty}, {unit_price}, {memo}, {reg_account}, {reg_account})")
        
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
    print("Successfully generated db/dump_large.sql with realistic product names")

if __name__ == "__main__":
    generate()
