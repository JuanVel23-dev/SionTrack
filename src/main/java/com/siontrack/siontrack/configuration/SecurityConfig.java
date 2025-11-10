package com.siontrack.siontrack.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 1. Define el usuario "admin"
     * Requerido porque Spring ignora el application.properties
     * cuando se usa una SecurityFilterChain personalizada.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("admin") // [cite: 5857]
                .password("admin123") // [cite: 5858]
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    /**
     * 2. Configura la cadena de filtros de seguridad
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 3. Permite explícitamente las carpetas de CSS y JS
                        .requestMatchers(
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**")
                        ).permitAll()
                        
                        // 4. Permite la página de login
                        .requestMatchers(new AntPathRequestMatcher("/login")).permitAll()
                        
                        // 5. Protege todo lo demás
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/web/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                // 6. Configura cabeceras para permitir FontAwesome (el CDN)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                    // Permite estilos de 'self' (tu app) y del CDN
                                    "style-src 'self' https://cdnjs.cloudflare.com; " + 
                                    // Permite scripts solo de 'self' (tu app)
                                    "script-src 'self'; " +
                                    // Permite fuentes de 'self' y del CDN (para los iconos)
                                    "font-src 'self' https://cdnjs.cloudflare.com;"
                                )
                        )
                );

        return http.build();
    }
}