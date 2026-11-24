package br.com.urbansafe.backend.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Saúde", description = "Disponibilidade da API. Públicos e sem detalhes internos.")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Operation(summary = "Disponibilidade básica da API")
    @ApiResponse(responseCode = "200", description = "API no ar")
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @Operation(summary = "Pronta para receber requisições", description = "Verifica a conexão com o banco; não expõe detalhes.")
    @ApiResponse(responseCode = "200", description = "Pronta")
    @ApiResponse(responseCode = "503", description = "Banco indisponível")
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "UP"));
        } catch (RuntimeException e) {
            log.warn("Verificação de prontidão falhou: {}", e.toString());
            return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
        }
    }
}
