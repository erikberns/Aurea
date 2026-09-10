package com.joyeriaEcommerce.AureaTPO.ordenes.datos;

import com.joyeriaEcommerce.AureaTPO.productos.datos.Product;
import com.joyeriaEcommerce.AureaTPO.usuarios.datos.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cantidad_producto")
    private Integer cantidadProducto;

    @Column(name = "envio_a")
    private String envioA;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Column(name = "total")
    private Double total;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Usuario user;

    protected Order() {
    }

    public Order(Integer cantidadProducto, String envioA, LocalDate fecha, OrderStatus status, Double total, Product product, Usuario user) {
        this.cantidadProducto = cantidadProducto;
        this.envioA = envioA;
        this.fecha = fecha;
        this.status = status;
        this.total = total;
        this.product = product;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public Integer getCantidadProducto() {
        return cantidadProducto;
    }

    public void setCantidadProducto(Integer cantidadProducto) {
        this.cantidadProducto = cantidadProducto;
    }

    public String getEnvioA() {
        return envioA;
    }

    public void setEnvioA(String envioA) {
        this.envioA = envioA;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
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

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Usuario getUser() {
        return user;
    }

    public void setUser(Usuario user) {
        this.user = user;
    }
}
