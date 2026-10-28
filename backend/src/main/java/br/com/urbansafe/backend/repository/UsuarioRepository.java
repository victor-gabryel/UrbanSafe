package br.com.urbansafe.backend.repository;

import br.com.urbansafe.backend.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Carrega o perfil junto (open-in-view está desligado, então o LAZY não pode ser resolvido depois). */
    @EntityGraph(attributePaths = "perfil")
    Optional<Usuario> findComPerfilByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "perfil")
    Optional<Usuario> findComPerfilById(Integer id);
}
