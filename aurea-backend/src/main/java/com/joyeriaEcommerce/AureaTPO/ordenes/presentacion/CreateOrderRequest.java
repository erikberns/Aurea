package com.joyeriaEcommerce.AureaTPO.ordenes.presentacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.List;

public class CreateOrderRequest {

    @NotBlank(message = "La dirección de envío no puede estar vacía")
    private String shippingAddress;

    @jakarta.validation.Valid
    @NotEmpty(message = "El carrito no puede estar vacío")
    private List<@NotNull CreateOrderItemRequest> items;

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<@NotNull CreateOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<@NotNull CreateOrderItemRequest> items) {
        this.items = items;
    }

    public static class CreateOrderItemRequest {
        @NotNull(message = "El ID del producto es obligatorio")
        private Long productId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a 0")
        private Integer quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
