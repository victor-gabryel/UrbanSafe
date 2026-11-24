package br.com.urbansafe.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import br.com.urbansafe.backend.entity.PerfilDeslocamento;

@Schema(description = "Origem, destino e preferências para calcular um percurso a pé ou de bicicleta")
public record CalcularRotaRequest(
                @NotNull(message = "A origem é obrigatória") @Valid PontoRequest origem,
                @NotNull(message = "O destino é obrigatório") @Valid PontoRequest destino,
                @Schema(description = "A_PE (padrão) ou BICICLETA", example = "A_PE") PerfilDeslocamento perfil,
                @Valid PreferenciasRotaRequest preferencias) {

    public PerfilDeslocamento perfilEfetivo() {
        return perfil == null ? PerfilDeslocamento.A_PE : perfil;
    }

    public boolean calcularExposicao() {
        return preferencias == null || preferencias.calcularExposicao();
    }

    public boolean ponderarPorDistancia() {
        return preferencias == null || preferencias.ponderarPorDistancia();
    }
}
