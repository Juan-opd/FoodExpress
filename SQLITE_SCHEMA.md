# Esquema SQLite de FoodExpress

```sql
CREATE TABLE users (email TEXT PRIMARY KEY, name TEXT NOT NULL, password_hash TEXT NOT NULL, created_at INTEGER NOT NULL);
CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE);
CREATE TABLE products (id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL, price REAL NOT NULL, emoji TEXT NOT NULL, category TEXT NOT NULL, rating REAL NOT NULL, delivery_time TEXT NOT NULL, popular INTEGER NOT NULL);
CREATE TABLE favorites (user_email TEXT NOT NULL, product_id TEXT NOT NULL, PRIMARY KEY(user_email, product_id));
CREATE TABLE cart_items (user_email TEXT NOT NULL, product_id TEXT NOT NULL, quantity INTEGER NOT NULL, PRIMARY KEY(user_email, product_id));
CREATE TABLE orders (number TEXT PRIMARY KEY, user_email TEXT NOT NULL, subtotal REAL NOT NULL, delivery REAL NOT NULL, discount REAL NOT NULL, total REAL NOT NULL, address TEXT NOT NULL, payment TEXT NOT NULL, date TEXT NOT NULL);
CREATE TABLE order_items (id INTEGER PRIMARY KEY AUTOINCREMENT, order_number TEXT NOT NULL, name TEXT NOT NULL, quantity INTEGER NOT NULL, price REAL NOT NULL, emoji TEXT NOT NULL);
```
