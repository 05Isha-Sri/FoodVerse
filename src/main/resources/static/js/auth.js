
function authFetch(url, options = {}) {
    const token = localStorage.getItem('jwt');
    const headers = new Headers(options.headers || {});
    if (token) {
        headers.set('Authorization', 'Bearer ' + token);
    }
    const mergedOptions = { ...options, headers };
    return fetch(url, mergedOptions).then(response => {
        if (response.status === 401) localStorage.removeItem('jwt');
        return response;
    });
}

export async function login(username, password) {
    const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });
    if (!response.ok) {
        let errorMsg = 'Login failed';
        try {
            const data = await response.json();
            errorMsg = data.message || errorMsg;
        } catch (e) {
            errorMsg = await response.text();
        }
        throw new Error(errorMsg);
    }
    const { token } = await response.json();
    localStorage.setItem('jwt', token);
    return token;
}

export async function register(payload) {
    const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    if (!response.ok) {
        let errorMsg = 'Registration failed';
        try {
            const data = await response.json();
            errorMsg = data.message || errorMsg;
        } catch (e) {
            errorMsg = await response.text();
        }
        throw new Error(errorMsg);
    }
}

export function logout() {
    localStorage.removeItem('jwt');
}

export function getCurrentUser() {
    const token = localStorage.getItem('jwt');
    if (!token) return null;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return {
            username: payload.sub,
            role: payload.role
        };
    } catch (e) {
        return null;
    }
}

export { authFetch };

export async function restoreSession() {
    const token = localStorage.getItem('jwt');
    if (!token) return null;
    try {
        const response = await authFetch('/api/auth/me');
        if (!response.ok) {
            localStorage.removeItem('jwt');
            return null;
        }
        const data = await response.json();
        return { username: data.username, role: data.role };
    } catch (e) {
        localStorage.removeItem('jwt');
        return null;
    }
}
