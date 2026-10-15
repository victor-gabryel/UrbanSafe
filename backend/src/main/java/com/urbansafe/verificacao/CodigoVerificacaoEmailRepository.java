package com.urbansafe.verificacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CodigoVerificacaoEmailRepository
        extends JpaRepository<CodigoVerificacaoEmail, UUID> {

    List<CodigoVerificacaoEmail> findByUsuarioIdAndUsadoFalse(
            UUID usuarioId
    );
}