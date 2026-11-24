package br.com.urbansafe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Preferências do cálculo. Campos ausentes assumem true.")
public record PreferenciasRotaRequest(
                @Schema(description = "false = não calcula o indicador de exposição (só distância, tempo e geometria)") Boolean considerarOcorrencias,
                @Schema(description = "false = toda ocorrência dentro do raio pesa igual; true = pesa menos quanto mais longe do percurso") Boolean considerarDistanciaDasOcorrencias) {

    public boolean calcularExposicao() {
        return considerarOcorrencias == null || considerarOcorrencias;
    }

    public boolean ponderarPorDistancia() {
        return considerarDistanciaDasOcorrencias == null || considerarDistanciaDasOcorrencias;
    }
}
