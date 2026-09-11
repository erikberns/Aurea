import { httpRequest } from '../api/httpClient';

export const createOrder = async (shippingAddress, items) => {
    return httpRequest('/ordenes', {
        method: 'POST',
        body: { shippingAddress, items }
    });
};

export const confirmOrder = async (orderId) => {
    return httpRequest(`/ordenes/${orderId}/confirmar`, {
        method: 'POST'
    });
};
