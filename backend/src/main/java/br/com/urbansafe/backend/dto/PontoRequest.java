package br.com.urbansafe.backend.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Ponto geográfico (origem ou destino de uma rota)")
public record PontoRequest(
        @Schema(description = "Latitude em graus decimais (-90 a 90)", example = "-8.0476") @NotNull(message = "A latitude é obrigatória") @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90") @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90") BigDecimal latitude,
        @Schema(description = "Longitude em graus decimais (-180 a 180)", example = "-34.8770") @NotNull(message = "A longitude é obrigatória") @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180") @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180") BigDecimal longitude,
        @Schema(description = "Rótulo ou endereço opcional, gravado no histórico de quem está autenticado", example = "Marco Zero") @Size(max = 255, message = "O rótulo deve ter no máximo 255 caracteres") String rotulo) {
}
