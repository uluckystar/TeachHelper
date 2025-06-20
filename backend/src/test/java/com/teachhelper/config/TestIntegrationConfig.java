package com.teachhelper.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.teachhelper.security.JwtTokenProvider;
import static org.mockito.Mockito.mock;

@TestConfiguration
@Profile("test")
public class TestIntegrationConfig {
    
    @Bean
    @Primary
    public JwtTokenProvider jwtTokenProvider() {
        return mock(JwtTokenProvider.class);
    }
}
