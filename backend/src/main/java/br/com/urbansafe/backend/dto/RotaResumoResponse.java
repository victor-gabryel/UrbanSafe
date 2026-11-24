package br.com.urbansafe.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.urbansafe.backend.entity.Rota;

@Schema(description = "Consulta de rota gravada no histórico do usuário (sem geometria)")
public record RotaResumoResponse(
                Integer id,
                String origem,
                String destino,
                BigDecimal origemLatitude,
                BigDecimal origemLongitude,
                BigDecimal destinoLatitude,
                BigDecimal destinoLongitude,
                @Schema(description = "Metros") BigDecimal distancia,
                @Schema(description = "Segundos") Integer tempoEstimado,
                @Schema(description = "0 a 100 ou nulo; indicador informativo, não é garantia de segurança") BigDecimal indiceExposicao,
                OffsetDateTime dataCalculo) {
    public static RotaResumoResponse de(Rota r) {
        return new RotaResumoResponse(r.getId(), r.getOrigem(), r.getDestino(), r.getOrigemLatitude(),
                r.getOrigemLongitude(), r.getDestinoLatitude(), r.getDestinoLongitude(), r.getDistancia(),
                r.getTempoEstimado(), r.getIndiceExposicao(), r.getDataCalculo());
    }
}
