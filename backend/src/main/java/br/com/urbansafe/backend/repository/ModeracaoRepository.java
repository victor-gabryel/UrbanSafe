package br.com.urbansafe.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.urbansafe.backend.entity.Moderacao;

public interface ModeracaoRepository extends JpaRepository<Moderacao, Integer> {

    List<Moderacao> findByOcorrenciaIdOrderByDataModeracaoAscIdAsc(Integer ocorrenciaId);
}
