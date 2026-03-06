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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("admin123")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF: Ignorar en API para permitir POST desde Postman
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**"))

                // 2. AUTHORIZATION
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/login", "/api/webhook").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())

                // 3. Form Login
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

                // 6. Headers — Content Security Policy
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