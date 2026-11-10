
function refreshIcons() {
    if (window.lucide && typeof window.lucide.createIcons === 'function') {
        window.lucide.createIcons();
    }
}
import { login, register, logout, getCurrentUser, restoreSession } from './auth.js';
import { listRestaurants, listFoodItems } from './restaurant.js';
import { getCart, addItem, updateItem, removeItem, clearCart } from './cart.js';
import { placeOrder, listOrders, cancelOrder } from './order.js';
import { getOwnerRestaurant, createOwnerRestaurant, updateOwnerRestaurant, getOwnerMenu, addOwnerFood, updateOwnerFood, deleteOwnerFood, getOwnerOrders, updateOwnerOrderStatus } from './owner.js';
import { getAvailableDeliveries, getWaitingDeliveries, getMyDeliveries, acceptDelivery, markDelivered } from './delivery.js';
import { getAdminStats, getAdminUsers, toggleAdminUserStatus, getAdminRestaurants, toggleAdminRestaurantApproval, getAdminOrders } from './admin.js';

const state = { user:null, restaurants:[], selectedRestaurant:null, foodItems:[], cart:{items:[],total:0}, orders:[], ownerRestaurant:null, ownerMenu:[], ownerOrders:[], availableDeliveries:[], waitingDeliveries:[], myDeliveries:[], adminStats:null, adminUsers:[], adminRestaurants:[], adminOrders:[] };
const INR = new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',minimumFractionDigits:2});
const $ = id => document.getElementById(id);
const money = value => INR.format(Number(value || 0));

const refs = {
    nav:$('main-nav'), auth:$('auth-container'), cartBtn:$('cart-btn'), cartBadge:$('cart-badge'),
    customer:$('customer-app'), owner:$('owner-app'), delivery:$('delivery-app'), admin:$('admin-app'),
    restaurants:$('restaurants-grid'), menu:$('menu-grid'), menuContainer:$('customer-menu-container'), search:$('search-input'), selectedTitle:$('selected-restaurant-title'),
    customerHome:$('customer-home-section'), customerOrders:$('customer-orders-section'), orders:$('orders-list'),
    cart:$('cart-drawer'), cartBackdrop:$('cart-backdrop'), cartItems:$('cart-items-list'), subtotal:$('cart-subtotal'), deliveryFee:$('cart-delivery-fee'), tax:$('cart-tax'), total:$('cart-grand-total'), address:$('address-input'), checkout:$('checkout-btn'), clearCart:$('clear-cart-btn'),
    authBackdrop:$('auth-modal-backdrop'), loginForm:$('login-form'), registerForm:$('register-form'),
    loginTab:$('login-tab-btn'), registerTab:$('register-tab-btn'), authClose:$('auth-modal-close'),
    ownerStats:$('owner-stats'), restaurantForm:$('restaurant-form'), restaurantName:$('restaurant-name'), restaurantDescription:$('restaurant-description'), restaurantHeading:$('owner-restaurant-heading'), ownerMenuPanel:$('owner-menu-panel'), ownerOrdersPanel:$('owner-orders-panel'), ownerMenu:$('owner-menu-list'), ownerOrders:$('owner-orders-list'), foodModal:$('food-modal-backdrop'), foodForm:$('food-form'), foodId:$('food-id'), foodName:$('food-name'), foodDescription:$('food-description'), foodPrice:$('food-price'), foodAvailable:$('food-available'), foodModalTitle:$('food-modal-title'), foodModalClose:$('food-modal-close'),
    deliveryStats:$('delivery-stats'), available:$('available-deliveries'), waiting:$('waiting-deliveries'), myDeliveries:$('my-deliveries'),
    adminStats:$('admin-stats'), adminUsersTbody:$('admin-users-tbody'), adminRestaurantsTbody:$('admin-restaurants-tbody'), adminOrdersTbody:$('admin-orders-tbody'),
    regRole:$('reg-role'), adminKeyGroup:$('admin-key-group'), regAdminKey:$('reg-admin-key')
};

function toast(message,type='info') { const el=document.createElement('div'); el.className=`toast toast-${type}`; el.textContent=message; $('toast-container').appendChild(el); setTimeout(()=>el.remove(),3500); }
function roleLabel(role) { return ({CUSTOMER:'Customer',RESTAURANT_OWNER:'Restaurant Owner',DELIVERY_AGENT:'Delivery Agent',ADMIN:'Admin'})[role] || role; }

function renderAuth() {
    if (!state.user) {
        refs.auth.innerHTML='';
        refs.cartBtn.style.display='none';
        document.body.classList.add('auth-required');
        return;
    }
    document.body.classList.remove('auth-required');
    refs.auth.innerHTML=`<div class="user-menu"><span class="user-avatar">${state.user.username[0].toUpperCase()}</span><div><strong>${state.user.username}</strong><span class="role-badge">${roleLabel(state.user.role)}</span></div><button class="btn btn-sm btn-outline" id="logout-btn"><i data-lucide="log-out" class="inline-icon"></i> Logout</button></div>`;
    refreshIcons();
    $('logout-btn').onclick=handleLogout;
    refs.cartBtn.style.display=state.user.role==='CUSTOMER'?'inline-flex':'none';
}

function showAppForRole() {
    refs.customer.style.display=state.user?.role==='CUSTOMER'?'block':'none';
    refs.owner.style.display=state.user?.role==='RESTAURANT_OWNER'?'block':'none';
    refs.delivery.style.display=state.user?.role==='DELIVERY_AGENT'?'block':'none';
    refs.admin.style.display=state.user?.role==='ADMIN'?'block':'none';
    refs.nav.innerHTML='';
    if (state.user?.role==='CUSTOMER') refs.nav.innerHTML='<button class="nav-btn active" id="home-nav"><i data-lucide="compass" class="nav-icon"></i> Explore</button><button class="nav-btn" id="orders-nav"><i data-lucide="package" class="nav-icon"></i> My Orders</button>';
    if (state.user?.role==='RESTAURANT_OWNER') refs.nav.innerHTML='<span class="nav-context"><i data-lucide="store" class="nav-icon"></i> Partner Dashboard</span>';
    if (state.user?.role==='DELIVERY_AGENT') refs.nav.innerHTML='<span class="nav-context"><i data-lucide="bike" class="nav-icon"></i> Delivery Dashboard</span>';
    if (state.user?.role==='ADMIN') refs.nav.innerHTML='<span class="nav-context"><i data-lucide="shield-check" class="nav-icon"></i> Admin Workspace</span>';
    if (state.user?.role==='CUSTOMER') { $('home-nav').onclick=()=>customerTab('home'); $('orders-nav').onclick=()=>customerTab('orders'); }
    refreshIcons();
}

function customerTab(tab) {
    if(tab==='orders') {
        refs.customerHome.style.display='none';
        refs.customerOrders.style.display='block';
        document.querySelectorAll('.nav-btn').forEach(b=>b.classList.remove('active'));
        $('orders-nav').classList.add('active');
        loadCustomerOrders();
        startCustomerOrderPolling();
    } else {
        refs.customerHome.style.display='block';
        refs.customerOrders.style.display='none';
        document.querySelectorAll('.nav-btn').forEach(b=>b.classList.remove('active'));
        $('home-nav').classList.add('active');
        stopCustomerOrderPolling();
    }
}

function openAuth(mode='login') {
    refs.authBackdrop.classList.add('open');
    refs.authBackdrop.classList.toggle('auth-page-mode', !state.user);

    // Always clear registration inputs
    ['reg-username','reg-email','reg-password'].forEach(id=>{ const el=$(id); if(el) el.value=''; });

    // Always clear password input and reset visibility
    const loginPwd = $('login-password');
    if (loginPwd) {
        loginPwd.value = '';
        loginPwd.type = 'password';
    }
    const regPwd = $('reg-password');
    if (regPwd) {
        regPwd.value = '';
        regPwd.type = 'password';
    }
    const loginIcon = $('login-pwd-icon');
    if (loginIcon) loginIcon.innerHTML = '<i data-lucide="eye"></i>';
    const regIcon = $('reg-pwd-icon');
    if (regIcon) regIcon.innerHTML = '<i data-lucide="eye"></i>';
    refreshIcons();

    // Only show login info if the user explicitly saved it
    const isRemembered = localStorage.getItem('foodverse_remember_login') === 'true';
    const savedUser = localStorage.getItem('foodverse_saved_username') || '';
    const userField = $('login-username');
    const remCheck = $('remember-login-check');

    if (isRemembered && savedUser) {
        if (userField) userField.value = savedUser;
        if (remCheck) remCheck.checked = true;
    } else {
        if (userField) userField.value = '';
        if (remCheck) remCheck.checked = false;
    }

    if(mode==='login'){
        refs.loginTab.classList.add('active');
        refs.registerTab.classList.remove('active');
        refs.loginForm.style.display='block';
        refs.registerForm.style.display='none';
    } else {
        refs.loginTab.classList.remove('active');
        refs.registerTab.classList.add('active');
        refs.loginForm.style.display='none';
        refs.registerForm.style.display='block';
    }
}
function closeAuth(){
    if(!state.user) return;
    refs.authBackdrop.classList.remove('open');
}

async function handleLogout(){
    stopCustomerOrderPolling();
    logout();
    state.user=null;
    state.cart={items:[],total:0};
    refs.restaurantForm.reset();
    refs.foodForm.reset();
    renderAuth();
    showAppForRole();
    openAuth('login');
    toast('Logged out','info');
}

async function loadCustomer(){
    try {
        const page=await listRestaurants(0,50);
        state.restaurants=page.content||[];
        state.selectedRestaurant=null;
        state.foodItems=[];
        if (refs.menuContainer) refs.menuContainer.style.display='none';
        renderRestaurants();
        if(!state.restaurants.length) renderEmptyCustomer();
        await loadCart();
    }
    catch(e){toast(e.message,'error');}
}
function renderEmptyCustomer(){ refs.restaurants.innerHTML='<div class="empty-state"><div class="empty-icon"><i data-lucide="store"></i></div><h3>No restaurants yet</h3><p>Restaurant partners will appear here once they create their FoodVerse restaurant.</p></div>'; refs.menu.innerHTML='<div class="empty-state"><div class="empty-icon"><i data-lucide="utensils"></i></div><h3>Menus will appear here</h3><p>Choose a restaurant to browse its menu.</p></div>'; refreshIcons(); }
function renderRestaurants(){
    const q=refs.search.value.toLowerCase().trim();
    const restaurants=state.restaurants.filter(r=>!q||r.name.toLowerCase().includes(q)||(r.description||'').toLowerCase().includes(q));
    if(!restaurants.length){refs.restaurants.innerHTML='<div class="empty-state"><div class="empty-icon"><i data-lucide="search"></i></div><h3>No restaurants found</h3><p>Try another restaurant name or clear the search.</p></div>';refreshIcons();return;}
    refs.restaurants.innerHTML=restaurants.map(r=>`<article class="restaurant-card ${state.selectedRestaurant?.id===r.id?'selected':''}" data-id="${r.id}"><div class="restaurant-cover"><span class="cover-icon"><i data-lucide="utensils-crossed"></i></span><span class="restaurant-badge"><i data-lucide="check" class="inline-icon"></i> Verified</span></div><div class="restaurant-body"><h3>${escapeHtml(r.name)}</h3><p>${escapeHtml(r.description||'Freshly prepared food from your local restaurant.')}</p><div class="restaurant-meta"><span>30–40 min</span><span>₹50 delivery</span></div><button type="button" class="btn btn-outline btn-sm view-menu-btn" data-id="${r.id}">View Menu</button></div></article>`).join(''); refreshIcons();
    refs.restaurants.querySelectorAll('.restaurant-card').forEach(c=>c.onclick=()=>{const r=state.restaurants.find(x=>x.id===+c.dataset.id);if(r)selectRestaurant(r);});
    refs.restaurants.querySelectorAll('.view-menu-btn').forEach(b=>b.onclick=e=>{
        e.stopPropagation();
        const r=state.restaurants.find(x=>x.id===+b.dataset.id);
        if(r){ selectRestaurant(r); refs.menu.scrollIntoView({behavior:'smooth', block:'start'}); }
    });
}
async function selectRestaurant(r){
    state.selectedRestaurant=r;
    if(refs.menuContainer) refs.menuContainer.style.display='block';
    refs.selectedTitle.textContent=`${r.name} Menu`;
    refs.search.value='';
    renderRestaurants();
    try {
        const p=await listFoodItems(r.id,0,100);
        state.foodItems=p.content||[];
        renderMenu();
        if(refs.menuContainer) {
            refs.menuContainer.scrollIntoView({behavior:'smooth', block:'start'});
        }
    } catch(e) {
        toast(e.message,'error');
    }
}
function dishIcon(name){
    const n = name.toLowerCase();
    if(n.includes('pizza')) return '<i data-lucide="pizza"></i>';
    if(n.includes('burger')) return '<i data-lucide="sandwich"></i>';
    if(n.includes('biryani') || n.includes('rice') || n.includes('curry')) return '<i data-lucide="soup"></i>';
    if(n.includes('pasta') || n.includes('noodle') || n.includes('momo')) return '<i data-lucide="soup"></i>';
    if(n.includes('dessert') || n.includes('cake') || n.includes('waffle')) return '<i data-lucide="cake-slice"></i>';
    if(n.includes('coffee') || n.includes('tea') || n.includes('shake')) return '<i data-lucide="coffee"></i>';
    if(n.includes('salad')) return '<i data-lucide="salad"></i>';
    return '<i data-lucide="utensils"></i>';
}
function dishEmoji(name){ return dishIcon(name); }
function renderMenu(){const q=refs.search.value.toLowerCase().trim();const items=state.foodItems.filter(i=>i.name.toLowerCase().includes(q)||(i.description||'').toLowerCase().includes(q)); if(!items.length){refs.menu.innerHTML='<div class="empty-state"><div class="empty-icon"><i data-lucide="utensils"></i></div><h3>No dishes found</h3><p>This restaurant has not added a matching dish yet.</p></div>';refreshIcons();return;} /* render menu */ refs.menu.innerHTML=items.map(i=>`<article class="food-card"><div class="food-avatar">${dishIcon(i.name)}</div><div class="food-card-content"><div class="food-status ${i.available?'in-stock':'out-of-stock'}">${i.available?'Available':'Unavailable'}</div><h3>${escapeHtml(i.name)}</h3><p>${escapeHtml(i.description||'Prepared fresh to order.')}</p><div class="food-footer"><strong>${money(i.price)}</strong><div class="food-actions"><div class="qty-control"><button class="qty-btn" data-action="minus" data-id="${i.id}">−</button><span id="qty-${i.id}">1</span><button class="qty-btn" data-action="plus" data-id="${i.id}">+</button></div><button class="btn btn-primary btn-sm add-btn" data-id="${i.id}" ${i.available?'':'disabled'}>Add</button></div></div></div></article>`).join(''); refreshIcons();
    refs.menu.querySelectorAll('[data-action]').forEach(b=>b.onclick=e=>{e.stopPropagation();const s=$(`qty-${b.dataset.id}`);let n=+s.textContent;n=b.dataset.action==='plus'?n+1:Math.max(1,n-1);s.textContent=n;});
    refs.menu.querySelectorAll('.add-btn').forEach(b=>b.onclick=async()=>{
        if(!state.user){openAuth('login');return;}
        const qty = +$(`qty-${b.dataset.id}`).textContent;
        try {
            state.cart = await addItem({foodItemId:+b.dataset.id, quantity:qty});
            updateCartUI();
            toast('Added to cart','success');
        } catch(e) {
            if(e.message && e.message.includes('Clear the cart before choosing another restaurant')) {
                if(confirm('Your cart already has items from another restaurant.\n\nClear your cart and add this dish instead?')) {
                    try {
                        await clearCart();
                        state.cart = await addItem({foodItemId:+b.dataset.id, quantity:qty});
                        updateCartUI();
                        toast('Cart cleared and new dish added!','success');
                    } catch(err) {
                        toast(err.message,'error');
                    }
                }
            } else {
                toast(e.message,'error');
            }
        }
    });
}

async function loadCart(){try{state.cart=await getCart();updateCartUI();}catch(e){state.cart={items:[],total:0};updateCartUI();if(e.status!==401&&e.status!==403)toast(e.message,'error');}}
function updateCartUI(){const items=state.cart.items||[];refs.cartBadge.textContent=items.reduce((s,i)=>s+i.quantity,0); if(!items.length){refs.cartItems.innerHTML='<div class="empty-state"><div class="empty-icon"><i data-lucide="shopping-bag"></i></div><h3>Your cart is empty</h3><p>Add dishes from a restaurant to start your order.</p></div>';refreshIcons();}else{refs.cartItems.innerHTML=items.map(i=>`<div class="cart-item"><div><strong>${escapeHtml(i.name)}</strong><span>${money(i.price)} each</span></div><div class="cart-item-controls"><button class="qty-btn cart-minus" data-id="${i.id}">−</button><span>${i.quantity}</span><button class="qty-btn cart-plus" data-id="${i.id}">+</button><button class="icon-btn remove-item" data-id="${i.id}">×</button></div></div>`).join(''); refs.cartItems.querySelectorAll('.cart-plus').forEach(b=>b.onclick=async()=>{try{await changeCart(+b.dataset.id,1);}catch(e){toast(e.message,'error');}});refs.cartItems.querySelectorAll('.cart-minus').forEach(b=>b.onclick=async()=>{try{await changeCart(+b.dataset.id,-1);}catch(e){toast(e.message,'error');}});refs.cartItems.querySelectorAll('.remove-item').forEach(b=>b.onclick=async()=>{try{await removeItem(+b.dataset.id);await loadCart();}catch(e){toast(e.message,'error');}});}
    const sub=state.cart.total||0, fee=sub?50:0, tax=Math.round(sub*.05*100)/100; refs.subtotal.textContent=money(sub);refs.deliveryFee.textContent=money(fee);refs.tax.textContent=money(tax);refs.total.textContent=money(sub+fee+tax);refs.checkout.disabled=!items.length;
}
async function changeCart(id,delta){const item=state.cart.items.find(i=>i.id===id);if(!item)return;const q=item.quantity+delta;if(q<=0)await removeItem(id);else await updateItem(id,q);await loadCart();}

let customerPollTimer = null;

function orderProgressStepper(status) {
    if (status === 'CANCELLED' || status === 'REJECTED') {
        return `<div class="order-alert-box order-alert-cancelled"><i data-lucide="alert-triangle" class="inline-icon"></i> Order ${prettyStatus(status)}</div>`;
    }
    const steps = [
        { key: 'PENDING', label: 'Placed' },
        { key: 'CONFIRMED', label: 'Confirmed' },
        { key: 'PREPARING', label: 'Preparing' },
        { key: 'OUT_FOR_DELIVERY', label: 'On the Way' },
        { key: 'DELIVERED', label: 'Delivered' }
    ];
    let rank = 1;
    if (status === 'PENDING') rank = 1;
    else if (status === 'CONFIRMED') rank = 2;
    else if (status === 'PREPARING') rank = 3;
    else if (status === 'READY_FOR_PICKUP' || status === 'OUT_FOR_DELIVERY') rank = 4;
    else if (status === 'DELIVERED') rank = 5;

    return `<div class="order-progress-stepper">${steps.map((st, idx) => {
        const stepNum = idx + 1;
        const isCompleted = stepNum < rank;
        const isActive = stepNum === rank;
        const cls = isCompleted ? 'completed' : isActive ? 'active' : '';
        const symbol = isCompleted ? '<i data-lucide="check" class="step-check-icon"></i>' : stepNum;
        return `<div class="step-node ${cls}"><div class="step-circle">${symbol}</div><span>${st.label}</span></div>`;
    }).join('')}</div>`;
}

function startCustomerOrderPolling() {
    stopCustomerOrderPolling();
    customerPollTimer = setInterval(async () => {
        if (!state.user || state.user.role !== 'CUSTOMER') {
            stopCustomerOrderPolling();
            return;
        }
        const isOrdersVisible = refs.customerOrders && refs.customerOrders.style.display !== 'none';
        const hasActiveOrders = state.orders.some(o => o.status !== 'DELIVERED' && o.status !== 'CANCELLED' && o.status !== 'REJECTED');
        if (isOrdersVisible || hasActiveOrders) {
            try {
                const prevStatuses = new Map(state.orders.map(o => [o.id, o.status]));
                const p = await listOrders(0, 50);
                const newOrders = p.content || [];

                newOrders.forEach(no => {
                    const prev = prevStatuses.get(no.id);
                    if (prev && prev !== 'DELIVERED' && no.status === 'DELIVERED') {
                        toast(`Order #${no.id} has been delivered!`, 'success');
                    } else if (prev && prev !== no.status) {
                        toast(`Order #${no.id} is now ${prettyStatus(no.status)}`, 'info');
                    }
                });

                state.orders = newOrders;
                if (isOrdersVisible) {
                    renderCustomerOrders();
                }
            } catch(e) {
                // Ignore silent background poll errors
            }
        }
    }, 3000);
}

function stopCustomerOrderPolling() {
    if (customerPollTimer) {
        clearInterval(customerPollTimer);
        customerPollTimer = null;
    }
}

async function loadCustomerOrders(){
    try {
        const p = await listOrders(0, 50);
        state.orders = p.content || [];
        renderCustomerOrders();
    } catch(e) {
        toast(e.message, 'error');
    }
}

function renderCustomerOrders(){
    if(!state.orders.length){
        refs.orders.innerHTML='<div class="empty-state"><div class="empty-icon"><i data-lucide="package"></i></div><h3>No orders yet</h3><p>Your order history will appear here after your first purchase.</p></div>';refreshIcons();
        return;
    }
    refs.orders.innerHTML=state.orders.map(orderCardCustomer).join(''); refreshIcons();
    refs.orders.querySelectorAll('.cancel-order').forEach(b=>b.onclick=async()=>{
        try {
            await cancelOrder(+b.dataset.id);
            toast('Order cancelled','info');
            loadCustomerOrders();
        } catch(e) {
            toast(e.message,'error');
        }
    });
}

function orderCardCustomer(o){
    return `<article class="order-card" id="order-card-${o.id}">
        <div class="order-card-header">
            <div>
                <span class="order-id">Order #${o.id}</span>
                <span class="order-restaurant">${escapeHtml(o.restaurantName||'Restaurant')}</span>
            </div>
            <span class="order-status-badge status-${o.status}">${prettyStatus(o.status)}</span>
        </div>
        ${orderProgressStepper(o.status)}
        <div class="order-items-preview">${(o.items||[]).map(i=>`${i.quantity}× ${escapeHtml(i.name)}`).join(' • ')}</div>
        <div class="order-address"><i data-lucide="map-pin" class="inline-icon"></i> ${escapeHtml(o.deliveryAddress||'Address not available')}</div>
        <div class="order-card-footer">
            <strong>${money(o.total)}</strong>
            ${o.status==='PENDING'?`<button class="btn btn-danger btn-sm cancel-order" data-id="${o.id}">Cancel</button>`:''}
        </div>
    </article>`;
}

async function loadOwner(){try{state.ownerRestaurant=await getOwnerRestaurant();populateOwnerRestaurant();await Promise.all([loadOwnerMenu(),loadOwnerOrders()]);}catch(e){if(e.status===404){showOwnerSetup();}else toast(e.message,'error');}}
function showOwnerSetup(){state.ownerRestaurant=null;state.ownerMenu=[];state.ownerOrders=[];refs.restaurantForm.reset();refs.restaurantForm.style.display='block';refs.ownerMenuPanel.style.display='none';refs.ownerOrdersPanel.style.display='none';refs.restaurantHeading.textContent='Create your restaurant';$('restaurant-save-btn').textContent='Create Restaurant';refs.ownerStats.innerHTML=stat('store','Restaurant','Not created')+stat('utensils','Menu items','0')+stat('package','Orders','0'); refreshIcons();}
function populateOwnerRestaurant(){refs.restaurantForm.style.display='block';refs.ownerMenuPanel.style.display='block';refs.ownerOrdersPanel.style.display='block';refs.restaurantHeading.textContent=state.ownerRestaurant.name;refs.restaurantName.value=state.ownerRestaurant.name;refs.restaurantDescription.value=state.ownerRestaurant.description||'';$('restaurant-save-btn').textContent='Save Restaurant';}
async function loadOwnerMenu(){const p=await getOwnerMenu();state.ownerMenu=p.content||[];renderOwnerMenu();renderOwnerStats();}
async function loadOwnerOrders(){const p=await getOwnerOrders();state.ownerOrders=p.content||[];renderOwnerOrders();renderOwnerStats();}
function renderOwnerStats(){refs.ownerStats.innerHTML=stat('store','Restaurant',state.ownerRestaurant?'Live':'Setup')+stat('utensils','Menu items',state.ownerMenu.length)+stat('package','Orders',state.ownerOrders.length); refreshIcons();}
function stat(icon,label,value){return `<div class="stat-card"><span class="stat-icon"><i data-lucide="${icon}"></i></span><div><span>${label}</span><strong>${value}</strong></div></div>`;}
function renderOwnerMenu(){if(!state.ownerMenu.length){refs.ownerMenu.innerHTML='<div class="empty-state compact"><div class="empty-icon"><i data-lucide="utensils"></i></div><h3>Your menu is empty</h3><p>Add your first food item with its INR price.</p></div>';refreshIcons();return;}/* owner menu */ refreshIcons(); refs.ownerMenu.innerHTML=state.ownerMenu.map(i=>`<div class="management-row"><div class="management-main"><span class="food-avatar small">${dishIcon(i.name)}</span><div><strong>${escapeHtml(i.name)}</strong><p>${escapeHtml(i.description||'No description')}</p></div></div><div class="management-price">${money(i.price)}<span class="availability ${i.available?'yes':'no'}">${i.available?'Available':'Unavailable'}</span></div><div class="management-actions"><button class="btn btn-outline btn-sm edit-food" data-id="${i.id}">Edit</button><button class="btn btn-danger btn-sm delete-food" data-id="${i.id}">Delete</button></div></div>`).join('');refs.ownerMenu.querySelectorAll('.edit-food').forEach(b=>b.onclick=()=>openFoodForm(state.ownerMenu.find(x=>x.id===+b.dataset.id)));refs.ownerMenu.querySelectorAll('.delete-food').forEach(b=>b.onclick=async()=>{if(confirm('Delete this menu item?')){try{await deleteOwnerFood(+b.dataset.id);await loadOwnerMenu();toast('Food item deleted','info');}catch(e){toast(e.message,'error');}}});}
function openFoodForm(item=null){refs.foodForm.reset();refs.foodId.value=item?.id||'';refs.foodName.value=item?.name||'';refs.foodDescription.value=item?.description||'';refs.foodPrice.value=item?.price??'';refs.foodAvailable.value=String(item?.available ?? true);refs.foodModalTitle.textContent=item?'Edit Food Item':'Add Food Item';refs.foodModal.classList.add('open');}
function renderOwnerOrders(){if(!state.ownerOrders.length){refs.ownerOrders.innerHTML='<div class="empty-state compact"><div class="empty-icon"><i data-lucide="package"></i></div><h3>No orders yet</h3><p>Customer orders will appear here.</p></div>';refreshIcons();return;}/* owner orders */ refreshIcons(); refs.ownerOrders.innerHTML=state.ownerOrders.map(o=>`<article class="order-card"><div class="order-card-header"><div><span class="order-id">Order #${o.id}</span><span class="order-restaurant">${escapeHtml(o.customerName||'Customer')}</span></div><span class="order-status-badge status-${o.status}">${prettyStatus(o.status)}</span></div><div class="order-items-preview">${(o.items||[]).map(i=>`${i.quantity}× ${escapeHtml(i.name)}`).join(' • ')}</div><div class="order-address"><i data-lucide="map-pin" class="inline-icon"></i> ${escapeHtml(o.deliveryAddress||'')}</div><div class="order-card-footer"><strong>${money(o.total)}</strong>${ownerActions(o)}</div></article>`).join('');refs.ownerOrders.querySelectorAll('[data-next-status]').forEach(b=>b.onclick=async()=>{try{await updateOwnerOrderStatus(+b.dataset.id,b.dataset.nextStatus);await loadOwnerOrders();toast('Order status updated','success');}catch(e){toast(e.message,'error');}});}
function ownerActions(o){if(o.status==='PENDING')return `<button class="btn btn-primary btn-sm" data-id="${o.id}" data-next-status="CONFIRMED">Accept Order</button><button class="btn btn-danger btn-sm" data-id="${o.id}" data-next-status="REJECTED">Reject</button>`;if(o.status==='CONFIRMED')return `<button class="btn btn-primary btn-sm" data-id="${o.id}" data-next-status="PREPARING">Start Preparing</button>`;if(o.status==='PREPARING')return `<button class="btn btn-primary btn-sm" data-id="${o.id}" data-next-status="READY_FOR_PICKUP">Ready for Pickup</button>`;return '';}

async function loadDelivery(){
    try{
        const [a,w,m]=await Promise.all([getAvailableDeliveries(),getWaitingDeliveries(),getMyDeliveries()]);
        state.availableDeliveries=a.content||[];
        state.waitingDeliveries=w.content||[];
        state.myDeliveries=m.content||[];
        renderDelivery();
    }catch(e){toast(e.message,'error');}
}
function renderDelivery(){
    refs.deliveryStats.innerHTML=stat('map-pin','Ready now',state.availableDeliveries.length)+stat('utensils','Being prepared',state.waitingDeliveries.length)+stat('bike','My active',state.myDeliveries.filter(x=>x.status==='OUT_FOR_DELIVERY').length)+stat('check-circle-2','Completed',state.myDeliveries.filter(x=>x.status==='DELIVERED').length);
    refs.available.innerHTML=state.availableDeliveries.length?state.availableDeliveries.map(o=>deliveryCard(o,true)).join(''):'<div class="empty-state compact"><div class="empty-icon"><i data-lucide="bike"></i></div><h3>No deliveries ready yet</h3><p>When a restaurant marks an order <strong>Ready for Pickup</strong>, it will appear here.</p></div>';
    refs.waiting.innerHTML=state.waitingDeliveries.length?state.waitingDeliveries.map(o=>deliveryCard(o,false,true)).join(''):'<div class="empty-state compact"><div class="empty-icon"><i data-lucide="utensils"></i></div><h3>No orders waiting for the restaurant</h3><p>Orders appear here while the restaurant is confirming or preparing them.</p></div>';
    refs.myDeliveries.innerHTML=state.myDeliveries.length?state.myDeliveries.map(o=>deliveryCard(o,false,false)).join(''):'<div class="empty-state compact"><div class="empty-icon"><i data-lucide="package"></i></div><h3>No deliveries yet</h3><p>Accept a ready order to start a delivery.</p></div>';
    refreshIcons();
    document.querySelectorAll('.accept-delivery').forEach(b=>b.onclick=async()=>{try{await acceptDelivery(+b.dataset.id);await loadDelivery();toast('Delivery accepted','success');}catch(e){toast(e.message,'error');}});
    document.querySelectorAll('.mark-delivered').forEach(b=>b.onclick=async()=>{try{await markDelivered(+b.dataset.id);await loadDelivery();toast('Order marked as delivered','success');}catch(e){toast(e.message,'error');}});
}
function deliveryCard(o,available,waiting=false){return `<article class="order-card delivery-card"><div class="order-card-header"><div><span class="order-id">Order #${o.id}</span><span class="order-restaurant">${escapeHtml(o.restaurantName||'Restaurant')}</span></div><span class="order-status-badge status-${o.status}">${prettyStatus(o.status)}</span></div><div class="delivery-route"><span><i data-lucide="store" class="inline-icon"></i> ${escapeHtml(o.restaurantName||'Restaurant')}</span><span class="route-line">→</span><span><i data-lucide="map-pin" class="inline-icon"></i> ${escapeHtml(o.deliveryAddress||'Customer')}</span></div><div class="order-items-preview">${(o.items||[]).map(i=>`${i.quantity}× ${escapeHtml(i.name)}`).join(' • ')}</div><div class="order-card-footer"><strong>${money(o.total)}</strong>${available?`<button class="btn btn-primary btn-sm accept-delivery" data-id="${o.id}">Accept Delivery</button>`:waiting?`<span class="muted">Waiting for restaurant</span>`:o.status==='OUT_FOR_DELIVERY'?`<button class="btn btn-primary btn-sm mark-delivered" data-id="${o.id}">Mark Delivered</button>`:''}</div></article>`;}

async function loadAdmin() {
    try {
        await Promise.all([loadAdminStats(), loadAdminRestaurants(), loadAdminUsers(), loadAdminOrders()]);
    } catch(e) {
        toast(e.message, 'error');
    }
}

async function loadAdminStats() {
    try {
        const stats = await getAdminStats();
        state.adminStats = stats;
        refs.adminStats.innerHTML =
            stat('users', 'Total Users', stats.totalUsers) +
            stat('store', 'Total Restaurants', stats.totalRestaurants) +
            stat('package', 'Total Orders', stats.totalOrders) +
            stat('badge-indian-rupee', 'Delivered Revenue', money(stats.totalRevenue));
        refreshIcons();
    } catch(e) {
        toast(e.message, 'error');
    }
}

async function loadAdminRestaurants() {
    try {
        const p = await getAdminRestaurants();
        state.adminRestaurants = p.content || [];
        renderAdminRestaurants();
    } catch(e) {
        toast(e.message, 'error');
    }
}

function renderAdminRestaurants() {
    if (!state.adminRestaurants.length) {
        refs.adminRestaurantsTbody.innerHTML = '<tr><td colspan="5" class="text-center muted" style="padding:1.5rem;text-align:center">No restaurants created yet</td></tr>';
        return;
    }
    refs.adminRestaurantsTbody.innerHTML = state.adminRestaurants.map(r => `
        <tr>
            <td><strong>#${r.id}</strong></td>
            <td><strong>${escapeHtml(r.name)}</strong></td>
            <td>${escapeHtml(r.description || 'No description')}</td>
            <td><span class="badge-tag ${r.approved ? 'badge-success' : 'badge-warning'}">${r.approved ? 'Approved' : 'Pending Approval'}</span></td>
            <td><button class="btn btn-sm ${r.approved ? 'btn-danger' : 'btn-primary'} toggle-rest-approval" data-id="${r.id}">${r.approved ? 'Suspend' : 'Approve'}</button></td>
        </tr>
    `).join('');
    refs.adminRestaurantsTbody.querySelectorAll('.toggle-rest-approval').forEach(b => {
        b.onclick = async () => {
            try {
                await toggleAdminRestaurantApproval(+b.dataset.id);
                toast('Restaurant status updated', 'success');
                await Promise.all([loadAdminRestaurants(), loadAdminStats()]);
            } catch(e) {
                toast(e.message, 'error');
            }
        };
    });
}

async function loadAdminUsers() {
    try {
        const p = await getAdminUsers();
        state.adminUsers = p.content || [];
        renderAdminUsers();
    } catch(e) {
        toast(e.message, 'error');
    }
}

function renderAdminUsers() {
    if (!state.adminUsers.length) {
        refs.adminUsersTbody.innerHTML = '<tr><td colspan="6" class="text-center muted" style="padding:1.5rem;text-align:center">No users registered yet</td></tr>';
        return;
    }
    refs.adminUsersTbody.innerHTML = state.adminUsers.map(u => `
        <tr>
            <td><strong>#${u.id}</strong></td>
            <td><strong>${escapeHtml(u.username)}</strong></td>
            <td>${escapeHtml(u.email)}</td>
            <td><span class="badge-tag badge-info">${roleLabel(u.role)}</span></td>
            <td><span class="badge-tag ${u.enabled ? 'badge-success' : 'badge-danger'}">${u.enabled ? 'Active' : 'Disabled'}</span></td>
            <td><button class="btn btn-sm ${u.enabled ? 'btn-danger' : 'btn-outline'} toggle-user-status" data-id="${u.id}">${u.enabled ? 'Disable' : 'Enable'}</button></td>
        </tr>
    `).join('');
    refs.adminUsersTbody.querySelectorAll('.toggle-user-status').forEach(b => {
        b.onclick = async () => {
            try {
                await toggleAdminUserStatus(+b.dataset.id);
                toast('User status updated', 'success');
                await Promise.all([loadAdminUsers(), loadAdminStats()]);
            } catch(e) {
                toast(e.message, 'error');
            }
        };
    });
}

async function loadAdminOrders() {
    try {
        const p = await getAdminOrders();
        state.adminOrders = p.content || [];
        renderAdminOrders();
    } catch(e) {
        toast(e.message, 'error');
    }
}

function renderAdminOrders() {
    if (!state.adminOrders.length) {
        refs.adminOrdersTbody.innerHTML = '<tr><td colspan="6" class="text-center muted" style="padding:1.5rem;text-align:center">No platform orders placed yet</td></tr>';
        return;
    }
    refs.adminOrdersTbody.innerHTML = state.adminOrders.map(o => `
        <tr>
            <td><strong>#${o.id}</strong></td>
            <td>${escapeHtml(o.customerName || 'Customer')}</td>
            <td>${escapeHtml(o.restaurantName || 'Restaurant')}</td>
            <td><strong>${money(o.total)}</strong></td>
            <td><span class="order-status-badge status-${o.status}">${prettyStatus(o.status)}</span></td>
            <td>${escapeHtml(o.deliveryAddress || 'N/A')}</td>
        </tr>
    `).join('');
}

function prettyStatus(s){return s.replaceAll('_',' ');}
function escapeHtml(v){return String(v??'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));}

function setupPasswordToggle(inputEl, btnEl, iconEl) {
    if (!inputEl || !btnEl || !iconEl) return;
    btnEl.onclick = e => {
        e.preventDefault();
        const isPwd = inputEl.type === 'password';
        inputEl.type = isPwd ? 'text' : 'password';
        iconEl.innerHTML = isPwd ? '<i data-lucide="eye-off"></i>' : '<i data-lucide="eye"></i>';
        btnEl.setAttribute('aria-label', isPwd ? 'Hide password' : 'Show password');
        refreshIcons();
    };
}

function attach(){
    refs.cartBtn.onclick=()=>{refs.cart.classList.add('open');refs.cartBackdrop.classList.add('open');}; $('cart-close-btn').onclick=closeCart; refs.cartBackdrop.onclick=closeCart;
    refs.clearCart.onclick=async()=>{if(confirm('Clear your cart?')){try{await clearCart();await loadCart();}catch(e){toast(e.message,'error');}}};
    refs.checkout.onclick=async()=>{const address=refs.address.value.trim();if(!address){toast('Enter a delivery address','error');return;}const items=state.cart.items.map(i=>({foodItemId:i.foodItemId,quantity:i.quantity}));try{refs.checkout.disabled=true;const o=await placeOrder({items,deliveryAddress:address,couponCode:null});await clearCart();state.cart={items:[],total:0};updateCartUI();closeCart();refs.address.value='';toast(`Order #${o.id} placed`,'success');customerTab('orders');}catch(e){toast(e.message,'error');}finally{refs.checkout.disabled=false;}};
    refs.search.oninput=()=>{renderRestaurants();renderMenu();};
    refs.authClose.onclick=closeAuth;refs.authBackdrop.onclick=e=>{if(e.target===refs.authBackdrop)closeAuth();};refs.loginTab.onclick=()=>openAuth('login');refs.registerTab.onclick=()=>openAuth('register');

    // Password visibility toggles
    setupPasswordToggle($('login-password'), $('toggle-login-password'), $('login-pwd-icon'));
    setupPasswordToggle($('reg-password'), $('toggle-reg-password'), $('reg-pwd-icon'));

    refs.loginForm.onsubmit=async e=>{
        e.preventDefault();
        const uname = $('login-username').value.trim();
        const pwd = $('login-password').value;
        const rememberMe = $('remember-login-check')?.checked;

        try {
            await login(uname, pwd);
            if (rememberMe) {
                localStorage.setItem('foodverse_remember_login', 'true');
                localStorage.setItem('foodverse_saved_username', uname);
            } else {
                localStorage.removeItem('foodverse_remember_login');
                localStorage.removeItem('foodverse_saved_username');
            }
            // Clear password immediately so it is never shown by default
            $('login-password').value = '';
            $('login-password').type = 'password';
            if ($('login-pwd-icon')) { $('login-pwd-icon').innerHTML = '<i data-lucide="eye"></i>'; refreshIcons(); }

            state.user=getCurrentUser();
            renderAuth();
            showAppForRole();
            closeAuth();
            toast('Signed in successfully','success');
            routeByRole();
        } catch(err) {
            toast(err.message,'error');
        }
    };

    refs.regRole.onchange = () => {
        refs.adminKeyGroup.style.display = refs.regRole.value === 'ADMIN' ? 'block' : 'none';
    };

    refs.registerForm.onsubmit=async e=>{
        e.preventDefault();
        try {
            const payload = {
                username:$('reg-username').value.trim(),
                email:$('reg-email').value.trim(),
                password:$('reg-password').value,
                role:$('reg-role').value
            };
            if (payload.role === 'ADMIN') {
                payload.adminKey = refs.regAdminKey.value.trim();
            }
            await register(payload);
            toast('Account created. Please sign in.','success');
            openAuth('login');
        } catch(err) {
            toast(err.message,'error');
        }
    };

    const refreshCustOrdersBtn = $('refresh-customer-orders');
    if (refreshCustOrdersBtn) {
        refreshCustOrdersBtn.onclick = async () => {
            try {
                await loadCustomerOrders();
                toast('Orders refreshed', 'info');
            } catch(e) {
                toast(e.message, 'error');
            }
        };
    }

    const refreshAdminRestBtn = $('refresh-admin-restaurants');
    if (refreshAdminRestBtn) {
        refreshAdminRestBtn.onclick = async () => {
            try {
                await Promise.all([loadAdminRestaurants(), loadAdminStats()]);
                toast('Restaurants refreshed', 'info');
            } catch(e) {
                toast(e.message, 'error');
            }
        };
    }

    const refreshAdminUsersBtn = $('refresh-admin-users');
    if (refreshAdminUsersBtn) {
        refreshAdminUsersBtn.onclick = async () => {
            try {
                await Promise.all([loadAdminUsers(), loadAdminStats()]);
                toast('Users refreshed', 'info');
            } catch(e) {
                toast(e.message, 'error');
            }
        };
    }

    const refreshAdminOrdersBtn = $('refresh-admin-orders');
    if (refreshAdminOrdersBtn) {
        refreshAdminOrdersBtn.onclick = async () => {
            try {
                await Promise.all([loadAdminOrders(), loadAdminStats()]);
                toast('Orders refreshed', 'info');
            } catch(e) {
                toast(e.message, 'error');
            }
        };
    }

    refs.restaurantForm.onsubmit=async e=>{e.preventDefault();const payload={name:refs.restaurantName.value.trim(),description:refs.restaurantDescription.value.trim()};try{state.ownerRestaurant?await updateOwnerRestaurant(payload):await createOwnerRestaurant(payload);state.ownerRestaurant=await getOwnerRestaurant();populateOwnerRestaurant();await loadOwnerMenu();await loadOwnerOrders();toast('Restaurant saved','success');}catch(err){toast(err.message,'error');}};
    $('add-food-btn').onclick=()=>openFoodForm();$('refresh-owner-orders').onclick=async()=>{try{await loadOwnerOrders();toast('Orders refreshed','info');}catch(e){toast(e.message,'error');}};$('refresh-delivery').onclick=async()=>{try{await loadDelivery();toast('Deliveries refreshed','info');}catch(e){toast(e.message,'error');}};refs.foodModalClose.onclick=()=>refs.foodModal.classList.remove('open');refs.foodModal.onclick=e=>{if(e.target===refs.foodModal)refs.foodModal.classList.remove('open');};refs.foodForm.onsubmit=async e=>{e.preventDefault();const payload={name:refs.foodName.value.trim(),description:refs.foodDescription.value.trim(),price:Number(refs.foodPrice.value),available:refs.foodAvailable.value==='true'};if(!payload.name||!Number.isFinite(payload.price)||payload.price<=0){toast('Enter a valid food name and price','error');return;}try{const id=refs.foodId.value;if(id)await updateOwnerFood(Number(id),payload);else await addOwnerFood(payload);refs.foodModal.classList.remove('open');await loadOwnerMenu();toast(id?'Food item updated':'Food item added','success');}catch(err){toast(err.message,'error');}};
}
function closeCart(){refs.cart.classList.remove('open');refs.cartBackdrop.classList.remove('open');}
function routeByRole(){
    if(state.user.role==='CUSTOMER')loadCustomer();
    if(state.user.role==='RESTAURANT_OWNER')loadOwner();
    if(state.user.role==='DELIVERY_AGENT')loadDelivery();
    if(state.user.role==='ADMIN')loadAdmin();
}
async function init(){
    state.user=await restoreSession();
    renderAuth();
    showAppForRole();
    attach(); refreshIcons();
    if(state.user) routeByRole();
    else openAuth('login');
}
window.addEventListener('focus', async () => {
    const cur = getCurrentUser();
    if (cur && state.user && (cur.username !== state.user.username || cur.role !== state.user.role)) {
        state.user = await restoreSession();
        renderAuth();
        showAppForRole();
        routeByRole();
    }
});
document.addEventListener('DOMContentLoaded',init);
