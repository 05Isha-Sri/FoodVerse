import { authFetch } from './auth.js';

async function request(url, options = {}) {
    const resp = await authFetch(url, options);
    if (!resp.ok) {
        let msg = 'Admin request failed';
        try { const data = await resp.json(); msg = data.message || msg; } catch { msg = await resp.text(); }
        const error = new Error(msg || `Request failed (${resp.status})`);
        error.status = resp.status;
        throw error;
    }
    return resp.json();
}

export const getAdminStats = () => request('/api/admin/stats');
export const getAdminUsers = (page = 0, size = 50) => request(`/api/admin/users?page=${page}&size=${size}`);
export const toggleAdminUserStatus = id => request(`/api/admin/users/${id}/toggle-status`, { method: 'PUT' });
export const getAdminRestaurants = (page = 0, size = 50) => request(`/api/admin/restaurants?page=${page}&size=${size}`);
export const toggleAdminRestaurantApproval = id => request(`/api/admin/restaurants/${id}/approval`, { method: 'PUT' });
export const getAdminOrders = (page = 0, size = 50) => request(`/api/admin/orders?page=${page}&size=${size}&sort=id,desc`);
