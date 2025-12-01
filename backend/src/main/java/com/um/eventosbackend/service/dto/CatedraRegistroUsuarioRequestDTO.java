package com.um.eventosbackend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * DTO para la petición de registro de usuario en el servicio de la cátedra.
 * <p>
 * Estructura requerida para el endpoint POST /api/v1/agregar_usuario
 */
public class CatedraRegistroUsuarioRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("nombreAlumno")
    private String nombreAlumno;

    @JsonProperty("descripcionProyecto")
    private String descripcionProyecto;

    public CatedraRegistroUsuarioRequestDTO() {
        // Constructor vacío para Jackson
    }

    public CatedraRegistroUsuarioRequestDTO(
        String username,
        String password,
        String firstName,
        String lastName,
        String email,
        String nombreAlumno,
        String descripcionProyecto
    ) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.nombreAlumno = nombreAlumno;
        this.descripcionProyecto = descripcionProyecto;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombreAlumno() {
        return nombreAlumno;
    }

    public void setNombreAlumno(String nombreAlumno) {
        this.nombreAlumno = nombreAlumno;
    }

    public String getDescripcionProyecto() {
        return descripcionProyecto;
    }

    public void setDescripcionProyecto(String descripcionProyecto) {
        this.descripcionProyecto = descripcionProyecto;
    }

    @Override
    public String toString() {
        return (
            "CatedraRegistroUsuarioRequestDTO{" +
            "username='" +
            username +
            '\'' +
            ", firstName='" +
            firstName +
            '\'' +
            ", lastName='" +
            lastName +
            '\'' +
            ", email='" +
            email +
            '\'' +
            ", nombreAlumno='" +
            nombreAlumno +
            '\'' +
            ", descripcionProyecto='" +
            descripcionProyecto +
            '\'' +
            '}'
        );
    }
}

