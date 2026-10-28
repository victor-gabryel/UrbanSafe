package br.com.urbansafe.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Metadados do Swagger e o esquema Bearer JWT usado pelo botão "Authorize". */

@Configuration

public class OpenApiConfig {

        public static final String ESQUEMA_BEARER = "bearerAuth";

        @Bean

        public OpenAPI urbanSafeOpenApi() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("UrbanSafe API")
                                                .version("0.0.1")
                                                .description("API REST do UrbanSafe. Rotas de autenticação são públicas; "
                                                                + "as demais exigem o cabeçalho Authorization: Bearer {accessToken}. "
                                                                + "Contas só acessam rotas protegidas depois de verificar o e-mail."))

                                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                                                new SecurityScheme()
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")));

        }
}