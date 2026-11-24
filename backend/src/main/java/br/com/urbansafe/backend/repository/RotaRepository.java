package br.com.urbansafe.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.urbansafe.backend.entity.Rota;

public interface RotaRepository extends JpaRepository<Rota, Integer> {

    Page<Rota> findByUsuarioId(Integer usuarioId, Pageable pageable);

    Optional<Rota> findByIdAndUsuarioId(Integer id, Integer usuarioId);
}
