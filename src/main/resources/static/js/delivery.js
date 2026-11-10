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
    return resp.json();
}

export const getAvailableDeliveries = () => request('/api/delivery/available?page=0&size=100');
export const getWaitingDeliveries = () => request('/api/delivery/waiting?page=0&size=100');
export const getMyDeliveries = () => request('/api/delivery/my?page=0&size=100');
export const acceptDelivery = id => request(`/api/delivery/${id}/accept`, { method:'PUT' });
export const markDelivered = id => request(`/api/delivery/${id}/status?status=DELIVERED`, { method:'PUT' });
