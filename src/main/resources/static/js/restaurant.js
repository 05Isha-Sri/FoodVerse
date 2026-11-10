
export async function listRestaurants(page = 0, size = 10) {
    const url = `/api/restaurants?page=${page}&size=${size}`;
    const resp = await fetch(url);
    if (!resp.ok) { const error = new Error(`Failed to load restaurants (${resp.status})`); error.status = resp.status; throw error; }
    return resp.json();
}

export async function getRestaurant(id) {
    const resp = await fetch(`/api/restaurants/${id}`);
    if (!resp.ok) { const error = new Error(`Restaurant ${id} not found`); error.status = resp.status; throw error; }
    return resp.json();
}

export async function listFoodItems(restaurantId, page = 0, size = 20) {
    const url = `/api/restaurants/${restaurantId}/food-items?page=${page}&size=${size}`;
    const resp = await fetch(url);
    if (!resp.ok) { const error = new Error(`Failed to load food items (${resp.status})`); error.status = resp.status; throw error; }
    return resp.json();
}