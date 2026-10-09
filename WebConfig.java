package com.realestate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Serves uploaded images (/uploads/**) and R charts (/charts/**) from disk. */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final Path uploads;
    private final Path charts;

    public WebConfig(@Value("${app.upload.dir}") String uploadDir,
                     @Value("${app.charts.dir}") String chartsDir) throws IOException {
        this.uploads = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.charts = Paths.get(chartsDir).toAbsolutePath().normalize();
        Files.createDirectories(uploads);
        Files.createDirectories(charts);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(uploads.toUri().toString()).setCachePeriod(3600);
        registry.addResourceHandler("/charts/**").addResourceLocations(charts.toUri().toString()).setCachePeriod(0);
    }
}
