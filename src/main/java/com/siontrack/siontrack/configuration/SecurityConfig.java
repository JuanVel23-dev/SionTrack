package com.siontrack.siontrack.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // 1. Permisos Públicos:
                // Permite que todos vean la página de login
                .requestMatchers("/login").permitAll() 
                // Permite acceso a recursos estáticos (CSS, JS) si los tienes
                .requestMatchers("/css/**", "/js/**").permitAll() 
                
                // 2. Permisos Protegidos:
                // Cualquier otra solicitud (incluyendo todos tus @RestController)
                // debe estar autenticada.
                .anyRequest().authenticated()
            )
            // 3. Configuración del Formulario de Login:
            .formLogin(form -> form
                // Le dice a Spring Security dónde está tu página de login
                .loginPage("/login") 
                // La URL que procesará el login (Spring la maneja)
                .loginProcessingUrl("/login") 
                // A dónde ir después de un login exitoso
                .defaultSuccessUrl("/web/dashboard", true) 
                .permitAll() // Todos pueden acceder a la URL de procesamiento
            )
            // 4. Configuración de Logout:
            .logout(logout -> logout
                .logoutUrl("/logout") // URL para cerrar sesión
                .logoutSuccessUrl("/login?logout") // A dónde ir después
                .permitAll()
            );
        
        // NOTA: No deshabilitamos CSRF. Thymeleaf lo maneja por ti.
        
        return http.build();
    }
}
