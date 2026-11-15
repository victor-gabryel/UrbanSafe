package br.com.urbansafe.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.security.JwtAuthenticationFilter;
import br.com.urbansafe.backend.security.JwtService;
import br.com.urbansafe.backend.security.RestAccessDeniedHandler;
import br.com.urbansafe.backend.security.RestAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtService jwtService,
                        UsuarioRepository usuarioRepository,
                        RestAuthenticationEntryPoint authenticationEntryPoint,
                        RestAccessDeniedHandler accessDeniedHandler,
                        CorsConfigurationSource corsConfigurationSource) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .httpBasic(basic -> basic.disable())
                                .formLogin(form -> form.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(handling -> handling
                                                .authenticationEntryPoint(authenticationEntryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/auth/cadastro",
                                                                "/api/auth/login",
                                                                "/api/auth/verificar-email",
                                                                "/api/auth/reenviar-verificacao",
                                                                "/api/auth/recuperar-senha",
                                                                "/api/auth/redefinir-senha",
                                                                "/api/auth/refresh",
                                                                "/api/auth/logout")
                                                .permitAll()
                                                // Documentação OpenAPI (desligue com SWAGGER_ENABLED=false).
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/v3/api-docs",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**")
                                                .permitAll()
                                                // Modo visitante (navegação anônima no app): leitura pública do mapa,
                                                // do mapa de calor
                                                // e do catálogo de tipos. Retornam só ocorrências PUBLICADAS e nenhum
                                                // dado pessoal.
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/mapa/ocorrencias",
                                                                "/api/mapa/calor",
                                                                "/api/tipos-ocorrencia")
                                                .permitAll()
                                                // Tipos de ocorrência: escrita só ADMINISTRADOR.
                                                .requestMatchers(HttpMethod.POST, "/api/tipos-ocorrencia")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.PATCH, "/api/tipos-ocorrencia/*")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/tipos-ocorrencia/*")
                                                .hasRole("ADMINISTRADOR")
                                                // Moderação (POST e GET): somente GESTOR e ADMINISTRADOR.
                                                .requestMatchers("/api/ocorrencias/*/moderacao")
                                                .hasAnyRole("GESTOR", "ADMINISTRADOR")
                                                // Administração: somente ADMINISTRADOR.
                                                .requestMatchers("/api/admin/**")
                                                .hasRole("ADMINISTRADOR")
                                                // Demais rotas (ocorrências, mídias, mapa, usuários): JWT válido; a
                                                // propriedade
                                                // e a visibilidade dos registros são validadas nos serviços.
                                                .anyRequest().authenticated())
                                .addFilterBefore(
                                                new JwtAuthenticationFilter(jwtService, usuarioRepository),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /**
         * CORS restrito à origem do front-end configurada em app.frontend.url
         * (FRONTEND_URL).
         * Sem cookies/credenciais: a autenticação é por header Authorization.
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource(
                        @Value("${app.frontend.url}") String frontendUrl) {
                CorsConfiguration configuracao = new CorsConfiguration();
                // Aceita várias origens separadas por vírgula (ex.: web do Expo em
                // http://localhost:8081).
                // O app nativo (Expo Go) não envia header Origin, então não depende do CORS.
                configuracao.setAllowedOrigins(Arrays.stream(frontendUrl.split(","))
                                .map(String::trim)
                                .filter(o -> !o.isEmpty())
                                .toList());
                configuracao.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
                configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                configuracao.setAllowCredentials(false);
                configuracao.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
                fonte.registerCorsConfiguration("/api/**", configuracao);
                return fonte;
        }
}
