package br.com.urbansafe.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.urbansafe.backend.entity.HistoricoOcorrencia;

public interface HistoricoOcorrenciaRepository extends JpaRepository<HistoricoOcorrencia, Integer> {

    List<HistoricoOcorrencia> findByOcorrenciaIdOrderByDataEventoAscIdAsc(Integer ocorrenciaId);
}
