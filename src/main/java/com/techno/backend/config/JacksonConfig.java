package com.techno.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson Configuration
 * Provides ObjectMapper bean for JSON serialization/deserialization
 * Ensures UTF-8 encoding for all JSON responses
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // Configure Hibernate6Module
        Hibernate6Module hibernate6Module = new Hibernate6Module();

        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule(), hibernate6Module)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        // Ensure UTF-8 encoding
        mapper.getFactory().setCharacterEscapes(null);
        return mapper;
    }
}
