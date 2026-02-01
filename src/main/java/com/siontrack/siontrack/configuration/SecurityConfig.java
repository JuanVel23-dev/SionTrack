package com.siontrack.siontrack.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // Ya no es estrictamente necesario con la sintaxis simplificada

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("admin") //
                .password("admin123") //
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF: Ignorar en API para permitir POST desde Postman
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")) // AGREGE LA BARRA "/" AL INICIO

                // 2. AUTHORIZATION: Unificado en un solo bloque y ordenado correctamente
                .authorizeHttpRequests(authorize -> authorize
                        // A. Recursos estáticos y Login (Públicos) - Van PRIMERO
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/login", "/api/webhook").permitAll()

                        // B. Rutas de API (Protegidas, pero accesibles con Basic Auth)
                        .requestMatchers("/api/**").authenticated()

                        // C. REGLA FINAL (Catch-all): Todo lo demás autenticado - DEBE IR AL FINAL
                        .anyRequest().authenticated())

                // 3. Form Login (Para usuarios web)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/web/dashboard", true)
                        .permitAll())

                // 4. Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())

                // 5. HTTP Basic (Para Postman)
                .httpBasic(Customizer.withDefaults())

                // 6. Headers (CSP para estilos y scripts)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "style-src 'self' https://cdnjs.cloudflare.com; script-src 'self'; font-src 'self' https://cdnjs.cloudflare.com;")));

        return http.build();
    }
}