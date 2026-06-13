# Multi-Mall POS System 🏬
[![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://github.com/Rudrasamadhiya/POSSystem) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

A complete Point of Sale (POS) billing system for malls and stores with multi-tenant support.

## Features ✨

### Multi-Mall Management
- Each mall has unique ID and credentials
- Separate dashboard per mall
- Independent product catalogs
- Mall-specific sales reports

### User Management
- **Admin** - Full access to dashboard, products, users, reports
- **Cashier** - Access to billing system only
- **Manager** - Access to reports and inventory
- Track active users

### Billing System
- Barcode scanning (keyboard/USB scanner)
- Real-time cart management
- Multiple payment methods (Cash, Card, UPI)
- Automatic inventory updates
- Receipt generation

### Reports & Analytics
- Daily/weekly/monthly sales reports
- Top-selling products
- Revenue tracking
- Transaction history
- Active users monitoring

## Installation 🚀

### 1. Install Python
Make sure Python 3.8+ is installed on your system.

### 2. Install Dependencies
```bash
cd C:\Users\suppo\OneDrive\Desktop\POSSystem
pip install -r requirements.txt
```

### 3. Run the Application
```bash
python app.py
```

The application will start at: **http://localhost:5000**

## Usage 📖

### First Time Setup

1. **Register a Mall**
   - Go to http://localhost:5000
   - Click "Register New Mall"
   - Fill in mall details (name, ID, password, location, contact)
   - Submit registration

2. **Login as Admin**
   - Click "Mall Admin Login"
   - Enter your Mall ID and password
   - Access the dashboard

3. **Add Products**
   - Go to "Products" section
   - Add products with barcode, name, price, stock
   - Products are now ready for billing

4. **Create Cashier Accounts**
   - Go to "Users" section
   - Create cashier/manager accounts
   - Provide username and password

5. **Start Billing**
   - Cashiers login via "Cashier Login"
   - Scan barcodes or enter manually
   - Add items to cart
   - Complete transaction

### Barcode Scanning

- Use USB barcode scanner (acts as keyboard input)
- Or manually type barcode and press Enter
- Product automatically added to cart

### Payment Methods

- **Cash** - Traditional cash payment
- **Card** - Credit/Debit card
- **UPI** - Digital payment (PhonePe, GPay, etc.)

## Database Structure 🗄️

The system uses SQLite database with the following tables:

- **malls** - Mall information
- **users** - Cashiers and managers
- **products** - Product catalog per mall
- **transactions** - Sales transactions
- **transaction_items** - Individual items in each transaction

## Security 🔒

- Password hashing using Werkzeug
- Session-based authentication
- Role-based access control
- Mall-specific data isolation

## Tech Stack 💻

- **Backend**: Flask (Python) - 80%
- **Database**: SQLite
- **Frontend**: HTML/CSS/JavaScript - 20%
- **Authentication**: Werkzeug Security

## File Structure 📁

```
POSSystem/
├── app.py                 # Main Flask application
├── requirements.txt       # Python dependencies
├── pos_system.db         # SQLite database (auto-created)
├── static/
│   ├── css/
│   │   └── style.css     # Styling
│   └── js/
│       └── billing.js    # Billing logic
└── templates/
    ├── index.html        # Home page
    ├── register.html     # Mall registration
    ├── login.html        # Admin login
    ├── user_login.html   # Cashier login
    ├── dashboard.html    # Admin dashboard
    ├── products.html     # Product management
    ├── users.html        # User management
    ├── billing.html      # Billing interface
    └── reports.html      # Sales reports
```

## Default Ports

- Application: **5000**
- Access from other devices: Use your computer's IP address (e.g., http://192.168.1.100:5000)

## Tips 💡

1. **Barcode Scanner**: Any USB barcode scanner works (no special drivers needed)
2. **Multiple Cashiers**: Each cashier can login simultaneously on different devices
3. **Stock Management**: Stock automatically decreases after each sale
4. **Reports**: View sales data by date range
5. **Backup**: Regularly backup `pos_system.db` file

## Troubleshooting 🔧

**Issue**: Port 5000 already in use
**Solution**: Change port in app.py: `app.run(debug=True, port=5001)`

**Issue**: Database locked
**Solution**: Close all connections and restart the app

**Issue**: Product not found
**Solution**: Verify barcode matches exactly in products table

## Future Enhancements 🚀

- [ ] Receipt printing (PDF/Thermal printer)
- [ ] Webcam barcode scanning
- [ ] Advanced analytics with charts
- [ ] Customer loyalty program
- [ ] Multi-language support
- [ ] Mobile app integration
- [ ] Cloud database support

## Support 📧

For issues or questions, check the code comments or modify as needed.

## License 📄

Free to use and modify for your business needs.

---

**Made with ❤️ for Malls and Stores**
