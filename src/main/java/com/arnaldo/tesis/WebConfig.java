package com.arnaldo.tesis;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el AuthInterceptor y define qué rutas quedan públicas.
 * Todo lo demás exige sesión iniciada.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")          // Protege todo...
                .excludePathPatterns(            // ...menos estas rutas públicas:
                        "/login",
                        "/logout",
                        "/css/**",
                        "/js/**",
                        "/img/**",               // <-- imágenes (fondo del login, etc.)
                        "/images/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error"
                );
    }
}
