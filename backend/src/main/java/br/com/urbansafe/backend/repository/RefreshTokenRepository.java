package br.com.urbansafe.backend.repository;

import br.com.urbansafe.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revogação atômica: devolve 0 se o token já estava revogado (evita uso concorrente do mesmo token). */
    @Modifying
    @Query("update RefreshToken r set r.revogadoEm = :agora "
            + "where r.id = :id and r.revogadoEm is null")
    int revogarSeAtivo(@Param("id") Integer id, @Param("agora") OffsetDateTime agora);

    @Modifying
    @Query("update RefreshToken r set r.revogadoEm = :agora "
            + "where r.usuario.id = :usuarioId and r.revogadoEm is null")
    int revogarTodosDoUsuario(@Param("usuarioId") Integer usuarioId, @Param("agora") OffsetDateTime agora);
}
