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

export const getMisPedidos = async () => {
    return httpRequest('/ordenes/mis-pedidos', {
        method: 'GET'
    });
};

export const getAllOrders = async () => {
    return httpRequest('/ordenes', {
        method: 'GET'
    });
};
