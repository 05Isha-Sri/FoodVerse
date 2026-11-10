
import { authFetch } from './auth.js';

export async function getCart() {
    const resp = await authFetch('/api/cart');
    if (!resp.ok) {
        if (resp.status === 401 || resp.status === 403) {
            throw new Error('Please log in to view your cart');
        }
        throw new Error('Failed to load cart');
    }
    return resp.json();
}

export async function addItem(itemDto) {
    const resp = await authFetch('/api/cart/items', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(itemDto)
    });
    if (!resp.ok) {
        let msg = 'Failed to add item';
        try {
            const data = await resp.json();
            msg = data.message || msg;
        } catch(e) {
            msg = await resp.text();
        }
        const error = new Error(msg || `Request failed (${resp.status})`);
        error.status = resp.status;
        throw error;
    }
    return resp.json();
}

export async function updateItem(itemId, quantity) {
    const url = `/api/cart/items/${itemId}?quantity=${quantity}`;
    const resp = await authFetch(url, { method: 'PUT' });
    if (!resp.ok) {
        let msg = 'Failed to update item';
        try {
            const data = await resp.json();
            msg = data.message || msg;
        } catch(e) {
            msg = await resp.text();
        }
        const error = new Error(msg || `Request failed (${resp.status})`);
        error.status = resp.status;
        throw error;
    }
    return resp.json();
}

export async function removeItem(itemId) {
    const url = `/api/cart/items/${itemId}`;
    const resp = await authFetch(url, { method: 'DELETE' });
    if (!resp.ok) {
        let msg = 'Failed to remove item';
        try {
            const data = await resp.json();
            msg = data.message || msg;
        } catch(e) {
            msg = await resp.text();
        }
        const error = new Error(msg || `Request failed (${resp.status})`);
        error.status = resp.status;
        throw error;
    }
}

export async function clearCart() {
    const resp = await authFetch('/api/cart', { method: 'DELETE' });
    if (!resp.ok) {
        throw new Error('Failed to clear cart');
    }
}