package com.web.spring.ideal_trip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    /**
     * Sirve /uploads/** desde la carpeta externa configurada en app.uploads.dir.
     * Sin esto, las imágenes subidas no son accesibles vía URL.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluto = Paths.get(uploadsDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absoluto);
    }
}