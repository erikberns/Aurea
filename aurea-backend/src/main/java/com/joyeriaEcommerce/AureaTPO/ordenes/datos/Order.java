package com.joyeriaEcommerce.AureaTPO.ordenes.datos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Version
    private Integer version;

    @OneToMany(mappedBy = "order", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private java.util.List<OrderItem> items = new java.util.ArrayList<>();

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Column(name = "total")
    private Double total;

    @Column(name = "user_id")
    private Long userId;

    protected Order() {
    }

    public Order(String shippingAddress, LocalDate orderDate, OrderStatus status, Double total, Long userId) {
        this.shippingAddress = shippingAddress;
        this.orderDate = orderDate;
        this.status = status;
        this.total = total;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public java.util.List<OrderItem> getItems() {
        return items;
    }

    public void setItems(java.util.List<OrderItem> items) {
        this.items = items;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
