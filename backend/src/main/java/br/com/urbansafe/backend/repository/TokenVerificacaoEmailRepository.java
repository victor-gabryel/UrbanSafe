package br.com.urbansafe.backend.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.urbansafe.backend.entity.TokenVerificacaoEmail;

public interface TokenVerificacaoEmailRepository
        extends JpaRepository<TokenVerificacaoEmail, Integer> {

    Optional<TokenVerificacaoEmail> findByTokenHash(String tokenHash);

    /** Há token emitido para o usuário depois de {@code limite}? Usado no intervalo mínimo entre reenvios. */
    boolean existsByUsuario_IdAndCriadoEmAfter(Integer usuarioId, OffsetDateTime limite);

    /**
     * Consumo atômico: só atualiza se o token ainda estiver pendente e dentro da validade.
     * Retorna 1 para quem "ganhou" e 0 para qualquer outra chamada concorrente ou repetida,
     * o que garante uso único mesmo com requisições simultâneas.
     */
    @Modifying
    @Query("update TokenVerificacaoEmail t set t.utilizadoEm = :agora "
            + "where t.tokenHash = :tokenHash and t.utilizadoEm is null and t.expiraEm > :agora")
    int consumir(@Param("tokenHash") String tokenHash, @Param("agora") OffsetDateTime agora);

    /** Invalida todos os tokens pendentes do usuário (um novo envio ou a verificação substitui os anteriores). */
    @Modifying
    @Query("update TokenVerificacaoEmail t set t.utilizadoEm = :agora "
            + "where t.usuario.id = :usuarioId and t.utilizadoEm is null")
    int invalidarPendentes(@Param("usuarioId") Integer usuarioId, @Param("agora") OffsetDateTime agora);
}
