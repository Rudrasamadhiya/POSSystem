let cart = [];
let codeReader = null;
let stream = null;

document.getElementById('barcodeInput').addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        scanProduct();
    }
});

async function scanProduct() {
    const barcode = document.getElementById('barcodeInput').value.trim();
    
    if (!barcode) {
        showNotification('Please enter a barcode', 'error');
        return;
    }
    
    const input = document.getElementById('barcodeInput');
    input.style.borderColor = '#667eea';
    
    try {
        const response = await fetch(`/api/scan-product/${barcode}`);
        const data = await response.json();
        
        if (response.ok) {
            input.style.borderColor = '#10b981';
            setTimeout(() => input.style.borderColor = '', 500);
            addToCart(data);
            document.getElementById('barcodeInput').value = '';
            document.getElementById('barcodeInput').focus();
        } else {
            input.style.borderColor = '#ef4444';
            setTimeout(() => input.style.borderColor = '', 500);
            showNotification(data.error || 'Product not found', 'error');
        }
    } catch (error) {
        showNotification('Error scanning product', 'error');
    }
}

function addToCart(product) {
    const existingItem = cart.find(item => item.id === product.id);
    
    if (existingItem) {
        if (existingItem.quantity < product.stock) {
            existingItem.quantity++;
        } else {
            showNotification('Not enough stock', 'error');
            return;
        }
    } else {
        cart.push({
            id: product.id,
            name: product.name,
            price: product.price,
            quantity: 1,
            stock: product.stock
        });
    }
    
    showNotification('✓ Added to cart', 'success');
    updateCart();
}

function updateCart() {
    const cartItems = document.getElementById('cartItems');
    cartItems.innerHTML = '';
    
    let total = 0;
    
    cart.forEach((item, index) => {
        const itemTotal = item.price * item.quantity;
        total += itemTotal;
        
        const cartItem = document.createElement('div');
        cartItem.className = 'cart-item';
        cartItem.innerHTML = `
            <div class="cart-item-info">
                <div class="cart-item-name">${item.name}</div>
                <div class="cart-item-price">₹${item.price.toFixed(2)} x ${item.quantity} = ₹${itemTotal.toFixed(2)}</div>
            </div>
            <div class="cart-item-actions">
                <button onclick="decreaseQuantity(${index})">-</button>
                <span>${item.quantity}</span>
                <button onclick="increaseQuantity(${index})">+</button>
                <button onclick="removeItem(${index})" style="background: #ef4444;">Remove</button>
            </div>
        `;
        cartItems.appendChild(cartItem);
    });
    
    document.getElementById('totalAmount').textContent = total.toFixed(2);
}

function increaseQuantity(index) {
    if (cart[index].quantity < cart[index].stock) {
        cart[index].quantity++;
        updateCart();
    } else {
        showNotification('Not enough stock', 'error');
    }
}

function decreaseQuantity(index) {
    if (cart[index].quantity > 1) {
        cart[index].quantity--;
        updateCart();
    }
}

function removeItem(index) {
    cart.splice(index, 1);
    updateCart();
}

function clearCart() {
    if (confirm('Clear all items from cart?')) {
        cart = [];
        updateCart();
    }
}

async function completeBilling() {
    if (cart.length === 0) {
        showNotification('Cart is empty', 'error');
        return;
    }
    
    const customerName = document.getElementById('customerName').value;
    const paymentMethod = document.getElementById('paymentMethod').value;
    const total = parseFloat(document.getElementById('totalAmount').textContent);
    
    const transactionData = {
        items: cart,
        total: total,
        payment_method: paymentMethod,
        customer_name: customerName
    };
    
    showNotification('Processing payment...', 'info');
    
    try {
        const response = await fetch('/api/complete-transaction', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(transactionData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification(`✓ Transaction completed! ID: ${data.transaction_id}`, 'success');
            celebrateTransaction();
            cart = [];
            updateCart();
            document.getElementById('customerName').value = '';
        } else {
            showNotification('Transaction failed', 'error');
        }
    } catch (error) {
        showNotification('Error completing transaction', 'error');
    }
}

function showNotification(message, type) {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    document.body.appendChild(notification);
    
    setTimeout(() => notification.classList.add('show'), 10);
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

function celebrateTransaction() {
    const celebration = document.createElement('div');
    celebration.className = 'celebration';
    celebration.innerHTML = '🎉';
    document.body.appendChild(celebration);
    
    setTimeout(() => celebration.remove(), 2000);
}

// Tab Switching
function switchTab(tab) {
    const tabs = document.querySelectorAll('.tab-btn');
    const contents = document.querySelectorAll('.tab-content');
    
    tabs.forEach(t => t.classList.remove('active'));
    contents.forEach(c => c.classList.remove('active'));
    
    if (tab === 'manual') {
        tabs[0].classList.add('active');
        document.getElementById('manualTab').classList.add('active');
        stopCamera();
    } else {
        tabs[1].classList.add('active');
        document.getElementById('cameraTab').classList.add('active');
    }
}

// Camera Scanner
async function startCamera() {
    try {
        const video = document.getElementById('video');
        const startBtn = document.getElementById('startCameraBtn');
        const stopBtn = document.getElementById('stopCameraBtn');
        const status = document.getElementById('scanStatus');
        
        stream = await navigator.mediaDevices.getUserMedia({ 
            video: { facingMode: 'environment' } 
        });
        
        video.srcObject = stream;
        video.play();
        
        startBtn.style.display = 'none';
        stopBtn.style.display = 'inline-block';
        status.innerHTML = '<p style="color: #10b981; font-weight: 600;">✓ Camera active - Point at barcode</p>';
        
        // Initialize barcode reader
        codeReader = new ZXing.BrowserMultiFormatReader();
        
        // Start scanning
        scanFromCamera();
        
    } catch (error) {
        showNotification('Camera access denied or not available', 'error');
        console.error('Camera error:', error);
    }
}

function stopCamera() {
    const video = document.getElementById('video');
    const startBtn = document.getElementById('startCameraBtn');
    const stopBtn = document.getElementById('stopCameraBtn');
    const status = document.getElementById('scanStatus');
    
    if (stream) {
        stream.getTracks().forEach(track => track.stop());
        stream = null;
    }
    
    if (video.srcObject) {
        video.srcObject = null;
    }
    
    startBtn.style.display = 'inline-block';
    stopBtn.style.display = 'none';
    status.innerHTML = '';
}

async function scanFromCamera() {
    const video = document.getElementById('video');
    const canvas = document.getElementById('canvas');
    const context = canvas.getContext('2d');
    
    const scan = async () => {
        if (!stream) return;
        
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        context.drawImage(video, 0, 0, canvas.width, canvas.height);
        
        try {
            const result = await codeReader.decodeFromCanvas(canvas);
            if (result) {
                const barcode = result.text;
                document.getElementById('scanStatus').innerHTML = `<p style="color: #667eea; font-weight: 600;">✓ Scanned: ${barcode}</p>`;
                
                // Fetch and add product
                const response = await fetch(`/api/scan-product/${barcode}`);
                const data = await response.json();
                
                if (response.ok) {
                    addToCart(data);
                    showNotification(`✓ ${data.name} added to cart`, 'success');
                } else {
                    showNotification(data.error || 'Product not found', 'error');
                }
                
                // Continue scanning after 1 second
                setTimeout(() => requestAnimationFrame(scan), 1000);
                return;
            }
        } catch (error) {
            // No barcode found, continue scanning
        }
        
        requestAnimationFrame(scan);
    };
    
    scan();
}
