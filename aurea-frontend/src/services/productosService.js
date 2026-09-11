const API_URL = import.meta.env.VITE_BACKEND_URL + '/api/productos';

export const getProductos = async () => {
    const response = await fetch(API_URL, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
    });

    if (!response.ok) {
        throw new Error('Error al obtener los productos');
    }

    return response.json();
};

export const getProductoById = async (id) => {
    const response = await fetch(`${API_URL}/${id}`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
    });

    if (!response.ok) {
        throw new Error(`Error al obtener el producto con ID ${id}`);
    }

    return response.json();
};
