const API_URL = import.meta.env.VITE_BACKEND_URL + '/api/ordenes';

export const createOrder = async (shippingAddress, items) => {
    const token = localStorage.getItem('token');
    
    const response = await fetch(API_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ shippingAddress, items })
    });

    if (!response.ok) {
        throw new Error('Error al crear la orden');
    }

    return response.json();
};

export const confirmOrder = async (orderId) => {
    const token = localStorage.getItem('token');
    
    const response = await fetch(`${API_URL}/${orderId}/confirmar`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        }
    });

    if (!response.ok) {
        throw new Error('Error al confirmar la orden');
    }

    return response.json();
};
