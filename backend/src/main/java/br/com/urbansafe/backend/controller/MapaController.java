package br.com.urbansafe.backend.controller;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.urbansafe.backend.config.OpenApiConfig;
import br.com.urbansafe.backend.dto.MapaCalorResponse;
import br.com.urbansafe.backend.dto.MapaOcorrenciasResponse;
import br.com.urbansafe.backend.entity.StatusOcorrencia;
import br.com.urbansafe.backend.security.UsuarioPrincipal;
import br.com.urbansafe.backend.service.MapaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/mapa")
@Tag(name = "Mapa", description = "Dados georreferenciados para o mapa e o mapa de calor (PostGIS). Exigem JWT; "
        + "retornam apenas ocorrências PUBLICADAS e nenhum dado pessoal.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER)
public class MapaController {

    private final MapaService mapaService;

    public MapaController(MapaService mapaService) {
        this.mapaService = mapaService;
    }

    @Operation(summary = "Ocorrências na área do mapa",
            description = "Perfis: USUARIO, GESTOR, ADMINISTRADOR. 'bbox' é obrigatório (lado máximo configurável, padrão 5°). "
                    + "Retorna no máximo 'limit' marcadores (padrão e máximo: 500); 'truncado=true' indica que há mais na área. "
                    + "Somente GESTOR/ADMINISTRADOR podem pedir outro status além de PUBLICADA.")
    @ApiResponse(responseCode = "200", description = "Marcadores")
    @ApiResponse(responseCode = "400", description = "bbox ausente/inválido, datas ou limite inválidos")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário comum pedindo status diferente de PUBLICADA")
    @GetMapping("/ocorrencias")
    public ResponseEntity<MapaOcorrenciasResponse> ocorrencias(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(description = "minLon,minLat,maxLon,maxLat", example = "-34.95,-8.10,-34.85,-8.00", required = true)
            @RequestParam(name = "bbox", required = false) String bbox,
            @Parameter(description = "Id do tipo de ocorrência")
            @RequestParam(name = "tipo", required = false) Integer tipo,
            @Parameter(description = "Início do período (ISO-8601)")
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataInicio,
            @Parameter(description = "Fim do período (ISO-8601)")
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataFim,
            @Parameter(description = "Status (padrão PUBLICADA; outros valores só para GESTOR/ADMINISTRADOR)")
            @RequestParam(name = "status", required = false) StatusOcorrencia status,
            @Parameter(description = "Máximo de marcadores (1 a 500)")
            @RequestParam(name = "limit", required = false) Integer limit) {

        return ResponseEntity.ok(mapaService.ocorrencias(principal, bbox, tipo, dataInicio, dataFim, status, limit));
    }

    @Operation(summary = "Dados agregados para o mapa de calor",
            description = "Perfis: USUARIO, GESTOR, ADMINISTRADOR. Agrega as ocorrências em células de uma grade "
                    + "(ST_SnapToGrid) dentro do 'bbox' obrigatório. Cada ponto traz o centro da célula, a quantidade e a "
                    + "intensidade relativa (0 a 1). **A densidade reflete apenas os relatos registrados; não representa a "
                    + "probabilidade real de crime nem a segurança de um local.** Máximo de 2000 células (as mais densas).")
    @ApiResponse(responseCode = "200", description = "Células agregadas")
    @ApiResponse(responseCode = "400", description = "bbox ausente/inválido, célula fora do intervalo, datas inválidas")
    @ApiResponse(responseCode = "401", description = "Não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário comum pedindo status diferente de PUBLICADA")
    @GetMapping("/calor")
    public ResponseEntity<MapaCalorResponse> calor(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(description = "minLon,minLat,maxLon,maxLat", example = "-34.95,-8.10,-34.85,-8.00", required = true)
            @RequestParam(name = "bbox", required = false) String bbox,
            @Parameter(description = "Id do tipo de ocorrência")
            @RequestParam(name = "tipo", required = false) Integer tipo,
            @Parameter(description = "Início do período (ISO-8601)")
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataInicio,
            @Parameter(description = "Fim do período (ISO-8601)")
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dataFim,
            @Parameter(description = "Status (padrão PUBLICADA; outros valores só para GESTOR/ADMINISTRADOR)")
            @RequestParam(name = "status", required = false) StatusOcorrencia status,
            @Parameter(description = "Tamanho da célula em graus (0.0005 a 0.05; padrão 0.005 ≈ 550 m)", example = "0.005")
            @RequestParam(name = "celula", required = false) Double celula) {

        return ResponseEntity.ok(mapaService.calor(principal, bbox, tipo, dataInicio, dataFim, status, celula));
    }
}
