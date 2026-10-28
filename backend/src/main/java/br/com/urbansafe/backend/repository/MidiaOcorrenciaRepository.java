package br.com.urbansafe.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.urbansafe.backend.entity.MidiaOcorrencia;

public interface MidiaOcorrenciaRepository extends JpaRepository<MidiaOcorrencia, Integer> {

    List<MidiaOcorrencia> findByOcorrenciaIdAndRemovidaEmIsNullOrderByIdAsc(Integer ocorrenciaId);

    Optional<MidiaOcorrencia> findByIdAndOcorrenciaIdAndRemovidaEmIsNull(Integer id, Integer ocorrenciaId);

    long countByOcorrenciaIdAndRemovidaEmIsNull(Integer ocorrenciaId);
}
