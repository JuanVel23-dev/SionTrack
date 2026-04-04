package com.siontrack.siontrack.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.siontrack.siontrack.services.UsuariosService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UsuariosService usuariosService;

    public SecurityConfig(UsuariosService usuariosService) {
        this.usuariosService = usuariosService;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http)throws Exception{
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder
                .userDetailsService(usuariosService)
                .passwordEncoder(passwordEncoder());
        return authBuilder.build();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Necesario para que Spring Security no bloquee el token CSRF en el cookie
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        // Solo deshabilitar CSRF para el webhook externo de WhatsApp
                        .ignoringRequestMatchers("/api/webhook", "/api/webhook/**")
                        // Usar cookie para que JS pueda leer el token CSRF
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler))

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/img/**", "/webjars/**", "/login", "/error").permitAll()
                        .requestMatchers("/api/webhook", "/api/webhook/**").permitAll()
                        // Restringir endpoints de Actuator
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/web/dashboard", true)
                        .permitAll())

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())

                // Control de sesiones
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(true)
                        .expiredUrl("/login?expired"))

                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(String.join("; ",
                                        "default-src 'self'",
                                        "script-src 'self' https://cdn.jsdelivr.net",
                                        "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://fonts.googleapis.com",
                                        "font-src 'self' https://cdnjs.cloudflare.com https://fonts.gstatic.com",
                                        "img-src 'self' data:",
                                        "connect-src 'self'"
                                ))));

        return http.build();
    }
}
