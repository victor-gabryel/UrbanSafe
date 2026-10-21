package br.com.urbansafe.backend.repository;

import br.com.urbansafe.backend.entity.TokenRecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface TokenRecuperacaoSenhaRepository
        extends JpaRepository<TokenRecuperacaoSenha, Integer> {

    Optional<TokenRecuperacaoSenha> findByTokenHash(String tokenHash);

    /** Invalida todos os tokens pendentes do usuário (um novo pedido ou uma redefinição substitui os anteriores). */
    @Modifying
    @Query("update TokenRecuperacaoSenha t set t.utilizadoEm = :agora "
            + "where t.usuario.id = :usuarioId and t.utilizadoEm is null")
    int invalidarPendentes(@Param("usuarioId") Integer usuarioId, @Param("agora") OffsetDateTime agora);
}
