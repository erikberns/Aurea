package com.joyeriaEcommerce.AureaTPO.productos.datos;

import com.joyeriaEcommerce.AureaTPO.categorias.datos.Category;
import com.joyeriaEcommerce.AureaTPO.descuentos.datos.Discount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activo")
    private Boolean activo;

    @Column(name = "description")
    private String description;

    @Column(name = "name")
    private String name;

    @Column(name = "precio")
    private Double precio;

    @Column(name = "precio_descuento")
    private Double precioDescuento;

    @Column(name = "stock")
    private Integer stock;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "discount_id")
    private Discount discount;

    protected Product() {
    }

    public Product(Boolean activo, String description, String name, Double precio, Double precioDescuento, Integer stock, Category category, Discount discount) {
        this.activo = activo;
        this.description = description;
        this.name = name;
        this.precio = precio;
        this.precioDescuento = precioDescuento;
        this.stock = stock;
        this.category = category;
        this.discount = discount;
    }

    public Long getId() {
        return id;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public Double getPrecioDescuento() {
        return precioDescuento;
    }

    public void setPrecioDescuento(Double precioDescuento) {
        this.precioDescuento = precioDescuento;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Discount getDiscount() {
        return discount;
    }

    public void setDiscount(Discount discount) {
        this.discount = discount;
    }
}
