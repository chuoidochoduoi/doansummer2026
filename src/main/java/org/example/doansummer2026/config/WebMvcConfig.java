package org.example.doansummer2026.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.example.doansummer2026.enums.TestRequestStatus;
import org.example.doansummer2026.enums.QueueStatus;

/** Cau hinh de phuc vu file upload trong thu muc uploads/ */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cho phep truy cap file upload qua /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    /** Converter String to Enum - hỗ trợ case-insensitive cho status filter */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // TestRequestStatus
        registry.addConverter(new Converter<String, TestRequestStatus>() {
            @Override
            public TestRequestStatus convert(String source) {
                if (source == null || source.isBlank()) return null;
                return TestRequestStatus.valueOf(source.toUpperCase());
            }
        });

        // QueueStatus
        registry.addConverter(new Converter<String, QueueStatus>() {
            @Override
            public QueueStatus convert(String source) {
                if (source == null || source.isBlank()) return null;
                return QueueStatus.valueOf(source.toUpperCase());
            }
        });
    }
}