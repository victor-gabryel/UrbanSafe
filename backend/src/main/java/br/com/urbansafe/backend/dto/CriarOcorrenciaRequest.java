package br.com.urbansafe.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Propositalmente NÃO há usuário, status nem relevância: o autor vem do JWT, o
 * status inicial é definido
 * pelo servidor e a relevância é classificação interna. Campos extras enviados
 * são descartados.
 */
@Schema(description = "Dados para registrar uma ocorrência")
public record CriarOcorrenciaRequest(

                @Schema(description = "Id de um tipo ATIVO (GET /api/tipos-ocorrencia)", example = "1") @NotNull(message = "O tipo de ocorrência é obrigatório") Integer tipoOcorrenciaId,

                @Schema(example = "Celular furtado no ponto de ônibus") @NotBlank(message = "O título é obrigatório") @Size(max = 150, message = "O título deve ter no máximo 150 caracteres") String titulo,

                @Schema(example = "Fui abordado enquanto esperava o ônibus e tiveram meu celular subtraído.") @NotBlank(message = "A descrição é obrigatória") @Size(max = 5000, message = "A descrição deve ter no máximo 5000 caracteres") String descricao,

                @Schema(description = "Latitude em graus decimais (-90 a 90)", example = "-8.0476") @NotNull(message = "A latitude é obrigatória") @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90") @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90") BigDecimal latitude,

                @Schema(description = "Longitude em graus decimais (-180 a 180)", example = "-34.8770") @NotNull(message = "A longitude é obrigatória") @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180") @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180") BigDecimal longitude,

                @Size(max = 500, message = "O endereço deve ter no máximo 500 caracteres") String endereco,

                @Schema(description = "Momento aproximado do fato (ISO-8601); não pode estar no futuro", example = "2026-10-20T18:30:00-03:00") OffsetDateTime dataOcorrencia,

                @Schema(description = "Se true, o autor não é exibido a outros usuários. Padrão: false") Boolean anonimato) {
}