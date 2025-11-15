from flask import Flask, render_template, request, redirect, url_for, session, jsonify, flash
from werkzeug.security import generate_password_hash, check_password_hash
from datetime import datetime
import sqlite3
import os

app = Flask(__name__)
app.secret_key = 'your-secret-key-change-in-production'

DATABASE = 'pos_system.db'

def get_db():
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    conn = get_db()
    conn.execute('''CREATE TABLE IF NOT EXISTS malls (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        mall_name TEXT NOT NULL,
        mall_id TEXT UNIQUE NOT NULL,
        password TEXT NOT NULL,
        location TEXT,
        contact TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )''')
    
    conn.execute('''CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        mall_id INTEGER,
        username TEXT NOT NULL,
        password TEXT NOT NULL,
        role TEXT NOT NULL,
        is_active INTEGER DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (mall_id) REFERENCES malls(id)
    )''')
    
    conn.execute('''CREATE TABLE IF NOT EXISTS products (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        mall_id INTEGER,
        barcode TEXT NOT NULL,
        name TEXT NOT NULL,
        price REAL NOT NULL,
        stock INTEGER DEFAULT 0,
        category TEXT,
        FOREIGN KEY (mall_id) REFERENCES malls(id)
    )''')
    
    conn.execute('''CREATE TABLE IF NOT EXISTS transactions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        mall_id INTEGER,
        user_id INTEGER,
        total_amount REAL,
        payment_method TEXT,
        customer_name TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (mall_id) REFERENCES malls(id),
        FOREIGN KEY (user_id) REFERENCES users(id)
    )''')
    
    conn.execute('''CREATE TABLE IF NOT EXISTS transaction_items (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        transaction_id INTEGER,
        product_id INTEGER,
        quantity INTEGER,
        price REAL,
        FOREIGN KEY (transaction_id) REFERENCES transactions(id),
        FOREIGN KEY (product_id) REFERENCES products(id)
    )''')
    
    conn.commit()
    conn.close()

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'POST':
        mall_name = request.form['mall_name']
        mall_id = request.form['mall_id']
        password = generate_password_hash(request.form['password'])
        location = request.form.get('location', '')
        contact = request.form.get('contact', '')
        
        conn = get_db()
        try:
            conn.execute('INSERT INTO malls (mall_name, mall_id, password, location, contact) VALUES (?, ?, ?, ?, ?)',
                        (mall_name, mall_id, password, location, contact))
            conn.commit()
            flash('Mall registered successfully!', 'success')
            return redirect(url_for('login'))
        except sqlite3.IntegrityError:
            flash('Mall ID already exists!', 'error')
        finally:
            conn.close()
    
    return render_template('register.html')

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        mall_id = request.form['mall_id']
        password = request.form['password']
        
        conn = get_db()
        mall = conn.execute('SELECT * FROM malls WHERE mall_id = ?', (mall_id,)).fetchone()
        conn.close()
        
        if mall and check_password_hash(mall['password'], password):
            session['mall_id'] = mall['id']
            session['mall_name'] = mall['mall_name']
            session['role'] = 'admin'
            return redirect(url_for('dashboard'))
        else:
            flash('Invalid credentials!', 'error')
    
    return render_template('login.html')

@app.route('/user-login', methods=['GET', 'POST'])
def user_login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']
        
        conn = get_db()
        user = conn.execute('SELECT * FROM users WHERE username = ? AND is_active = 1', (username,)).fetchone()
        conn.close()
        
        if user and check_password_hash(user['password'], password):
            session['user_id'] = user['id']
            session['mall_id'] = user['mall_id']
            session['role'] = user['role']
            
            conn = get_db()
            mall = conn.execute('SELECT mall_name FROM malls WHERE id = ?', (user['mall_id'],)).fetchone()
            session['mall_name'] = mall['mall_name']
            conn.close()
            
            return redirect(url_for('billing'))
        else:
            flash('Invalid credentials!', 'error')
    
    return render_template('user_login.html')

@app.route('/dashboard')
def dashboard():
    if 'mall_id' not in session:
        return redirect(url_for('login'))
    
    conn = get_db()
    
    # Get statistics
    total_sales = conn.execute('SELECT SUM(total_amount) as total FROM transactions WHERE mall_id = ?', 
                               (session['mall_id'],)).fetchone()['total'] or 0
    
    total_products = conn.execute('SELECT COUNT(*) as count FROM products WHERE mall_id = ?', 
                                  (session['mall_id'],)).fetchone()['count']
    
    active_users = conn.execute('SELECT COUNT(*) as count FROM users WHERE mall_id = ? AND is_active = 1', 
                                (session['mall_id'],)).fetchone()['count']
    
    today_sales = conn.execute('SELECT SUM(total_amount) as total FROM transactions WHERE mall_id = ? AND DATE(created_at) = DATE("now")', 
                               (session['mall_id'],)).fetchone()['total'] or 0
    
    recent_transactions = conn.execute('SELECT * FROM transactions WHERE mall_id = ? ORDER BY created_at DESC LIMIT 10', 
                                       (session['mall_id'],)).fetchall()
    
    conn.close()
    
    return render_template('dashboard.html', 
                          total_sales=total_sales,
                          total_products=total_products,
                          active_users=active_users,
                          today_sales=today_sales,
                          recent_transactions=recent_transactions)

@app.route('/products', methods=['GET', 'POST'])
def products():
    if 'mall_id' not in session:
        return redirect(url_for('login'))
    
    conn = get_db()
    
    if request.method == 'POST':
        barcode = request.form['barcode']
        name = request.form['name']
        price = float(request.form['price'])
        stock = int(request.form['stock'])
        category = request.form.get('category', '')
        
        conn.execute('INSERT INTO products (mall_id, barcode, name, price, stock, category) VALUES (?, ?, ?, ?, ?, ?)',
                    (session['mall_id'], barcode, name, price, stock, category))
        conn.commit()
        flash('Product added successfully!', 'success')
    
    products = conn.execute('SELECT * FROM products WHERE mall_id = ?', (session['mall_id'],)).fetchall()
    conn.close()
    
    return render_template('products.html', products=products)

@app.route('/users', methods=['GET', 'POST'])
def users():
    if 'mall_id' not in session or session.get('role') != 'admin':
        return redirect(url_for('dashboard'))
    
    conn = get_db()
    
    if request.method == 'POST':
        username = request.form['username']
        password = generate_password_hash(request.form['password'])
        role = request.form['role']
        
        conn.execute('INSERT INTO users (mall_id, username, password, role) VALUES (?, ?, ?, ?)',
                    (session['mall_id'], username, password, role))
        conn.commit()
        flash('User created successfully!', 'success')
    
    users = conn.execute('SELECT * FROM users WHERE mall_id = ?', (session['mall_id'],)).fetchall()
    conn.close()
    
    return render_template('users.html', users=users)

@app.route('/billing')
def billing():
    if 'mall_id' not in session:
        return redirect(url_for('user_login'))
    
    return render_template('billing.html')

@app.route('/api/scan-product/<barcode>')
def scan_product(barcode):
    if 'mall_id' not in session:
        return jsonify({'error': 'Unauthorized'}), 401
    
    conn = get_db()
    product = conn.execute('SELECT * FROM products WHERE mall_id = ? AND barcode = ?', 
                          (session['mall_id'], barcode)).fetchone()
    conn.close()
    
    if product:
        return jsonify({
            'id': product['id'],
            'name': product['name'],
            'price': product['price'],
            'stock': product['stock']
        })
    else:
        return jsonify({'error': 'Product not found'}), 404

@app.route('/api/complete-transaction', methods=['POST'])
def complete_transaction():
    if 'mall_id' not in session:
        return jsonify({'error': 'Unauthorized'}), 401
    
    data = request.json
    items = data['items']
    total = data['total']
    payment_method = data['payment_method']
    customer_name = data.get('customer_name', '')
    
    conn = get_db()
    cursor = conn.cursor()
    
    user_id = session.get('user_id', None)
    cursor.execute('INSERT INTO transactions (mall_id, user_id, total_amount, payment_method, customer_name) VALUES (?, ?, ?, ?, ?)',
                  (session['mall_id'], user_id, total, payment_method, customer_name))
    transaction_id = cursor.lastrowid
    
    for item in items:
        cursor.execute('INSERT INTO transaction_items (transaction_id, product_id, quantity, price) VALUES (?, ?, ?, ?)',
                      (transaction_id, item['id'], item['quantity'], item['price']))
        cursor.execute('UPDATE products SET stock = stock - ? WHERE id = ?', (item['quantity'], item['id']))
    
    conn.commit()
    conn.close()
    
    return jsonify({'success': True, 'transaction_id': transaction_id})

@app.route('/reports')
def reports():
    if 'mall_id' not in session:
        return redirect(url_for('login'))
    
    conn = get_db()
    
    # Daily sales
    daily_sales = conn.execute('''SELECT DATE(created_at) as date, SUM(total_amount) as total 
                                  FROM transactions WHERE mall_id = ? 
                                  GROUP BY DATE(created_at) ORDER BY date DESC LIMIT 30''', 
                               (session['mall_id'],)).fetchall()
    
    # Top products
    top_products = conn.execute('''SELECT p.name, SUM(ti.quantity) as sold 
                                   FROM transaction_items ti 
                                   JOIN products p ON ti.product_id = p.id 
                                   JOIN transactions t ON ti.transaction_id = t.id
                                   WHERE t.mall_id = ? 
                                   GROUP BY p.id ORDER BY sold DESC LIMIT 10''', 
                                (session['mall_id'],)).fetchall()
    
    conn.close()
    
    return render_template('reports.html', daily_sales=daily_sales, top_products=top_products)

@app.route('/logout')
def logout():
    session.clear()
    return redirect(url_for('index'))

if __name__ == '__main__':
    init_db()
    port = int(os.environ.get('PORT', 5000))
    app.run(debug=False, host='0.0.0.0', port=port)
