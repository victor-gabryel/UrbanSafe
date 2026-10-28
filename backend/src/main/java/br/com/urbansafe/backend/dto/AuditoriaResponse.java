package br.com.urbansafe.backend.dto;

import java.time.OffsetDateTime;

import br.com.urbansafe.backend.entity.AuditoriaAdministrativa;

public record AuditoriaResponse(
        Integer id,
        Integer usuarioId,
        String acao,
        String entidade,
        Integer entidadeId,
        String detalhes,
        OffsetDateTime dataEvento) {

    public static AuditoriaResponse de(AuditoriaAdministrativa a) {
        return new AuditoriaResponse(a.getId(), a.getUsuarioId(), a.getAcao(), a.getEntidade(),
                a.getEntidadeId(), a.getDetalhes(), a.getDataEvento());
    }
}
