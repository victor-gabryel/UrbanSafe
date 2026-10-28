package br.com.urbansafe.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.urbansafe.backend.entity.Ocorrencia;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Integer> {

    /** Ocorrências excluídas logicamente não são retornadas. */
    Optional<Ocorrencia> findByIdAndExcluidaEmIsNull(Integer id);
}
