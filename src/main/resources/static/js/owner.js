import { authFetch } from './auth.js';

async function request(url, options = {}) {
    const resp = await authFetch(url, options);
    if (!resp.ok) {
        let msg = 'Request failed';
        try { const data = await resp.json(); msg = data.message || msg; } catch { msg = await resp.text(); }
        const error = new Error(msg || `Request failed (${resp.status})`);
        error.status = resp.status;
        throw error;
    }
    if (resp.status === 204) return null;
    return resp.json();
}

export const getOwnerRestaurant = () => request('/api/owner/restaurant');
export const createOwnerRestaurant = payload => request('/api/owner/restaurant', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload) });
export const updateOwnerRestaurant = payload => request('/api/owner/restaurant', { method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload) });
export const getOwnerMenu = () => request('/api/owner/menu?page=0&size=100');
export const addOwnerFood = payload => request('/api/owner/menu', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload) });
export const updateOwnerFood = (id,payload) => request(`/api/owner/menu/${id}`, { method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload) });
export const deleteOwnerFood = id => request(`/api/owner/menu/${id}`, { method:'DELETE' });
export const getOwnerOrders = () => request('/api/owner/orders?page=0&size=100');
export const updateOwnerOrderStatus = (id,status) => request(`/api/owner/orders/${id}/status?status=${encodeURIComponent(status)}`, { method:'PUT' });
