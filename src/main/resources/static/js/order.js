
import { authFetch } from './auth.js';

export async function placeOrder(payload) {
    const url = '/api/orders';
    const resp = await authFetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    if (!resp.ok) {
        let msg = 'Order placement failed';
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

export async function getOrder(orderId) {
    const url = `/api/orders/${orderId}`;
    const resp = await authFetch(url);
    if (!resp.ok) throw new Error(`Order ${orderId} not found`);
    return resp.json();
}

export async function listOrders(page = 0, size = 10) {
    const url = `/api/orders/my?page=${page}&size=${size}&sort=id,desc`;
    const resp = await authFetch(url);
    if (!resp.ok) {
        if (resp.status === 401 || resp.status === 403) {
            throw new Error('Please log in to view your orders');
        }
        throw new Error('Failed to list orders');
    }
    return resp.json();
}

export async function cancelOrder(orderId) {
    const url = `/api/orders/${orderId}/cancel`;
    const resp = await authFetch(url, { method: 'PUT' });
    if (!resp.ok) {
        let msg = 'Failed to cancel order';
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