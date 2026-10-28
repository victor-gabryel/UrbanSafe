package br.com.urbansafe.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.urbansafe.backend.entity.Perfil;

public interface PerfilRepository extends JpaRepository<Perfil, Integer> {

    Optional<Perfil> findByNome(String nome);
}
