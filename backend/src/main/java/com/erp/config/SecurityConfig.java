package com.erp.config;

import com.erp.auth.JwtAuthFilter;
import com.erp.users.AppUser;
import com.erp.users.Role;
import com.erp.users.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository repository) {
        return username -> repository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuario no encontrado"));
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions ->
                    exceptions.authenticationEntryPoint(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/login", "/actuator/health").permitAll()
                    .requestMatchers("/actuator/info").hasRole("ADMIN")
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    // Movimientos: reglas específicas antes de las de productos.
                    .requestMatchers(HttpMethod.GET,
                            "/api/products/*/movements",
                            "/api/products/*/movements/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN")
                    .requestMatchers(HttpMethod.POST,
                            "/api/products/*/movements",
                            "/api/products/*/movements/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN")
                    .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN", "VENDEDOR", "CONTADOR")
                    .requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN")
                    .requestMatchers(HttpMethod.PUT, "/api/products", "/api/products/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN")
                    .requestMatchers(HttpMethod.PATCH, "/api/products", "/api/products/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN")
                    .requestMatchers(HttpMethod.GET, "/api/suppliers", "/api/suppliers/**",
                            "/api/purchases", "/api/purchases/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN", "CONTADOR")
                    .requestMatchers(HttpMethod.POST, "/api/suppliers", "/api/suppliers/**")
                        .hasAnyRole("ADMIN", "GERENTE")
                    .requestMatchers(HttpMethod.PUT, "/api/suppliers", "/api/suppliers/**")
                        .hasAnyRole("ADMIN", "GERENTE")
                    .requestMatchers(HttpMethod.PATCH, "/api/suppliers", "/api/suppliers/**")
                        .hasAnyRole("ADMIN", "GERENTE")
                    .requestMatchers(HttpMethod.POST, "/api/purchases", "/api/purchases/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN")
                    // Ventas: consulta y registro según el rol.
                    .requestMatchers(HttpMethod.GET, "/api/sales", "/api/sales/**")
                        .hasAnyRole("ADMIN", "GERENTE", "VENDEDOR", "CONTADOR")
                    .requestMatchers(HttpMethod.POST, "/api/sales", "/api/sales/**")
                        .hasAnyRole("ADMIN", "GERENTE", "VENDEDOR")
                    // Categorías: consulta para todos los roles operativos.
                    .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN", "VENDEDOR", "CONTADOR")
                    .requestMatchers(HttpMethod.POST, "/api/categories", "/api/categories/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN")
                    .requestMatchers(HttpMethod.PUT, "/api/categories", "/api/categories/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN")
                    .requestMatchers(HttpMethod.PATCH, "/api/categories", "/api/categories/**")
                        .hasAnyRole("ADMIN", "GERENTE", "ALMACEN")
                    // Clientes: consulta y gestión para administración y ventas.
                    .requestMatchers(HttpMethod.GET, "/api/customers", "/api/customers/**")
                        .hasAnyRole("ADMIN", "GERENTE", "VENDEDOR")
                    .requestMatchers(HttpMethod.POST, "/api/customers", "/api/customers/**")
                        .hasAnyRole("ADMIN", "GERENTE", "VENDEDOR")
                    .requestMatchers(HttpMethod.PUT, "/api/customers", "/api/customers/**")
                        .hasAnyRole("ADMIN", "GERENTE", "VENDEDOR")
                    .requestMatchers(HttpMethod.PATCH, "/api/customers", "/api/customers/**")
                        .hasAnyRole("ADMIN", "GERENTE", "VENDEDOR")
                    .anyRequest().denyAll())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CommandLineRunner seedAdmin(
            UserRepository repository,
            PasswordEncoder encoder,
            @Value("${app.admin.username:admin}") String username,
            @Value("${ERP_ADMIN_PASSWORD:}") String password) {

        return args -> {
            if (!repository.existsByUsername(username)) {
                if (password == null || password.isBlank()) {
                    throw new IllegalStateException(
                            "No se puede crear el administrador inicial: "
                            + "configure ERP_ADMIN_PASSWORD en el entorno.");
                }

                repository.save(new AppUser(
                        username, encoder.encode(password), Role.ADMIN));
                System.out.println("Usuario administrador inicial creado: " + username);
            }
        };
    }
}
