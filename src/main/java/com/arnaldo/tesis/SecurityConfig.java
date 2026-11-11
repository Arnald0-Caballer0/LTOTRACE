package com.arnaldo.tesis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Configuración de Spring Security para LTOTRACE.
 *
 * Spring Security se utiliza para dos funciones:
 *  1. Protección CSRF: toda solicitud que modifica datos (POST, PUT, DELETE) debe
 *     incluir el token de la sesión; si falta o no coincide, se rechaza.
 *  2. Cabeceras de seguridad HTTP: impiden que el sistema se cargue dentro de otra
 *     página, que el navegador interprete mal el tipo de contenido o guarde en caché
 *     páginas con datos, y obligan a usar HTTPS (HSTS).
 *
 * La autenticación (inicio de sesión con bloqueo por intentos) y la autorización por
 * rol y permisos siguen a cargo de LoginController, AuthInterceptor y PermisoService.
 * Por eso aquí todas las rutas se permiten: el control de acceso lo hace el interceptor.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        http
            // El acceso lo controla AuthInterceptor; Spring Security no exige autenticación propia
            .authorizeHttpRequests(a -> a.anyRequest().permitAll())

            // Protección CSRF (token guardado en la sesión del usuario)
            .csrf(Customizer.withDefaults())

            // Respuesta cuando el token CSRF falta o no es válido
            .exceptionHandling(e -> e.accessDeniedHandler(new CsrfRechazoHandler()))

            // Cabeceras de seguridad HTTP
            .headers(h -> h
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)          // X-Frame-Options: DENY
                .contentTypeOptions(Customizer.withDefaults())                      // X-Content-Type-Options: nosniff
                .cacheControl(Customizer.withDefaults())                            // Cache-Control: no-store
                .httpStrictTransportSecurity(hsts -> hsts                           // Strict-Transport-Security
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .contentSecurityPolicy(c -> c.policyDirectives("frame-ancestors 'none'")))

            // LTOTRACE usa su propio inicio y cierre de sesión (LoginController)
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable())
            .requestCache(rc -> rc.disable());

        return http.build();
    }

    /**
     * Spring Security no autentica usuarios en LTOTRACE. Declarar este administrador
     * evita que Spring Boot cree un usuario por defecto con una contraseña generada.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return autenticacion -> {
            throw new BadCredentialsException("LTOTRACE utiliza su propio inicio de sesión");
        };
    }
}
