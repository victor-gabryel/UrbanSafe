package br.com.urbansafe.backend.repository;

import br.com.urbansafe.backend.entity.TokenVerificacaoEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenVerificacaoEmailRepository
        extends JpaRepository<TokenVerificacaoEmail, Integer> {

    Optional<TokenVerificacaoEmail> findByTokenHash(String tokenHash);
}