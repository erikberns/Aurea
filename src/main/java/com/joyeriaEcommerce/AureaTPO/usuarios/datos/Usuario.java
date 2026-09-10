package com.joyeriaEcommerce.AureaTPO.usuarios.datos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 80)
    private String nombre;

    @Column(name = "last_name", nullable = false, length = 80)
    private String apellido;

    @Column(name = "name", length = 160)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password", nullable = false)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Rol rol;

    protected Usuario() {
    }

    public Usuario(String nombre, String apellido, String email, String contrasenaHash, Rol rol) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void actualizarPerfil(String nombre, String apellido) {
        this.nombre = nombre;
        this.apellido = apellido;
    }
}
