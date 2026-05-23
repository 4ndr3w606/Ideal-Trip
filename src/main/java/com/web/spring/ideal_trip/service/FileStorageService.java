package com.web.spring.ideal_trip.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio para guardar imágenes subidas por los admins.
 *
 * Reglas:
 *   - Solo acepta image/jpeg, image/png, image/webp.
 *   - Genera un nombre único (UUID) para evitar colisiones.
 *   - Devuelve la URL pública relativa: "/uploads/paquetes/<archivo>".
 *   - La carpeta física se crea automáticamente al arrancar.
 */
@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> CONTENT_TYPES_VALIDOS = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final Path rootDir;
    private final Path paquetesDir;

    public FileStorageService(@Value("${app.uploads.dir}") String uploadsDir) {
        this.rootDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
        this.paquetesDir = rootDir.resolve("paquetes");
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(paquetesDir);
            log.info("Directorio de uploads listo en: {}", paquetesDir);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio de uploads: " + paquetesDir, ex);
        }
    }

    /**
     * Guarda una imagen de paquete y devuelve la URL pública relativa
     * (lista para guardarse en paquete.imagenUrl).
     */
    public String guardarImagenPaquete(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !CONTENT_TYPES_VALIDOS.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido: " + contentType
                            + ". Solo JPG, PNG o WEBP.");
        }

        String extension = extraerExtension(archivo.getOriginalFilename(), contentType);
        String nombreUnico = UUID.randomUUID() + extension;
        Path destino = paquetesDir.resolve(nombreUnico);

        try {
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            log.info("Imagen guardada: {}", destino);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Error al guardar la imagen: " + nombreUnico, ex);
        }

        return "/uploads/paquetes/" + nombreUnico;
    }

    /**
     * Borra una imagen del disco. Silencioso si el archivo no existe.
     * Útil cuando se edita un paquete y se reemplaza la imagen.
     */
    public void borrarImagenPaquete(String urlPublica) {
        if (urlPublica == null || !urlPublica.startsWith("/uploads/paquetes/")) {
            return;
        }
        String nombre = urlPublica.substring("/uploads/paquetes/".length());
        Path archivo = paquetesDir.resolve(nombre);
        try {
            boolean borrado = Files.deleteIfExists(archivo);
            if (borrado) {
                log.info("Imagen borrada: {}", archivo);
            }
        } catch (IOException ex) {
            log.warn("No se pudo borrar la imagen {}: {}", archivo, ex.getMessage());
        }
    }

    /* ============ helpers ============ */

    private String extraerExtension(String nombreOriginal, String contentType) {
        String fileName = StringUtils.cleanPath(nombreOriginal != null ? nombreOriginal : "");
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            return fileName.substring(dot).toLowerCase();
        }
        // Fallback por content-type
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}