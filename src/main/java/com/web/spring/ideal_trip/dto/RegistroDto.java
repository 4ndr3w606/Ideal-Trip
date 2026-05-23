package com.web.spring.ideal_trip.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max= 80, message ="El nombre no puede exceder 80 caracteres")
    private String nombre;

    @NotBlank(message= "El apellido es obligatorio")
    @Size(max = 80, message = "El apellido no puede exceder 80 caracteres")
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    @Size(max=120)
    private String email;

    @NotBlank
    @Size(min=8, max=100, message = "La contraseña debe tener al menor 8 caracteres")
    private String password;

    @NotBlank(message = "Por favor confirma la contraseña")
    private String confirmarPassword;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;





}
